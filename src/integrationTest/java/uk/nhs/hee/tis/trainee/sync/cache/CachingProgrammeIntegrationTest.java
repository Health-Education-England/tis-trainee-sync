/*
 * The MIT License (MIT)
 *
 * Copyright 2021 Crown Copyright (Health Education England)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package uk.nhs.hee.tis.trainee.sync.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.redis.testcontainers.RedisContainer;
import io.awspring.cloud.autoconfigure.sqs.SqsAutoConfiguration;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.nhs.hee.tis.trainee.sync.DockerImageNames;
import uk.nhs.hee.tis.trainee.sync.config.MongoConfiguration;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.Programme;
import uk.nhs.hee.tis.trainee.sync.repository.ProgrammeRepository;
import uk.nhs.hee.tis.trainee.sync.service.ProgrammeSyncService;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@EnableAutoConfiguration(exclude = SqsAutoConfiguration.class)
class CachingProgrammeIntegrationTest {

  private static final String PROGRAMME_FORDY = "fordy";

  @Container
  @ServiceConnection
  private static final MongoDBContainer mongoContainer = new MongoDBContainer(
      DockerImageNames.MONGO);

  @Container
  private static final RedisContainer redisContainer = new RedisContainer(DockerImageNames.REDIS);

  @DynamicPropertySource
  private static void registerRedisProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.redis.host", redisContainer::getHost);
    registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
  }

  // We require access to the mock before the proxy wraps it.
  private static ProgrammeRepository mockProgrammeRepository;

  @Autowired
  private ProgrammeSyncService programmeSyncService;

  @Autowired
  private CacheManager cacheManager;

  @MockitoBean
  private SqsTemplate sqsTemplate;

  private Cache programmeCache;

  private Programme programme;

  @BeforeEach
  void setup() {
    // Reset static mock to clear any interactions and stubbing, ensures it is clean for each test.
    reset(mockProgrammeRepository);

    programme = new Programme();
    programme.setTisId(PROGRAMME_FORDY);
    programme.setOperation(Operation.DELETE);
    programme.setTable(Programme.ENTITY_NAME);

    programmeCache = cacheManager.getCache(Programme.ENTITY_NAME);
    assertNotNull(programmeCache);
    programmeCache.clear();
  }

  @Test
  void shouldHitCacheOnSecondInvocation() {
    when(mockProgrammeRepository.findById(PROGRAMME_FORDY))
        .thenReturn(Optional.of(programme), Optional.of(new Programme()));
    assertThat(programmeCache.get(PROGRAMME_FORDY)).isNull();

    Optional<Programme> actual1 = programmeSyncService.findById(PROGRAMME_FORDY);
    assertThat(programmeCache.get(PROGRAMME_FORDY)).isNotNull();
    Optional<Programme> actual2 = programmeSyncService.findById(PROGRAMME_FORDY);

    verify(mockProgrammeRepository).findById(PROGRAMME_FORDY);
    assertThat(actual1).isPresent().get().isEqualTo(programme).isEqualTo(actual2.orElseThrow());
  }

  @Test
  void shouldEvictWhenDeletedAndMissCacheOnNextInvocation() {
    Programme otherProgramme = new Programme();
    final String otherKey = "Foo";
    otherProgramme.setTisId(otherKey);
    otherProgramme.setOperation(Operation.LOAD);
    when(mockProgrammeRepository.findById(otherKey)).thenReturn(Optional.of(otherProgramme));
    programmeSyncService.findById(otherKey);
    assertThat(programmeCache.get(otherKey)).isNotNull();
    when(mockProgrammeRepository.findById(PROGRAMME_FORDY)).thenReturn(Optional.of(programme));
    programmeSyncService.findById(PROGRAMME_FORDY);
    assertThat(programmeCache.get(PROGRAMME_FORDY)).isNotNull();

    programmeSyncService.syncRecord(programme);
    assertThat(programmeCache.get(PROGRAMME_FORDY)).isNull();
    assertThat(programmeCache.get(otherKey)).isNotNull();
    verify(mockProgrammeRepository).deleteById(PROGRAMME_FORDY);

    programmeSyncService.findById(PROGRAMME_FORDY);
    assertThat(programmeCache.get(PROGRAMME_FORDY)).isNotNull();

    verify(mockProgrammeRepository, times(2)).findById(PROGRAMME_FORDY);
  }

  @Test
  void shouldReplaceCacheWhenSaved() {
    Programme staleProgramme = new Programme();
    staleProgramme.setTisId(PROGRAMME_FORDY);
    staleProgramme.setTable("Stale");
    staleProgramme.setOperation(Operation.UPDATE);
    when(mockProgrammeRepository.save(staleProgramme)).thenReturn(staleProgramme);
    programmeSyncService.syncRecord(staleProgramme);
    assertThat(programmeCache.get(PROGRAMME_FORDY).get()).isEqualTo(staleProgramme);

    Programme updateProgramme = new Programme();
    updateProgramme.setTable(Programme.ENTITY_NAME);
    updateProgramme.setTisId(PROGRAMME_FORDY);
    updateProgramme.setOperation(Operation.UPDATE);
    when(mockProgrammeRepository.save(updateProgramme)).thenReturn(programme);

    programmeSyncService.syncRecord(updateProgramme);
    assertThat(programmeCache.get(PROGRAMME_FORDY).get()).isEqualTo(programme);
    verify(mockProgrammeRepository).save(staleProgramme);
    verify(mockProgrammeRepository).save(updateProgramme);
  }

  @TestConfiguration
  static class Configuration {

    @Primary
    @Bean
    ProgrammeRepository mockProgrammeRepository() {
      mockProgrammeRepository = mock(ProgrammeRepository.class);
      return mockProgrammeRepository;
    }

    /// /// Mocks to enable application context //////
    @MockitoBean
    private MongoConfiguration mongoConfiguration;
    /////////////////////////////////////////////////
  }
}

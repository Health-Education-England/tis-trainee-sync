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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;

import io.awspring.cloud.autoconfigure.messaging.SqsAutoConfiguration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import uk.nhs.hee.tis.trainee.sync.config.MongoConfiguration;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.Programme;
import uk.nhs.hee.tis.trainee.sync.repository.ProgrammeRepository;
import uk.nhs.hee.tis.trainee.sync.service.ProgrammeSyncService;

@SpringBootTest
@EnableAutoConfiguration(exclude = SqsAutoConfiguration.class)
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
class CachingProgrammeIntTest {

  private static final String PROGRAMME_FORDY = "fordy";

  // We require access to the mock before the proxy wraps it.
  private static ProgrammeRepository mockProgrammeRepository;

  @Autowired
  ProgrammeSyncService programmeSyncService;

  @Autowired
  CacheManager cacheManager;

  private Cache programmeCache;

  private Programme programme;

  @BeforeEach
  void setup() {
    programme = new Programme();
    programme.setTisId(PROGRAMME_FORDY);
    programme.setOperation(Operation.DELETE);
    programme.setTable(Programme.ENTITY_NAME);

    programmeCache = cacheManager.getCache(Programme.ENTITY_NAME);
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

    ////// Mocks to enable application context //////
    @MockBean
    private MongoConfiguration mongoConfiguration;
    /////////////////////////////////////////////////
  }
}

/*
 * The MIT License (MIT)
 *
 * Copyright 2024 Crown Copyright (Health Education England)
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

import io.awspring.cloud.autoconfigure.sqs.SqsAutoConfiguration;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.util.Map;
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
import org.springframework.test.context.ActiveProfiles;
import uk.nhs.hee.tis.trainee.sync.config.MongoConfiguration;
import uk.nhs.hee.tis.trainee.sync.model.HeeUser;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.repository.HeeUserRepository;
import uk.nhs.hee.tis.trainee.sync.service.HeeUserSyncService;
import uk.nhs.hee.tis.trainee.sync.service.ReferenceSyncService;

@SpringBootTest(properties = {"cloud.aws.region.static=eu-west-2"})
@ActiveProfiles("int")
@EnableAutoConfiguration(exclude = SqsAutoConfiguration.class)
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
class CachingHeeUserIntTest {

  private static final String HEEUSER_FORDY = "fordy";

  // We require access to the mock before the proxy wraps it.
  private static HeeUserRepository mockHeeUserRepository;

  @Autowired
  HeeUserSyncService heeUserSyncService;

  @MockBean
  ReferenceSyncService referenceSyncService;

  @MockBean
  private SqsTemplate sqsTemplate;

  @Autowired
  CacheManager cacheManager;

  private Cache dbcCache;

  private HeeUser heeUser;

  @BeforeEach
  void setup() {
    heeUser = new HeeUser();
    heeUser.setTisId(HEEUSER_FORDY);
    heeUser.setOperation(Operation.DELETE);
    heeUser.setTable(HeeUser.ENTITY_NAME);
    heeUser.setData(Map.of("name", HEEUSER_FORDY));

    dbcCache = cacheManager.getCache(HeeUser.ENTITY_NAME);
  }

  @Test
  void shouldHitCacheOnSecondInvocation() {
    when(mockHeeUserRepository.findById(HEEUSER_FORDY))
        .thenReturn(Optional.of(heeUser), Optional.of(new HeeUser()));
    assertThat(dbcCache.get(HEEUSER_FORDY)).isNull();

    Optional<HeeUser> actual1 = heeUserSyncService.findById(HEEUSER_FORDY);
    assertThat(dbcCache.get(HEEUSER_FORDY)).isNotNull();
    Optional<HeeUser> actual2 = heeUserSyncService.findById(HEEUSER_FORDY);

    verify(mockHeeUserRepository).findById(HEEUSER_FORDY);
    assertThat(actual1).isPresent().get().isEqualTo(heeUser).isEqualTo(actual2.orElseThrow());
  }

  @Test
  void shouldEvictWhenDeletedAndMissCacheOnNextInvocation() {
    HeeUser otherHeeUser = new HeeUser();
    final String otherKey = "Foo";
    otherHeeUser.setTisId(otherKey);
    otherHeeUser.setOperation(Operation.LOAD);
    when(mockHeeUserRepository.findById(otherKey)).thenReturn(Optional.of(otherHeeUser));
    heeUserSyncService.findById(otherKey);
    assertThat(dbcCache.get(otherKey)).isNotNull();
    when(mockHeeUserRepository.findById(HEEUSER_FORDY)).thenReturn(Optional.of(heeUser));
    heeUserSyncService.findById(HEEUSER_FORDY);
    assertThat(dbcCache.get(HEEUSER_FORDY)).isNotNull();

    when(mockHeeUserRepository.findByName(HEEUSER_FORDY)).thenReturn(Optional.of(heeUser));
    heeUserSyncService.syncRecord(heeUser);

    assertThat(dbcCache.get(HEEUSER_FORDY)).isNull();
    assertThat(dbcCache.get(otherKey)).isNotNull();
    verify(mockHeeUserRepository).deleteById(HEEUSER_FORDY);

    heeUserSyncService.findById(HEEUSER_FORDY);
    assertThat(dbcCache.get(HEEUSER_FORDY)).isNotNull();

    verify(mockHeeUserRepository, times(2)).findById(HEEUSER_FORDY);
  }

  @Test
  void shouldReplaceCacheWhenSaved() {
    HeeUser staleHeeUser = new HeeUser();
    staleHeeUser.setTisId(HEEUSER_FORDY);
    staleHeeUser.setTable("Stale");
    staleHeeUser.setOperation(Operation.UPDATE);
    when(mockHeeUserRepository.save(staleHeeUser)).thenReturn(staleHeeUser);
    heeUserSyncService.syncRecord(staleHeeUser);
    assertThat(dbcCache.get(HEEUSER_FORDY).get()).isEqualTo(staleHeeUser);

    HeeUser updateHeeUser = new HeeUser();
    updateHeeUser.setTable(HeeUser.ENTITY_NAME);
    updateHeeUser.setTisId(HEEUSER_FORDY);
    updateHeeUser.setOperation(Operation.UPDATE);
    when(mockHeeUserRepository.save(updateHeeUser)).thenReturn(heeUser);

    heeUserSyncService.syncRecord(updateHeeUser);
    assertThat(dbcCache.get(HEEUSER_FORDY).get()).isEqualTo(heeUser);
    verify(mockHeeUserRepository).save(staleHeeUser);
    verify(mockHeeUserRepository).save(updateHeeUser);
  }

  @TestConfiguration
  static class Configuration {

    @Primary
    @Bean
    HeeUserRepository mockHeeUserRepository() {
      mockHeeUserRepository = mock(HeeUserRepository.class);
      return mockHeeUserRepository;
    }

    ////// Mocks to enable application context //////
    @MockBean
    private MongoConfiguration mongoConfiguration;
    /////////////////////////////////////////////////
  }
}

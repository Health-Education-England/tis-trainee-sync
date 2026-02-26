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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.nhs.hee.tis.trainee.sync.config.MongoConfiguration;
import uk.nhs.hee.tis.trainee.sync.model.Dbc;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.repository.DbcRepository;
import uk.nhs.hee.tis.trainee.sync.service.DbcSyncService;
import uk.nhs.hee.tis.trainee.sync.service.ReferenceSyncService;

@SpringBootTest(properties = {"cloud.aws.region.static=eu-west-2"})
@ActiveProfiles("int")
@EnableAutoConfiguration(exclude = SqsAutoConfiguration.class)
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
class CachingDbcIntTest {

  private static final String DBC_FORDY = "fordy";

  // We require access to the mock before the proxy wraps it.
  private static DbcRepository mockDbcRepository;

  @Autowired
  DbcSyncService dbcSyncService;

  @MockitoBean
  ReferenceSyncService referenceSyncService;

  @MockitoBean
  private SqsTemplate sqsTemplate;

  @Autowired
  CacheManager cacheManager;

  private Cache dbcCache;

  private Dbc dbc;

  @BeforeEach
  void setup() {
    dbc = new Dbc();
    dbc.setTisId(DBC_FORDY);
    dbc.setOperation(Operation.DELETE);
    dbc.setTable(Dbc.ENTITY_NAME);

    dbcCache = cacheManager.getCache(Dbc.ENTITY_NAME);
  }

  @Test
  void shouldHitCacheOnSecondInvocation() {
    when(mockDbcRepository.findById(DBC_FORDY))
        .thenReturn(Optional.of(dbc), Optional.of(new Dbc()));
    assertThat(dbcCache.get(DBC_FORDY)).isNull();

    Optional<Dbc> actual1 = dbcSyncService.findById(DBC_FORDY);
    assertThat(dbcCache.get(DBC_FORDY)).isNotNull();
    Optional<Dbc> actual2 = dbcSyncService.findById(DBC_FORDY);

    verify(mockDbcRepository).findById(DBC_FORDY);
    assertThat(actual1).isPresent().get().isEqualTo(dbc).isEqualTo(actual2.orElseThrow());
  }

  @Test
  void shouldEvictWhenDeletedAndMissCacheOnNextInvocation() {
    Dbc otherDbc = new Dbc();
    final String otherKey = "Foo";
    otherDbc.setTisId(otherKey);
    otherDbc.setOperation(Operation.LOAD);
    when(mockDbcRepository.findById(otherKey)).thenReturn(Optional.of(otherDbc));
    dbcSyncService.findById(otherKey);
    assertThat(dbcCache.get(otherKey)).isNotNull();
    when(mockDbcRepository.findById(DBC_FORDY)).thenReturn(Optional.of(dbc));
    dbcSyncService.findById(DBC_FORDY);
    assertThat(dbcCache.get(DBC_FORDY)).isNotNull();

    dbcSyncService.syncRecord(dbc);
    assertThat(dbcCache.get(DBC_FORDY)).isNull();
    assertThat(dbcCache.get(otherKey)).isNotNull();
    verify(mockDbcRepository).deleteById(DBC_FORDY);

    dbcSyncService.findById(DBC_FORDY);
    assertThat(dbcCache.get(DBC_FORDY)).isNotNull();

    verify(mockDbcRepository, times(2)).findById(DBC_FORDY);
  }

  @Test
  void shouldReplaceCacheWhenSaved() {
    Dbc staleDbc = new Dbc();
    staleDbc.setTisId(DBC_FORDY);
    staleDbc.setTable("Stale");
    staleDbc.setOperation(Operation.UPDATE);
    when(mockDbcRepository.save(staleDbc)).thenReturn(staleDbc);
    dbcSyncService.syncRecord(staleDbc);
    assertThat(dbcCache.get(DBC_FORDY).get()).isEqualTo(staleDbc);

    Dbc updateDbc = new Dbc();
    updateDbc.setTable(Dbc.ENTITY_NAME);
    updateDbc.setTisId(DBC_FORDY);
    updateDbc.setOperation(Operation.UPDATE);
    when(mockDbcRepository.save(updateDbc)).thenReturn(dbc);

    dbcSyncService.syncRecord(updateDbc);
    assertThat(dbcCache.get(DBC_FORDY).get()).isEqualTo(dbc);
    verify(mockDbcRepository).save(staleDbc);
    verify(mockDbcRepository).save(updateDbc);
  }

  @TestConfiguration
  static class Configuration {

    @Primary
    @Bean
    DbcRepository mockDbcRepository() {
      mockDbcRepository = mock(DbcRepository.class);
      return mockDbcRepository;
    }

    ////// Mocks to enable application context //////
    @MockitoBean
    private MongoConfiguration mongoConfiguration;
    /////////////////////////////////////////////////
  }
}

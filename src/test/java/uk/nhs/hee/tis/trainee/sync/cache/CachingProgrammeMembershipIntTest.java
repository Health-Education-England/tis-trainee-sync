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

import io.awspring.cloud.autoconfigure.sqs.SqsAutoConfiguration;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.ProgrammeMembership;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.repository.ProgrammeMembershipRepository;
import uk.nhs.hee.tis.trainee.sync.service.ProgrammeMembershipSyncService;

@SpringBootTest(properties = {"cloud.aws.region.static=eu-west-2"})
@ActiveProfiles("int")
@EnableAutoConfiguration(exclude = SqsAutoConfiguration.class)
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
class CachingProgrammeMembershipIntTest {

  private static final UUID PROGRAMME_MEMBERSHIP_ID = UUID.randomUUID();

  // We require access to the mock before the proxy wraps it.
  private static ProgrammeMembershipRepository mockProgrammeMembershipRepository;

  @MockBean
  private SqsTemplate sqsTemplate;

  @Autowired
  ProgrammeMembershipSyncService programmeMembershipSyncService;

  @Autowired
  CacheManager cacheManager;

  private Cache programmeMembershipCache;

  private Record programmeMembershipRecord;

  private ProgrammeMembership programmeMembership;

  @BeforeEach
  void setup() {
    programmeMembershipRecord = new Record();
    programmeMembershipRecord.setTisId(PROGRAMME_MEMBERSHIP_ID.toString());
    programmeMembershipRecord.setOperation(Operation.DELETE);
    programmeMembershipRecord.setTable(ProgrammeMembership.ENTITY_NAME);
    programmeMembershipRecord.getData().put("uuid", PROGRAMME_MEMBERSHIP_ID.toString());

    programmeMembership = new ProgrammeMembership();

    programmeMembershipCache = cacheManager.getCache(ProgrammeMembership.ENTITY_NAME);
  }

  @Test
  void shouldHitCacheOnSecondInvocation() {
    when(mockProgrammeMembershipRepository.findById(PROGRAMME_MEMBERSHIP_ID))
        .thenReturn(Optional.of(programmeMembership), Optional.of(new ProgrammeMembership()));
    assertThat(programmeMembershipCache.get(PROGRAMME_MEMBERSHIP_ID)).isNull();

    Optional<ProgrammeMembership> actual1 =
        programmeMembershipSyncService.findById(PROGRAMME_MEMBERSHIP_ID.toString());
    assertThat(programmeMembershipCache.get(PROGRAMME_MEMBERSHIP_ID)).isNotNull();
    Optional<ProgrammeMembership> actual2 =
        programmeMembershipSyncService.findById(PROGRAMME_MEMBERSHIP_ID.toString());

    verify(mockProgrammeMembershipRepository).findById(PROGRAMME_MEMBERSHIP_ID);
    assertThat(actual1).isPresent().get()
        .isEqualTo(programmeMembership)
        .isEqualTo(actual2.orElseThrow());
  }

  @Test
  void shouldEvictWhenDeletedAndMissCacheOnNextInvocation() {
    ProgrammeMembership otherProgrammeMembership = new ProgrammeMembership();
    final UUID otherKey = UUID.randomUUID();
    otherProgrammeMembership.setUuid(otherKey);
    when(mockProgrammeMembershipRepository.findById(otherKey))
        .thenReturn(Optional.of(otherProgrammeMembership));
    programmeMembershipSyncService.findById(otherKey.toString());
    assertThat(programmeMembershipCache.get(otherKey)).isNotNull();
    when(mockProgrammeMembershipRepository.findById(PROGRAMME_MEMBERSHIP_ID))
        .thenReturn(Optional.of(programmeMembership));
    programmeMembershipSyncService.findById(PROGRAMME_MEMBERSHIP_ID.toString());
    assertThat(programmeMembershipCache.get(PROGRAMME_MEMBERSHIP_ID)).isNotNull();

    programmeMembershipSyncService.syncProgrammeMembership(programmeMembershipRecord);
    assertThat(programmeMembershipCache.get(PROGRAMME_MEMBERSHIP_ID)).isNull();
    assertThat(programmeMembershipCache.get(otherKey)).isNotNull();
    verify(mockProgrammeMembershipRepository).deleteById(PROGRAMME_MEMBERSHIP_ID);

    programmeMembershipSyncService.findById(PROGRAMME_MEMBERSHIP_ID.toString());
    assertThat(programmeMembershipCache.get(PROGRAMME_MEMBERSHIP_ID)).isNotNull();

    verify(mockProgrammeMembershipRepository, times(2))
        .findById(PROGRAMME_MEMBERSHIP_ID);
  }

  @Test
  void shouldReplaceCacheWhenSaved() {
    Record staleProgrammeMembershipRecord = new Record();
    staleProgrammeMembershipRecord.setTisId(PROGRAMME_MEMBERSHIP_ID.toString());
    staleProgrammeMembershipRecord.setTable("Stale");
    staleProgrammeMembershipRecord.setOperation(Operation.UPDATE);
    staleProgrammeMembershipRecord.getData().putAll(
        Map.of("uuid", PROGRAMME_MEMBERSHIP_ID.toString(), "leavingReason", "reason one"));

    ProgrammeMembership staleProgrammeMembership = new ProgrammeMembership();
    staleProgrammeMembership.setUuid(PROGRAMME_MEMBERSHIP_ID);
    staleProgrammeMembership.setLeavingReason("reason one");

    when(mockProgrammeMembershipRepository.save(staleProgrammeMembership))
        .thenReturn(staleProgrammeMembership);
    programmeMembershipSyncService.syncProgrammeMembership(staleProgrammeMembershipRecord);
    assertThat(programmeMembershipCache.get(PROGRAMME_MEMBERSHIP_ID).get())
        .isEqualTo(staleProgrammeMembership);

    Record updateProgrammeMembershipRecord = new Record();
    updateProgrammeMembershipRecord.setTable(ProgrammeMembership.ENTITY_NAME);
    updateProgrammeMembershipRecord.setTisId(PROGRAMME_MEMBERSHIP_ID.toString());
    updateProgrammeMembershipRecord.setOperation(Operation.UPDATE);
    updateProgrammeMembershipRecord.getData().putAll(
        Map.of("uuid", PROGRAMME_MEMBERSHIP_ID.toString(), "leavingReason", "reason two"));

    ProgrammeMembership updatedProgrammeMembership = new ProgrammeMembership();
    updatedProgrammeMembership.setUuid(PROGRAMME_MEMBERSHIP_ID);
    updatedProgrammeMembership.setLeavingReason("reason two");

    when(mockProgrammeMembershipRepository.save(updatedProgrammeMembership))
        .thenReturn(updatedProgrammeMembership);

    programmeMembershipSyncService.syncProgrammeMembership(updateProgrammeMembershipRecord);
    assertThat(programmeMembershipCache.get(PROGRAMME_MEMBERSHIP_ID).get())
        .isEqualTo(updatedProgrammeMembership);
    verify(mockProgrammeMembershipRepository).save(staleProgrammeMembership);
    verify(mockProgrammeMembershipRepository).save(updatedProgrammeMembership);
  }

  @TestConfiguration
  static class Configuration {

    @Primary
    @Bean
    ProgrammeMembershipRepository mockProgrammeMembershipRepository() {
      mockProgrammeMembershipRepository = mock(ProgrammeMembershipRepository.class);
      return mockProgrammeMembershipRepository;
    }

    ////// Mocks to enable application context //////
    @MockBean
    private MongoConfiguration mongoConfiguration;
    /////////////////////////////////////////////////
  }
}

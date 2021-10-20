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

import com.amazonaws.services.sqs.AmazonSQSAsync;
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
import uk.nhs.hee.tis.trainee.sync.model.ProgrammeMembership;
import uk.nhs.hee.tis.trainee.sync.repository.ProgrammeMembershipRepository;
import uk.nhs.hee.tis.trainee.sync.service.ProgrammeMembershipSyncService;

@SpringBootTest(properties = { "cloud.aws.region.static=eu-west-2" })
@EnableAutoConfiguration(exclude = SqsAutoConfiguration.class)
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
class CachingProgrammeMembershipIntTest {

  private static final String PROGRAMME_MEMBERSHIP_FORDY = "fordy";

  // We require access to the mock before the proxy wraps it.
  private static ProgrammeMembershipRepository mockProgrammeMembershipRepository;

  @MockBean
  private AmazonSQSAsync amazonSqsAsync;

  @Autowired
  ProgrammeMembershipSyncService programmeMembershipSyncService;

  @Autowired
  CacheManager cacheManager;

  private Cache programmeMembershipCache;

  private ProgrammeMembership programmeMembership;

  @BeforeEach
  void setup() {
    programmeMembership = new ProgrammeMembership();
    programmeMembership.setTisId(PROGRAMME_MEMBERSHIP_FORDY);
    programmeMembership.setOperation(Operation.DELETE);
    programmeMembership.setTable(ProgrammeMembership.ENTITY_NAME);

    programmeMembershipCache = cacheManager.getCache(ProgrammeMembership.ENTITY_NAME);
  }

  @Test
  void shouldHitCacheOnSecondInvocation() {
    when(mockProgrammeMembershipRepository.findById(PROGRAMME_MEMBERSHIP_FORDY))
        .thenReturn(Optional.of(programmeMembership), Optional.of(new ProgrammeMembership()));
    assertThat(programmeMembershipCache.get(PROGRAMME_MEMBERSHIP_FORDY)).isNull();

    Optional<ProgrammeMembership> actual1 =
        programmeMembershipSyncService.findById(PROGRAMME_MEMBERSHIP_FORDY);
    assertThat(programmeMembershipCache.get(PROGRAMME_MEMBERSHIP_FORDY)).isNotNull();
    Optional<ProgrammeMembership> actual2 =
        programmeMembershipSyncService.findById(PROGRAMME_MEMBERSHIP_FORDY);

    verify(mockProgrammeMembershipRepository).findById(PROGRAMME_MEMBERSHIP_FORDY);
    assertThat(actual1).isPresent().get()
        .isEqualTo(programmeMembership)
        .isEqualTo(actual2.orElseThrow());
  }

  @Test
  void shouldEvictWhenDeletedAndMissCacheOnNextInvocation() {
    ProgrammeMembership otherProgrammeMembership = new ProgrammeMembership();
    final String otherKey = "Foo";
    otherProgrammeMembership.setTisId(otherKey);
    otherProgrammeMembership.setOperation(Operation.LOAD);
    when(mockProgrammeMembershipRepository.findById(otherKey))
        .thenReturn(Optional.of(otherProgrammeMembership));
    programmeMembershipSyncService.findById(otherKey);
    assertThat(programmeMembershipCache.get(otherKey)).isNotNull();
    when(mockProgrammeMembershipRepository.findById(PROGRAMME_MEMBERSHIP_FORDY))
        .thenReturn(Optional.of(programmeMembership));
    programmeMembershipSyncService.findById(PROGRAMME_MEMBERSHIP_FORDY);
    assertThat(programmeMembershipCache.get(PROGRAMME_MEMBERSHIP_FORDY)).isNotNull();

    programmeMembershipSyncService.syncRecord(programmeMembership);
    assertThat(programmeMembershipCache.get(PROGRAMME_MEMBERSHIP_FORDY)).isNull();
    assertThat(programmeMembershipCache.get(otherKey)).isNotNull();
    verify(mockProgrammeMembershipRepository).deleteById(PROGRAMME_MEMBERSHIP_FORDY);

    programmeMembershipSyncService.findById(PROGRAMME_MEMBERSHIP_FORDY);
    assertThat(programmeMembershipCache.get(PROGRAMME_MEMBERSHIP_FORDY)).isNotNull();

    verify(mockProgrammeMembershipRepository, times(2))
        .findById(PROGRAMME_MEMBERSHIP_FORDY);
  }

  @Test
  void shouldReplaceCacheWhenSaved() {
    ProgrammeMembership staleProgrammeMembership = new ProgrammeMembership();
    staleProgrammeMembership.setTisId(PROGRAMME_MEMBERSHIP_FORDY);
    staleProgrammeMembership.setTable("Stale");
    staleProgrammeMembership.setOperation(Operation.UPDATE);
    when(mockProgrammeMembershipRepository.save(staleProgrammeMembership))
        .thenReturn(staleProgrammeMembership);
    programmeMembershipSyncService.syncRecord(staleProgrammeMembership);
    assertThat(programmeMembershipCache.get(PROGRAMME_MEMBERSHIP_FORDY).get())
        .isEqualTo(staleProgrammeMembership);

    ProgrammeMembership updateProgrammeMembership = new ProgrammeMembership();
    updateProgrammeMembership.setTable(ProgrammeMembership.ENTITY_NAME);
    updateProgrammeMembership.setTisId(PROGRAMME_MEMBERSHIP_FORDY);
    updateProgrammeMembership.setOperation(Operation.UPDATE);
    when(mockProgrammeMembershipRepository.save(updateProgrammeMembership))
        .thenReturn(programmeMembership);

    programmeMembershipSyncService.syncRecord(updateProgrammeMembership);
    assertThat(programmeMembershipCache.get(PROGRAMME_MEMBERSHIP_FORDY).get())
        .isEqualTo(programmeMembership);
    verify(mockProgrammeMembershipRepository).save(staleProgrammeMembership);
    verify(mockProgrammeMembershipRepository).save(updateProgrammeMembership);
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

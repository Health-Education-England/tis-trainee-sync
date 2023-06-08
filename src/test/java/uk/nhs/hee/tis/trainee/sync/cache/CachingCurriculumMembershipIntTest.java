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
import org.springframework.test.context.ActiveProfiles;
import uk.nhs.hee.tis.trainee.sync.config.MongoConfiguration;
import uk.nhs.hee.tis.trainee.sync.model.CurriculumMembership;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.repository.CurriculumMembershipRepository;
import uk.nhs.hee.tis.trainee.sync.service.CurriculumMembershipSyncService;

@SpringBootTest(properties = { "cloud.aws.region.static=eu-west-2" })
@ActiveProfiles("int")
@EnableAutoConfiguration(exclude = SqsAutoConfiguration.class)
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
class CachingCurriculumMembershipIntTest {

  private static final String CURRICULUM_MEMBERSHIP_FORDY = "fordy";

  // We require access to the mock before the proxy wraps it.
  private static CurriculumMembershipRepository mockCurriculumMembershipRepository;

  @MockBean
  private AmazonSQSAsync amazonSqsAsync;

  @Autowired
  CurriculumMembershipSyncService curriculumMembershipSyncService;

  @Autowired
  CacheManager cacheManager;

  private Cache curriculumMembershipCache;

  private CurriculumMembership curriculumMembership;

  @BeforeEach
  void setup() {
    curriculumMembership = new CurriculumMembership();
    curriculumMembership.setTisId(CURRICULUM_MEMBERSHIP_FORDY);
    curriculumMembership.setOperation(Operation.DELETE);
    curriculumMembership.setTable(CurriculumMembership.ENTITY_NAME);

    curriculumMembershipCache = cacheManager.getCache(CurriculumMembership.ENTITY_NAME);
  }

  @Test
  void shouldHitCacheOnSecondInvocation() {
    when(mockCurriculumMembershipRepository.findById(CURRICULUM_MEMBERSHIP_FORDY))
        .thenReturn(Optional.of(curriculumMembership), Optional.of(new CurriculumMembership()));
    assertThat(curriculumMembershipCache.get(CURRICULUM_MEMBERSHIP_FORDY)).isNull();

    Optional<CurriculumMembership> actual1 =
        curriculumMembershipSyncService.findById(CURRICULUM_MEMBERSHIP_FORDY);
    assertThat(curriculumMembershipCache.get(CURRICULUM_MEMBERSHIP_FORDY)).isNotNull();
    Optional<CurriculumMembership> actual2 =
        curriculumMembershipSyncService.findById(CURRICULUM_MEMBERSHIP_FORDY);

    verify(mockCurriculumMembershipRepository).findById(CURRICULUM_MEMBERSHIP_FORDY);
    assertThat(actual1).isPresent().get()
        .isEqualTo(curriculumMembership)
        .isEqualTo(actual2.orElseThrow());
  }

  @Test
  void shouldEvictWhenDeletedAndMissCacheOnNextInvocation() {
    CurriculumMembership otherCurriculumMembership = new CurriculumMembership();
    final String otherKey = "Foo";
    otherCurriculumMembership.setTisId(otherKey);
    otherCurriculumMembership.setOperation(Operation.LOAD);
    when(mockCurriculumMembershipRepository.findById(otherKey))
        .thenReturn(Optional.of(otherCurriculumMembership));
    curriculumMembershipSyncService.findById(otherKey);
    assertThat(curriculumMembershipCache.get(otherKey)).isNotNull();
    when(mockCurriculumMembershipRepository.findById(CURRICULUM_MEMBERSHIP_FORDY))
        .thenReturn(Optional.of(curriculumMembership));
    curriculumMembershipSyncService.findById(CURRICULUM_MEMBERSHIP_FORDY);
    assertThat(curriculumMembershipCache.get(CURRICULUM_MEMBERSHIP_FORDY)).isNotNull();

    curriculumMembershipSyncService.syncCurriculumMembership(curriculumMembership);
    assertThat(curriculumMembershipCache.get(CURRICULUM_MEMBERSHIP_FORDY)).isNull();
    assertThat(curriculumMembershipCache.get(otherKey)).isNotNull();
    verify(mockCurriculumMembershipRepository).deleteById(CURRICULUM_MEMBERSHIP_FORDY);

    curriculumMembershipSyncService.findById(CURRICULUM_MEMBERSHIP_FORDY);
    assertThat(curriculumMembershipCache.get(CURRICULUM_MEMBERSHIP_FORDY)).isNotNull();

    verify(mockCurriculumMembershipRepository, times(2))
        .findById(CURRICULUM_MEMBERSHIP_FORDY);
  }

  @Test
  void shouldReplaceCacheWhenSaved() {
    CurriculumMembership staleCurriculumMembership = new CurriculumMembership();
    staleCurriculumMembership.setTisId(CURRICULUM_MEMBERSHIP_FORDY);
    staleCurriculumMembership.setTable("Stale");
    staleCurriculumMembership.setOperation(Operation.UPDATE);
    when(mockCurriculumMembershipRepository.save(staleCurriculumMembership))
        .thenReturn(staleCurriculumMembership);
    curriculumMembershipSyncService.syncCurriculumMembership(staleCurriculumMembership);
    assertThat(curriculumMembershipCache.get(CURRICULUM_MEMBERSHIP_FORDY).get())
        .isEqualTo(staleCurriculumMembership);

    CurriculumMembership updateCurriculumMembership = new CurriculumMembership();
    updateCurriculumMembership.setTable(CurriculumMembership.ENTITY_NAME);
    updateCurriculumMembership.setTisId(CURRICULUM_MEMBERSHIP_FORDY);
    updateCurriculumMembership.setOperation(Operation.UPDATE);
    when(mockCurriculumMembershipRepository.save(updateCurriculumMembership))
        .thenReturn(curriculumMembership);

    curriculumMembershipSyncService.syncCurriculumMembership(updateCurriculumMembership);
    assertThat(curriculumMembershipCache.get(CURRICULUM_MEMBERSHIP_FORDY).get())
        .isEqualTo(curriculumMembership);
    verify(mockCurriculumMembershipRepository).save(staleCurriculumMembership);
    verify(mockCurriculumMembershipRepository).save(updateCurriculumMembership);
  }

  @TestConfiguration
  static class Configuration {

    @Primary
    @Bean
    CurriculumMembershipRepository mockCurriculumMembershipRepository() {
      mockCurriculumMembershipRepository = mock(CurriculumMembershipRepository.class);
      return mockCurriculumMembershipRepository;
    }

    ////// Mocks to enable application context //////
    @MockBean
    private MongoConfiguration mongoConfiguration;
    /////////////////////////////////////////////////
  }
}

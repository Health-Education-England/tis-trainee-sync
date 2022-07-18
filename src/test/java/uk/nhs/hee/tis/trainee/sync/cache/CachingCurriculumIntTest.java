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
import uk.nhs.hee.tis.trainee.sync.model.Curriculum;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.repository.CurriculumRepository;
import uk.nhs.hee.tis.trainee.sync.service.CurriculumSyncService;
import uk.nhs.hee.tis.trainee.sync.service.ReferenceSyncService;

@SpringBootTest(properties = { "cloud.aws.region.static=eu-west-2" })
@ActiveProfiles("int")
@EnableAutoConfiguration(exclude = SqsAutoConfiguration.class)
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
class CachingCurriculumIntTest {

  private static final String CURRICULUM_FORDY = "fordy";

  // We require access to the mock before the proxy wraps it.
  private static CurriculumRepository mockCurriculumRepository;

  @Autowired
  CurriculumSyncService curriculumSyncService;

  @MockBean
  ReferenceSyncService referenceSyncService;

  @MockBean
  private AmazonSQSAsync amazonSqsAsync;

  @Autowired
  CacheManager cacheManager;

  private Cache curriculumCache;

  private Curriculum curriculum;

  @BeforeEach
  void setup() {
    curriculum = new Curriculum();
    curriculum.setTisId(CURRICULUM_FORDY);
    curriculum.setOperation(Operation.DELETE);
    curriculum.setTable(Curriculum.ENTITY_NAME);

    curriculumCache = cacheManager.getCache(Curriculum.ENTITY_NAME);
  }

  @Test
  void shouldHitCacheOnSecondInvocation() {
    when(mockCurriculumRepository.findById(CURRICULUM_FORDY))
        .thenReturn(Optional.of(curriculum), Optional.of(new Curriculum()));
    assertThat(curriculumCache.get(CURRICULUM_FORDY)).isNull();

    Optional<Curriculum> actual1 = curriculumSyncService.findById(CURRICULUM_FORDY);
    assertThat(curriculumCache.get(CURRICULUM_FORDY)).isNotNull();
    Optional<Curriculum> actual2 = curriculumSyncService.findById(CURRICULUM_FORDY);

    verify(mockCurriculumRepository).findById(CURRICULUM_FORDY);
    assertThat(actual1).isPresent().get().isEqualTo(curriculum).isEqualTo(actual2.orElseThrow());
  }

  @Test
  void shouldEvictWhenDeletedAndMissCacheOnNextInvocation() {
    Curriculum otherCurriculum = new Curriculum();
    final String otherKey = "Foo";
    otherCurriculum.setTisId(otherKey);
    otherCurriculum.setOperation(Operation.LOAD);
    when(mockCurriculumRepository.findById(otherKey)).thenReturn(Optional.of(otherCurriculum));
    curriculumSyncService.findById(otherKey);
    assertThat(curriculumCache.get(otherKey)).isNotNull();
    when(mockCurriculumRepository.findById(CURRICULUM_FORDY)).thenReturn(Optional.of(curriculum));
    curriculumSyncService.findById(CURRICULUM_FORDY);
    assertThat(curriculumCache.get(CURRICULUM_FORDY)).isNotNull();

    curriculumSyncService.syncRecord(curriculum);
    assertThat(curriculumCache.get(CURRICULUM_FORDY)).isNull();
    assertThat(curriculumCache.get(otherKey)).isNotNull();
    verify(mockCurriculumRepository).deleteById(CURRICULUM_FORDY);

    curriculumSyncService.findById(CURRICULUM_FORDY);
    assertThat(curriculumCache.get(CURRICULUM_FORDY)).isNotNull();

    verify(mockCurriculumRepository, times(2)).findById(CURRICULUM_FORDY);
  }

  @Test
  void shouldReplaceCacheWhenSaved() {
    Curriculum staleCurriculum = new Curriculum();
    staleCurriculum.setTisId(CURRICULUM_FORDY);
    staleCurriculum.setTable("Stale");
    staleCurriculum.setOperation(Operation.UPDATE);
    when(mockCurriculumRepository.save(staleCurriculum)).thenReturn(staleCurriculum);
    curriculumSyncService.syncRecord(staleCurriculum);
    assertThat(curriculumCache.get(CURRICULUM_FORDY).get()).isEqualTo(staleCurriculum);

    Curriculum updateCurriculum = new Curriculum();
    updateCurriculum.setTable(Curriculum.ENTITY_NAME);
    updateCurriculum.setTisId(CURRICULUM_FORDY);
    updateCurriculum.setOperation(Operation.UPDATE);
    when(mockCurriculumRepository.save(updateCurriculum)).thenReturn(curriculum);

    curriculumSyncService.syncRecord(updateCurriculum);
    assertThat(curriculumCache.get(CURRICULUM_FORDY).get()).isEqualTo(curriculum);
    verify(mockCurriculumRepository).save(staleCurriculum);
    verify(mockCurriculumRepository).save(updateCurriculum);
  }

  @TestConfiguration
  static class Configuration {

    @Primary
    @Bean
    CurriculumRepository mockCurriculumRepository() {
      mockCurriculumRepository = mock(CurriculumRepository.class);
      return mockCurriculumRepository;
    }

    ////// Mocks to enable application context //////
    @MockBean
    private MongoConfiguration mongoConfiguration;
    /////////////////////////////////////////////////
  }
}

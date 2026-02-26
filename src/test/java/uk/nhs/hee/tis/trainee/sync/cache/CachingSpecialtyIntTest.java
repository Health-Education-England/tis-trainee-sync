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
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.Specialty;
import uk.nhs.hee.tis.trainee.sync.repository.SpecialtyRepository;
import uk.nhs.hee.tis.trainee.sync.service.SpecialtySyncService;

@SpringBootTest(properties = {"cloud.aws.region.static=eu-west-2"})
@ActiveProfiles("int")
@EnableAutoConfiguration(exclude = SqsAutoConfiguration.class)
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
class CachingSpecialtyIntTest {

  private static final String SPECIALTY_FORDY = "fordy";

  // We require access to the mock before the proxy wraps it.
  private static SpecialtyRepository mockSpecialtyRepository;

  @MockitoBean
  private SqsTemplate sqsTemplate;

  @Autowired
  SpecialtySyncService specialtySyncService;

  @Autowired
  CacheManager cacheManager;

  private Cache specialtyCache;

  private Specialty specialty;

  @BeforeEach
  void setup() {
    specialty = new Specialty();
    specialty.setTisId(SPECIALTY_FORDY);
    specialty.setOperation(Operation.DELETE);
    specialty.setTable(Specialty.ENTITY_NAME);

    specialtyCache = cacheManager.getCache(Specialty.ENTITY_NAME);
  }

  @Test
  void shouldHitCacheOnSecondInvocation() {
    when(mockSpecialtyRepository.findById(SPECIALTY_FORDY))
        .thenReturn(Optional.of(specialty), Optional.of(new Specialty()));
    assertThat(specialtyCache.get(SPECIALTY_FORDY)).isNull();

    Optional<Specialty> actual1 = specialtySyncService.findById(SPECIALTY_FORDY);
    assertThat(specialtyCache.get(SPECIALTY_FORDY)).isNotNull();
    Optional<Specialty> actual2 = specialtySyncService.findById(SPECIALTY_FORDY);

    verify(mockSpecialtyRepository).findById(SPECIALTY_FORDY);
    assertThat(actual1).isPresent().get().isEqualTo(specialty).isEqualTo(actual2.orElseThrow());
  }

  @Test
  void shouldEvictWhenDeletedAndMissCacheOnNextInvocation() {
    Specialty otherSpecialty = new Specialty();
    final String otherKey = "Foo";
    otherSpecialty.setTisId(otherKey);
    otherSpecialty.setOperation(Operation.LOAD);
    when(mockSpecialtyRepository.findById(otherKey)).thenReturn(Optional.of(otherSpecialty));
    specialtySyncService.findById(otherKey);
    assertThat(specialtyCache.get(otherKey)).isNotNull();
    when(mockSpecialtyRepository.findById(SPECIALTY_FORDY)).thenReturn(Optional.of(specialty));
    specialtySyncService.findById(SPECIALTY_FORDY);
    assertThat(specialtyCache.get(SPECIALTY_FORDY)).isNotNull();

    specialtySyncService.syncRecord(specialty);
    assertThat(specialtyCache.get(SPECIALTY_FORDY)).isNull();
    assertThat(specialtyCache.get(otherKey)).isNotNull();
    verify(mockSpecialtyRepository).deleteById(SPECIALTY_FORDY);

    specialtySyncService.findById(SPECIALTY_FORDY);
    assertThat(specialtyCache.get(SPECIALTY_FORDY)).isNotNull();

    verify(mockSpecialtyRepository, times(2)).findById(SPECIALTY_FORDY);
  }

  @Test
  void shouldReplaceCacheWhenSaved() {
    Specialty staleSpecialty = new Specialty();
    staleSpecialty.setTisId(SPECIALTY_FORDY);
    staleSpecialty.setTable("Stale");
    staleSpecialty.setOperation(Operation.UPDATE);
    when(mockSpecialtyRepository.save(staleSpecialty)).thenReturn(staleSpecialty);
    specialtySyncService.syncRecord(staleSpecialty);
    assertThat(specialtyCache.get(SPECIALTY_FORDY).get()).isEqualTo(staleSpecialty);

    Specialty updateSpecialty = new Specialty();
    updateSpecialty.setTable(Specialty.ENTITY_NAME);
    updateSpecialty.setTisId(SPECIALTY_FORDY);
    updateSpecialty.setOperation(Operation.UPDATE);
    when(mockSpecialtyRepository.save(updateSpecialty)).thenReturn(specialty);

    specialtySyncService.syncRecord(updateSpecialty);
    assertThat(specialtyCache.get(SPECIALTY_FORDY).get()).isEqualTo(specialty);
    verify(mockSpecialtyRepository).save(staleSpecialty);
    verify(mockSpecialtyRepository).save(updateSpecialty);
  }

  @TestConfiguration
  static class Configuration {

    ////// Mocks to enable application context //////
    @MockitoBean
    private MongoConfiguration mongoConfiguration;

    @Primary
    @Bean
    SpecialtyRepository mockSpecialtyRepository() {
      mockSpecialtyRepository = mock(SpecialtyRepository.class);
      return mockSpecialtyRepository;
    }
    /////////////////////////////////////////////////
  }
}

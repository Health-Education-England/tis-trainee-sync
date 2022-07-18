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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
import uk.nhs.hee.tis.trainee.sync.model.PlacementSpecialty;
import uk.nhs.hee.tis.trainee.sync.repository.PlacementSpecialtyRepository;
import uk.nhs.hee.tis.trainee.sync.service.PlacementSpecialtySyncService;

@Disabled
@SpringBootTest
@EnableAutoConfiguration(exclude = SqsAutoConfiguration.class)
@ActiveProfiles("int")
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
class CachingPlacementSpecialtyIntTest {

  private static final String PLACEMENT_SPECIALTY_FORDY = "fordy";
  private static final String PLACEMENT_SPECIALTY_SPECIALTY_ID = "bar";

  private static final String DATA_PLACEMENT_SPECIALTY_SPECIALTY_TYPE = "placementSpecialtyType";
  private static final String DATA_PLACEMENT_SPECIALTY_PLACEMENT_ID = "placementId";
  private static final String DATA_PLACEMENT_SPECIALTY_SPECIALTY_ID = "specialtyId";

  private static final String PLACEMENT_DATA_SPECIALTY_TYPE_PRIMARY = "PRIMARY";

  // We require access to the mock before the proxy wraps it.
  private static PlacementSpecialtyRepository mockPlacementSpecialtyRepository;

  @Autowired
  PlacementSpecialtySyncService placementSpecialtySyncService;

  @Autowired
  CacheManager cacheManager;

  private Cache placementSpecialtyCache;

  private PlacementSpecialty placementSpecialty;

  @BeforeEach
  void setup() {
    placementSpecialty = new PlacementSpecialty();
    placementSpecialty.setTisId(PLACEMENT_SPECIALTY_FORDY);
    placementSpecialty.setOperation(Operation.DELETE);
    placementSpecialty.setTable(PlacementSpecialty.ENTITY_NAME);
    placementSpecialty.setData(new HashMap<>(Map.of(
        DATA_PLACEMENT_SPECIALTY_PLACEMENT_ID, PLACEMENT_SPECIALTY_FORDY,
        DATA_PLACEMENT_SPECIALTY_SPECIALTY_ID, PLACEMENT_SPECIALTY_SPECIALTY_ID
    )));

    placementSpecialtyCache = cacheManager.getCache(PlacementSpecialty.ENTITY_NAME);
  }

  @Test
  void shouldHitCacheOnSecondInvocation() {
    when(mockPlacementSpecialtyRepository.findById(PLACEMENT_SPECIALTY_FORDY))
        .thenReturn(Optional.of(placementSpecialty), Optional.of(new PlacementSpecialty()));
    assertThat(placementSpecialtyCache.get(PLACEMENT_SPECIALTY_FORDY)).isNull();

    Optional<PlacementSpecialty> actual1 =
        placementSpecialtySyncService.findById(PLACEMENT_SPECIALTY_FORDY);
    assertThat(placementSpecialtyCache.get(PLACEMENT_SPECIALTY_FORDY)).isNotNull();
    Optional<PlacementSpecialty> actual2 =
        placementSpecialtySyncService.findById(PLACEMENT_SPECIALTY_FORDY);

    verify(mockPlacementSpecialtyRepository).findById(PLACEMENT_SPECIALTY_FORDY);
    assertThat(actual1).isPresent().get()
        .isEqualTo(placementSpecialty)
        .isEqualTo(actual2.orElseThrow());
  }

  @Test
  void shouldEvictWhenDeletedAndMissCacheOnNextInvocation() {
    PlacementSpecialty otherPlacementSpecialty = new PlacementSpecialty();
    final String otherKey = "Foo";
    otherPlacementSpecialty.setTisId(otherKey);
    otherPlacementSpecialty.setOperation(Operation.LOAD);
    when(mockPlacementSpecialtyRepository.findById(otherKey))
        .thenReturn(Optional.of(otherPlacementSpecialty));

    placementSpecialtySyncService.findById(otherKey);
    assertThat(placementSpecialtyCache.get(otherKey)).isNotNull();

    when(mockPlacementSpecialtyRepository.findById(PLACEMENT_SPECIALTY_FORDY))
        .thenReturn(Optional.of(placementSpecialty));

    placementSpecialtySyncService.findById(PLACEMENT_SPECIALTY_FORDY);
    assertThat(placementSpecialtyCache.get(PLACEMENT_SPECIALTY_FORDY)).isNotNull();

    placementSpecialtySyncService.syncRecord(placementSpecialty);
    assertThat(placementSpecialtyCache.get(PLACEMENT_SPECIALTY_FORDY)).isNull();
    assertThat(placementSpecialtyCache.get(otherKey)).isNotNull();
    verify(mockPlacementSpecialtyRepository).deleteById(PLACEMENT_SPECIALTY_FORDY);

    placementSpecialtySyncService.findById(PLACEMENT_SPECIALTY_FORDY);
    assertThat(placementSpecialtyCache.get(PLACEMENT_SPECIALTY_FORDY)).isNotNull();

    verify(mockPlacementSpecialtyRepository, times(2))
        .findById(PLACEMENT_SPECIALTY_FORDY);
  }

  @Test
  void shouldReplaceCacheWhenSaved() {
    PlacementSpecialty stalePlacementSpecialty = new PlacementSpecialty();
    stalePlacementSpecialty.setTisId(PLACEMENT_SPECIALTY_FORDY);
    stalePlacementSpecialty.setTable("Stale");
    stalePlacementSpecialty.setOperation(Operation.UPDATE);
    stalePlacementSpecialty.setData(new HashMap<>(Map.of(
        DATA_PLACEMENT_SPECIALTY_SPECIALTY_TYPE, PLACEMENT_DATA_SPECIALTY_TYPE_PRIMARY,
        DATA_PLACEMENT_SPECIALTY_PLACEMENT_ID, PLACEMENT_SPECIALTY_FORDY)));
    when(mockPlacementSpecialtyRepository.save(stalePlacementSpecialty))
        .thenReturn(stalePlacementSpecialty);
    placementSpecialtySyncService.syncRecord(stalePlacementSpecialty);
    assertThat(placementSpecialtyCache.get(PLACEMENT_SPECIALTY_FORDY).get())
        .isEqualTo(stalePlacementSpecialty);

    PlacementSpecialty updatePlacementSpecialty = new PlacementSpecialty();
    updatePlacementSpecialty.setTable(PlacementSpecialty.ENTITY_NAME);
    updatePlacementSpecialty.setTisId(PLACEMENT_SPECIALTY_FORDY);
    updatePlacementSpecialty.setOperation(Operation.UPDATE);
    updatePlacementSpecialty.setData(new HashMap<>(Map.of(
        DATA_PLACEMENT_SPECIALTY_SPECIALTY_TYPE, PLACEMENT_DATA_SPECIALTY_TYPE_PRIMARY,
        DATA_PLACEMENT_SPECIALTY_PLACEMENT_ID, PLACEMENT_SPECIALTY_FORDY)));

    when(mockPlacementSpecialtyRepository.save(updatePlacementSpecialty))
        .thenReturn(placementSpecialty);

    placementSpecialtySyncService.syncRecord(updatePlacementSpecialty);
    assertThat(placementSpecialtyCache.get(PLACEMENT_SPECIALTY_FORDY).get())
        .isEqualTo(placementSpecialty);
    verify(mockPlacementSpecialtyRepository).save(stalePlacementSpecialty);
    verify(mockPlacementSpecialtyRepository).save(updatePlacementSpecialty);
  }

  @TestConfiguration
  static class Configuration {

    ////// Mocks to enable application context //////
    @MockBean
    private MongoConfiguration mongoConfiguration;
    /////////////////////////////////////////////////

    @Primary
    @Bean
    PlacementSpecialtyRepository mockPlacementSpecialtyRepository() {
      mockPlacementSpecialtyRepository = mock(PlacementSpecialtyRepository.class);
      return mockPlacementSpecialtyRepository;
    }
  }
}

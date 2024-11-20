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

package uk.nhs.hee.tis.trainee.sync.event;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.nhs.hee.tis.trainee.sync.model.Operation.LOOKUP;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeDeleteEvent;
import uk.nhs.hee.tis.trainee.sync.model.Placement;
import uk.nhs.hee.tis.trainee.sync.model.PlacementSpecialty;
import uk.nhs.hee.tis.trainee.sync.service.FifoMessagingService;
import uk.nhs.hee.tis.trainee.sync.service.PlacementSpecialtySyncService;
import uk.nhs.hee.tis.trainee.sync.service.PlacementSyncService;

class PlacementSpecialtyEventListenerTest {

  private static final String PLACEMENT_QUEUE_URL = "https://queue.placement";
  private static final String PLACEMENT_ID = "placement1";
  private static final String PLACEMENT_ID_2 = "placement2";

  private PlacementSpecialtyEventListener listener;
  private PlacementSpecialtySyncService placementSpecialtyService;
  private PlacementSyncService placementService;
  private FifoMessagingService fifoMessagingService;
  private Cache cache;

  @BeforeEach
  void setUp() {
    placementSpecialtyService = mock(PlacementSpecialtySyncService.class);
    placementService = mock(PlacementSyncService.class);
    fifoMessagingService = mock(FifoMessagingService.class);

    CacheManager cacheManager = mock(CacheManager.class);
    cache = mock(Cache.class);
    when(cacheManager.getCache(anyString())).thenReturn(cache);

    listener = new PlacementSpecialtyEventListener(placementSpecialtyService, placementService,
        fifoMessagingService, PLACEMENT_QUEUE_URL, cacheManager);
  }

  @Test
  void shouldNotInteractWithPlacementQueueAfterSaveWhenNoRelatedPlacements() {
    PlacementSpecialty placementSpecialty = new PlacementSpecialty();
    placementSpecialty.setData(Collections.singletonMap("placementId", PLACEMENT_ID));
    AfterSaveEvent<PlacementSpecialty> event = new AfterSaveEvent<>(
        placementSpecialty, null, null);

    listener.onAfterSave(event);

    verify(placementService).findById("placement1");
    verifyNoMoreInteractions(placementService);
    verifyNoInteractions(fifoMessagingService);
  }

  @Test
  void shouldSendRelatedPlacementToQueueAfterSaveWhenPlacementFound() {
    PlacementSpecialty placementSpecialty = new PlacementSpecialty();
    placementSpecialty.setData(Collections.singletonMap("placementId", PLACEMENT_ID));

    Placement placement = new Placement();
    placement.setTisId(PLACEMENT_ID);

    when(placementService.findById(PLACEMENT_ID)).thenReturn(Optional.of(placement));

    AfterSaveEvent<PlacementSpecialty> event = new AfterSaveEvent<>(
        placementSpecialty, null, null);
    listener.onAfterSave(event);

    verify(fifoMessagingService).sendMessageToFifoQueue(
        eq(PLACEMENT_QUEUE_URL), eq(placement), any());
    assertThat("Unexpected table operation.", placement.getOperation(), is(LOOKUP));
    verify(placementService, never()).request(PLACEMENT_ID);
  }

  @Test
  void shouldFindAndCachePlacementSpecialtyIfNotInCacheBeforeDelete() {
    PlacementSpecialty placementSpecialty = new PlacementSpecialty();
    placementSpecialty.setTisId(PLACEMENT_ID);
    when(cache.get(PLACEMENT_ID, PlacementSpecialty.class)).thenReturn(null);
    when(placementSpecialtyService.findById(any())).thenReturn(Optional.of(placementSpecialty));

    Document document = new Document();
    document.append("_id", PLACEMENT_ID);
    BeforeDeleteEvent<PlacementSpecialty> event =
        new BeforeDeleteEvent<>(document, null, null);

    listener.onBeforeDelete(event);

    verify(placementSpecialtyService).findById(PLACEMENT_ID);
    verify(cache).put(PLACEMENT_ID, placementSpecialty);
    verifyNoInteractions(fifoMessagingService);
  }

  @Test
  void shouldNotFindAndCachePlacementSpecialtyIfInCacheBeforeDelete() {
    Document document = new Document();
    document.append("_id", PLACEMENT_ID);
    BeforeDeleteEvent<PlacementSpecialty> event =
        new BeforeDeleteEvent<>(document, null, null);

    PlacementSpecialty placementSpecialty = new PlacementSpecialty();
    when(cache.get(PLACEMENT_ID, PlacementSpecialty.class)).thenReturn(placementSpecialty);

    listener.onBeforeDelete(event);

    verifyNoInteractions(placementSpecialtyService);
    verifyNoInteractions(fifoMessagingService);
  }

  @Test
  void shouldNotQueueRelatedPlacementWhenPlacementSpecialtyNotInCacheAfterDelete() {
    Document document = new Document();
    document.append("_id", PLACEMENT_ID);
    AfterDeleteEvent<PlacementSpecialty> event =
        new AfterDeleteEvent<>(document, null, null);

    when(cache.get(PLACEMENT_ID, PlacementSpecialty.class)).thenReturn(null);

    listener.onAfterDelete(event);

    verifyNoInteractions(fifoMessagingService);
  }

  @Test
  void shouldNotQueueRelatedPlacementWhenPlacementNotFoundAfterDelete() {
    PlacementSpecialty placementSpecialty = new PlacementSpecialty();

    when(cache.get(PLACEMENT_ID, PlacementSpecialty.class)).thenReturn(placementSpecialty);
    when(placementService.findById(any())).thenReturn(Optional.empty());

    Document document = new Document();
    document.append("_id", PLACEMENT_ID);
    AfterDeleteEvent<PlacementSpecialty> event =
        new AfterDeleteEvent<>(document, null, null);

    listener.onAfterDelete(event);

    verifyNoInteractions(fifoMessagingService);
  }

  @Test
  void shouldQueueRelatedPlacementWhenPlacementFoundAfterDelete() {
    PlacementSpecialty placementSpecialty = new PlacementSpecialty();
    placementSpecialty.setData(new HashMap<>(Map.of(
        "placementId", PLACEMENT_ID_2)));
    when(cache.get(PLACEMENT_ID, PlacementSpecialty.class)).thenReturn(placementSpecialty);

    Placement placement = new Placement();
    placement.setTisId(PLACEMENT_ID_2);
    when(placementService.findById(PLACEMENT_ID_2)).thenReturn(Optional.of(placement));

    Document document = new Document();
    document.append("_id", PLACEMENT_ID);
    AfterDeleteEvent<PlacementSpecialty> event = new AfterDeleteEvent<>(document, null, null);

    listener.onAfterDelete(event);

    verify(fifoMessagingService).sendMessageToFifoQueue(
        eq(PLACEMENT_QUEUE_URL), eq(placement), any());

    assertThat("Unexpected operation.", placement.getOperation(), is(LOOKUP));
  }
}

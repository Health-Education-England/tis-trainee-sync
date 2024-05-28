/*
 * The MIT License (MIT)
 *
 * Copyright 2023 Crown Copyright (Health Education England)
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import java.util.Optional;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeDeleteEvent;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.Placement;
import uk.nhs.hee.tis.trainee.sync.model.PlacementSite;
import uk.nhs.hee.tis.trainee.sync.service.FifoMessagingService;
import uk.nhs.hee.tis.trainee.sync.service.PlacementSiteSyncService;
import uk.nhs.hee.tis.trainee.sync.service.PlacementSyncService;

class PlacementSiteEventListenerTest {

  private static final String PLACEMENT_QUEUE_URL = "https://queue.placement";

  private PlacementSiteEventListener listener;
  private PlacementSyncService placementService;
  private PlacementSiteSyncService placementSiteService;
  private FifoMessagingService fifoMessagingService;
  private Cache cache;

  @BeforeEach
  void setUp() {
    placementSiteService = mock(PlacementSiteSyncService.class);
    placementService = mock(PlacementSyncService.class);
    fifoMessagingService = mock(FifoMessagingService.class);

    CacheManager cacheManager = mock(CacheManager.class);
    cache = mock(Cache.class);
    when(cacheManager.getCache(anyString())).thenReturn(cache);

    listener = new PlacementSiteEventListener(placementSiteService, placementService,
        fifoMessagingService, PLACEMENT_QUEUE_URL, cacheManager);
  }

  @Test
  void shouldRequestPlacementWhenMissingAfterSave() {
    PlacementSite placementSite = new PlacementSite();
    placementSite.setId(1L);
    placementSite.setPlacementId(2L);
    AfterSaveEvent<PlacementSite> event = new AfterSaveEvent<>(placementSite, null, null);

    when(placementService.findById("2")).thenReturn(Optional.empty());

    listener.onAfterSave(event);

    verify(placementService).request("2");
    verifyNoInteractions(fifoMessagingService);
  }

  @Test
  void shouldQueuePlacementWhenFoundAfterSave() {
    PlacementSite placementSite = new PlacementSite();
    placementSite.setId(1L);
    placementSite.setPlacementId(2L);
    AfterSaveEvent<PlacementSite> event = new AfterSaveEvent<>(placementSite, null, null);

    Placement placement = new Placement();
    placement.setTisId("2");
    when(placementService.findById("2")).thenReturn(Optional.of(placement));

    listener.onAfterSave(event);

    verify(placementService, never()).request(any());
    verify(fifoMessagingService).sendMessageToFifoQueue(PLACEMENT_QUEUE_URL, placement);

    assertThat("Unexpected operation.", placement.getOperation(), is(Operation.LOAD));
  }

  @Test
  void shouldFindAndCachePlacementSiteIfNotInCacheBeforeDelete() {
    PlacementSite placementSite = new PlacementSite();
    placementSite.setId(1L);
    when(cache.get(1L, PlacementSite.class)).thenReturn(null);
    when(placementSiteService.findById(any())).thenReturn(Optional.of(placementSite));

    Document document = new Document();
    document.append("_id", 1L);
    BeforeDeleteEvent<PlacementSite> event = new BeforeDeleteEvent<>(document, null, null);

    listener.onBeforeDelete(event);

    verify(placementSiteService).findById(1L);
    verify(cache).put(1L, placementSite);
    verifyNoInteractions(fifoMessagingService);
  }

  @Test
  void shouldNotFindAndCachePlacementSiteIfInCacheBeforeDelete() {
    Document document = new Document();
    document.append("_id", 1L);
    BeforeDeleteEvent<PlacementSite> event = new BeforeDeleteEvent<>(document, null, null);

    PlacementSite placementSite = new PlacementSite();
    when(cache.get(1L, PlacementSite.class)).thenReturn(placementSite);

    listener.onBeforeDelete(event);

    verifyNoInteractions(placementSiteService);
    verifyNoInteractions(fifoMessagingService);
  }

  @Test
  void shouldNotQueueRelatedPlacementWhenPlacementSiteNotInCacheAfterDelete() {
    Document document = new Document();
    document.append("_id", 1L);
    AfterDeleteEvent<PlacementSite> event = new AfterDeleteEvent<>(document, null, null);

    when(cache.get(1L, PlacementSite.class)).thenReturn(null);

    listener.onAfterDelete(event);

    verifyNoInteractions(fifoMessagingService);
  }

  @Test
  void shouldNotQueueRelatedPlacementWhenPlacementNotFoundAfterDelete() {
    PlacementSite placementSite = new PlacementSite();
    placementSite.setPlacementId(2L);
    when(cache.get(1L, PlacementSite.class)).thenReturn(placementSite);

    when(placementService.findById("2")).thenReturn(Optional.empty());

    Document document = new Document();
    document.append("_id", 1L);
    AfterDeleteEvent<PlacementSite> event = new AfterDeleteEvent<>(document, null, null);

    listener.onAfterDelete(event);

    verifyNoInteractions(fifoMessagingService);
  }

  @Test
  void shouldQueueRelatedPlacementWhenPlacementFoundAfterDelete() {
    PlacementSite placementSite = new PlacementSite();
    placementSite.setPlacementId(2L);
    when(cache.get(1L, PlacementSite.class)).thenReturn(placementSite);

    Placement placement = new Placement();
    placement.setTisId("2");
    when(placementService.findById("2")).thenReturn(Optional.of(placement));

    Document document = new Document();
    document.append("_id", 1L);
    AfterDeleteEvent<PlacementSite> event = new AfterDeleteEvent<>(document, null, null);

    listener.onAfterDelete(event);

    verify(fifoMessagingService).sendMessageToFifoQueue(PLACEMENT_QUEUE_URL, placement);

    assertThat("Unexpected operation.", placement.getOperation(), is(Operation.LOAD));
  }
}

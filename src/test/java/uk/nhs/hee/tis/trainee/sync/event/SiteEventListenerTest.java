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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.Placement;
import uk.nhs.hee.tis.trainee.sync.model.PlacementSite;
import uk.nhs.hee.tis.trainee.sync.model.Site;
import uk.nhs.hee.tis.trainee.sync.service.FifoMessagingService;
import uk.nhs.hee.tis.trainee.sync.service.PlacementSiteSyncService;
import uk.nhs.hee.tis.trainee.sync.service.PlacementSyncService;

class SiteEventListenerTest {

  private static final String PLACEMENT_QUEUE_URL = "https://queue.placement";

  private static final String SITE_ID = "1";
  private static final String PLACEMENT_ID_1 = "2";
  private static final String PLACEMENT_ID_2 = "3";

  private SiteEventListener listener;
  private PlacementSyncService placementService;
  private PlacementSiteSyncService placementSiteService;
  private FifoMessagingService fifoMessagingService;

  @BeforeEach
  void setUp() {
    placementService = mock(PlacementSyncService.class);
    placementSiteService = mock(PlacementSiteSyncService.class);
    fifoMessagingService = mock(FifoMessagingService.class);
    listener = new SiteEventListener(placementService, placementSiteService, fifoMessagingService,
        PLACEMENT_QUEUE_URL);
  }

  @Test
  void shouldNotInteractWithPlacementQueueAfterSaveWhenNoRelatedPlacements() {
    when(placementSiteService.findOtherSitesBySiteId(Long.parseLong(SITE_ID))).thenReturn(
        Set.of());
    when(placementService.findBySiteId(SITE_ID)).thenReturn(Set.of());

    Site site = new Site();
    site.setTisId(SITE_ID);
    AfterSaveEvent<Site> event = new AfterSaveEvent<>(site, null, null);

    listener.onAfterSave(event);

    verifyNoInteractions(fifoMessagingService);
  }

  @Test
  void shouldSendSiteLinkedPlacementsToQueueWhenFoundAfterSave() {
    when(placementSiteService.findOtherSitesBySiteId(Long.parseLong(SITE_ID))).thenReturn(
        Set.of());

    Placement placement1 = new Placement();
    placement1.setTisId(PLACEMENT_ID_1);
    Placement placement2 = new Placement();
    placement2.setTisId(PLACEMENT_ID_2);
    when(placementService.findBySiteId(SITE_ID)).thenReturn(Set.of(placement1, placement2));

    Site site = new Site();
    site.setTisId(SITE_ID);
    AfterSaveEvent<Site> event = new AfterSaveEvent<>(site, null, null);

    listener.onAfterSave(event);

    verify(fifoMessagingService).sendMessageToFifoQueue(
        eq(PLACEMENT_QUEUE_URL), eq(placement1), any());
    assertThat("Unexpected table operation.", placement1.getOperation(), is(Operation.LOAD));

    verify(fifoMessagingService).sendMessageToFifoQueue(
        eq(PLACEMENT_QUEUE_URL), eq(placement2), any());
    assertThat("Unexpected table operation.", placement2.getOperation(), is(Operation.LOAD));
  }

  @Test
  void shouldRequestMissingPlacementSiteLinkedPlacementsToQueueWhenNotFoundAfterSave() {
    PlacementSite placementSite1 = new PlacementSite();
    placementSite1.setPlacementId(Long.parseLong(PLACEMENT_ID_1));
    PlacementSite placementSite2 = new PlacementSite();
    placementSite2.setPlacementId(Long.parseLong(PLACEMENT_ID_2));
    when(placementSiteService.findOtherSitesBySiteId(Long.parseLong(SITE_ID))).thenReturn(
        Set.of(placementSite1, placementSite2));

    Placement placement1 = new Placement();
    placement1.setTisId(PLACEMENT_ID_1);
    when(placementService.findById(PLACEMENT_ID_1)).thenReturn(Optional.of(placement1));
    when(placementService.findById(PLACEMENT_ID_2)).thenReturn(Optional.empty());

    when(placementService.findBySiteId(SITE_ID)).thenReturn(Set.of());

    Site site = new Site();
    site.setTisId(SITE_ID);
    AfterSaveEvent<Site> event = new AfterSaveEvent<>(site, null, null);

    listener.onAfterSave(event);

    verify(fifoMessagingService).sendMessageToFifoQueue(
        eq(PLACEMENT_QUEUE_URL), eq(placement1), any());
    assertThat("Unexpected table operation.", placement1.getOperation(), is(Operation.LOAD));

    verify(placementService).request(PLACEMENT_ID_2);
  }

  @Test
  void shouldSendPlacementSiteLinkedPlacementsToQueueWhenFoundAfterSave() {
    PlacementSite placementSite1 = new PlacementSite();
    placementSite1.setPlacementId(Long.parseLong(PLACEMENT_ID_1));
    PlacementSite placementSite2 = new PlacementSite();
    placementSite2.setPlacementId(Long.parseLong(PLACEMENT_ID_2));
    when(placementSiteService.findOtherSitesBySiteId(Long.parseLong(SITE_ID))).thenReturn(
        Set.of(placementSite1, placementSite2));

    Placement placement1 = new Placement();
    placement1.setTisId(PLACEMENT_ID_1);
    Placement placement2 = new Placement();
    placement2.setTisId(PLACEMENT_ID_2);
    when(placementService.findById(PLACEMENT_ID_1)).thenReturn(Optional.of(placement1));
    when(placementService.findById(PLACEMENT_ID_2)).thenReturn(Optional.of(placement2));

    when(placementService.findBySiteId(SITE_ID)).thenReturn(Set.of());

    Site site = new Site();
    site.setTisId(SITE_ID);
    AfterSaveEvent<Site> event = new AfterSaveEvent<>(site, null, null);

    listener.onAfterSave(event);

    verify(fifoMessagingService).sendMessageToFifoQueue(PLACEMENT_QUEUE_URL, placement1, any());
    assertThat("Unexpected table operation.", placement1.getOperation(), is(Operation.LOAD));

    verify(fifoMessagingService).sendMessageToFifoQueue(PLACEMENT_QUEUE_URL, placement2, any());
    assertThat("Unexpected table operation.", placement2.getOperation(), is(Operation.LOAD));
  }

  @Test
  void shouldSendAllLinkedPlacementsToQueueWhenFoundAfterSave() {
    PlacementSite placementSite1 = new PlacementSite();
    placementSite1.setPlacementId(Long.parseLong(PLACEMENT_ID_1));
    when(placementSiteService.findOtherSitesBySiteId(Long.parseLong(SITE_ID))).thenReturn(
        Set.of(placementSite1));

    Placement placement1 = new Placement();
    placement1.setTisId(PLACEMENT_ID_1);
    when(placementService.findById(PLACEMENT_ID_1)).thenReturn(Optional.of(placement1));

    Placement placement2 = new Placement();
    placement2.setTisId(PLACEMENT_ID_2);
    when(placementService.findBySiteId(SITE_ID)).thenReturn(Set.of(placement2));

    Site site = new Site();
    site.setTisId(SITE_ID);
    AfterSaveEvent<Site> event = new AfterSaveEvent<>(site, null, null);

    listener.onAfterSave(event);

    verify(fifoMessagingService).sendMessageToFifoQueue(
        eq(PLACEMENT_QUEUE_URL), eq(placement1), any());
    assertThat("Unexpected table operation.", placement1.getOperation(), is(Operation.LOAD));

    verify(fifoMessagingService).sendMessageToFifoQueue(
        eq(PLACEMENT_QUEUE_URL), eq(placement2), any());
    assertThat("Unexpected table operation.", placement2.getOperation(), is(Operation.LOAD));
  }
}

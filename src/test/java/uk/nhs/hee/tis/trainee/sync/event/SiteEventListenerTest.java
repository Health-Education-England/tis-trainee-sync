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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.Placement;
import uk.nhs.hee.tis.trainee.sync.model.Site;
import uk.nhs.hee.tis.trainee.sync.service.PlacementSyncService;

class SiteEventListenerTest {

  private static final String PLACEMENT_QUEUE_URL = "https://queue.placement";

  private SiteEventListener listener;
  private PlacementSyncService placementService;
  private QueueMessagingTemplate messagingTemplate;

  @BeforeEach
  void setUp() {
    placementService = mock(PlacementSyncService.class);
    messagingTemplate = mock(QueueMessagingTemplate.class);
    listener = new SiteEventListener(placementService, messagingTemplate, PLACEMENT_QUEUE_URL);
  }

  @Test
  void shouldNotInteractWithPlacementQueueAfterSaveWhenNoRelatedPlacements() {
    Site site = new Site();
    site.setTisId("s1");
    AfterSaveEvent<Site> event = new AfterSaveEvent<>(site, null, null);

    when(placementService.findBySiteId("s1")).thenReturn(Collections.emptySet());

    listener.onAfterSave(event);

    verifyNoInteractions(messagingTemplate);
  }

  @Test
  void shouldSendRelatedPlacementsToQueueAfterSaveWhenRelatedPlacements() {
    Site site = new Site();
    site.setTisId("s1");

    Placement placement1 = new Placement();
    placement1.setTisId("p1");

    Placement placement2 = new Placement();
    placement2.setTisId("p2");
    when(placementService.findBySiteId("s1")).thenReturn(Set.of(placement1, placement2));

    AfterSaveEvent<Site> event = new AfterSaveEvent<>(site, null, null);
    listener.onAfterSave(event);

    verify(messagingTemplate).convertAndSend(PLACEMENT_QUEUE_URL, placement1);
    assertThat("Unexpected table operation.", placement1.getOperation(), is(Operation.LOAD));

    verify(messagingTemplate).convertAndSend(PLACEMENT_QUEUE_URL, placement2);
    assertThat("Unexpected table operation.", placement2.getOperation(), is(Operation.LOAD));
  }
}

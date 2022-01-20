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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.Placement;
import uk.nhs.hee.tis.trainee.sync.model.PlacementSpecialty;
import uk.nhs.hee.tis.trainee.sync.service.PlacementSyncService;

class PlacementSpecialtyEventListenerTest {

  private static final String PLACEMENT_QUEUE_URL = "https://queue.placement";
  private static final String PLACEMENT_ID = "placement1";

  private PlacementSpecialtyEventListener listener;
  private PlacementSyncService placementService;
  private QueueMessagingTemplate messagingTemplate;

  @BeforeEach
  void setUp() {
    placementService = mock(PlacementSyncService.class);
    messagingTemplate = mock(QueueMessagingTemplate.class);
    listener = new PlacementSpecialtyEventListener(placementService, messagingTemplate,
        PLACEMENT_QUEUE_URL);
  }

  @Test
  void shouldSendRelatedPlacementToQueueAfterSaveWhenPlacementFound() {
    PlacementSpecialty placementSpecialty = new PlacementSpecialty();
    placementSpecialty.setData(Collections.singletonMap("placementId", PLACEMENT_ID));

    Placement placement = new Placement();
    placement.setTisId(PLACEMENT_ID);

    when(placementService.findById(PLACEMENT_ID)).thenReturn(Optional.of(placement));

    AfterSaveEvent<PlacementSpecialty> event = new AfterSaveEvent<>(placementSpecialty, null, null);
    listener.onAfterSave(event);

    verify(messagingTemplate).convertAndSend(PLACEMENT_QUEUE_URL, placement);
    assertThat("Unexpected table operation.", placement.getOperation(), is(Operation.LOAD));
    verify(placementService, never()).request(PLACEMENT_ID);
  }

  @Test
  void shouldRequestRelatedPlacementAfterSaveWhenPlacementNotFound() {
    PlacementSpecialty placementSpecialty = new PlacementSpecialty();
    placementSpecialty.setData(Collections.singletonMap("placementId", PLACEMENT_ID));

    when(placementService.findById(PLACEMENT_ID)).thenReturn(Optional.empty());

    AfterSaveEvent<PlacementSpecialty> event = new AfterSaveEvent<>(placementSpecialty, null, null);
    listener.onAfterSave(event);

    verify(messagingTemplate, never())
        .convertAndSend(eq(PLACEMENT_QUEUE_URL), any(Placement.class));
    verify(placementService).request(PLACEMENT_ID);
  }
}

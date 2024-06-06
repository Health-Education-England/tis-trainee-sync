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

import java.util.Set;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.PlacementSpecialty;
import uk.nhs.hee.tis.trainee.sync.model.PostSpecialty;
import uk.nhs.hee.tis.trainee.sync.model.Specialty;
import uk.nhs.hee.tis.trainee.sync.service.FifoMessagingService;
import uk.nhs.hee.tis.trainee.sync.service.PlacementSpecialtySyncService;
import uk.nhs.hee.tis.trainee.sync.service.PostSpecialtySyncService;

class SpecialtyEventListenerTest {

  private static final String PLACEMENT_SPECIALTY_QUEUE_URL = "https://queue.placement-specialty";
  private static final String POST_SPECIALTY_QUEUE_URL = "https://queue.post-specialty";

  private SpecialtyEventListener listener;
  private PlacementSpecialtySyncService placementSpecialtyService;
  private PostSpecialtySyncService postSpecialtyService;
  private FifoMessagingService fifoMessagingService;

  @BeforeEach
  void setUp() {
    placementSpecialtyService = mock(PlacementSpecialtySyncService.class);
    postSpecialtyService = mock(PostSpecialtySyncService.class);
    fifoMessagingService = mock(FifoMessagingService.class);
    listener = new SpecialtyEventListener(placementSpecialtyService, postSpecialtyService,
        fifoMessagingService, PLACEMENT_SPECIALTY_QUEUE_URL, POST_SPECIALTY_QUEUE_URL);
  }

  @Test
  void shouldNotInteractWithPlacementSpecialtyQueueAfterSaveWhenNoRelatedPlacementSpecialties() {
    Specialty specialty = new Specialty();
    specialty.setTisId("specialty1");
    AfterSaveEvent<Specialty> event = new AfterSaveEvent<>(specialty, null, null);

    when(placementSpecialtyService.findPrimaryAndSubPlacementSpecialtiesBySpecialtyId("specialty1"))
        .thenReturn(Set.of());

    listener.onAfterSave(event);

    verifyNoInteractions(fifoMessagingService);
  }

  @Test
  void shouldSendRelatedPlacementSpecialtiesToQueueAfterSaveWhenRelatedPlacementSpecialties() {
    Specialty specialty = new Specialty();
    specialty.setTisId("specialty1");

    PlacementSpecialty placementSpecialty1 = new PlacementSpecialty();
    placementSpecialty1.setTisId("placementSpecialty1");

    PlacementSpecialty placementSpecialty2 = new PlacementSpecialty();
    placementSpecialty2.setTisId("placementSpecialty2");

    when(placementSpecialtyService.findBySpecialtyId("specialty1"))
        .thenReturn(Set.of(placementSpecialty1, placementSpecialty2));

    AfterSaveEvent<Specialty> event = new AfterSaveEvent<>(specialty, null, null);
    listener.onAfterSave(event);

    verify(fifoMessagingService).sendMessageToFifoQueue(
        eq(PLACEMENT_SPECIALTY_QUEUE_URL), eq(placementSpecialty1), any());
    assertThat("Unexpected table operation.", placementSpecialty1.getOperation(),
        is(Operation.LOAD));

    verify(fifoMessagingService).sendMessageToFifoQueue(
        eq(PLACEMENT_SPECIALTY_QUEUE_URL), eq(placementSpecialty2), any()));
    assertThat("Unexpected table operation.", placementSpecialty2.getOperation(),
        is(Operation.LOAD));
  }

  @Test
  void shouldNotInteractWithPlacementSpecialtyQueueAfterDeleteWhenNoRelatedPlacementSpecialties() {
    Document document = new Document();
    document.append("_id", "specialty1");
    AfterDeleteEvent<Specialty> event = new AfterDeleteEvent<>(document, Specialty.class,
        "specialty");

    when(placementSpecialtyService.findPrimaryAndSubPlacementSpecialtiesBySpecialtyId("specialty1"))
        .thenReturn(Set.of());

    listener.onAfterDelete(event);

    verifyNoInteractions(fifoMessagingService);
  }

  @Test
  void shouldSendRelatedPlacementSpecialtiesToQueueAfterDeleteWhenRelatedPlacementSpecialties() {
    Document document = new Document();
    document.append("_id", "specialty1");

    PlacementSpecialty placementSpecialty1 = new PlacementSpecialty();
    placementSpecialty1.setTisId("placementSpecialty1");

    PlacementSpecialty placementSpecialty2 = new PlacementSpecialty();
    placementSpecialty2.setTisId("placementSpecialty2");

    when(placementSpecialtyService.findBySpecialtyId("specialty1"))
        .thenReturn(Set.of(placementSpecialty1, placementSpecialty2));

    AfterDeleteEvent<Specialty> event = new AfterDeleteEvent<>(document, Specialty.class,
        "specialty");
    listener.onAfterDelete(event);

    verify(fifoMessagingService).sendMessageToFifoQueue(
        eq(PLACEMENT_SPECIALTY_QUEUE_URL), eq(placementSpecialty1), any());
    assertThat("Unexpected table operation.", placementSpecialty1.getOperation(),
        is(Operation.DELETE));

    verify(fifoMessagingService).sendMessageToFifoQueue(
        eq(PLACEMENT_SPECIALTY_QUEUE_URL), eq(placementSpecialty2), any());
    assertThat("Unexpected table operation.", placementSpecialty2.getOperation(),
        is(Operation.DELETE));
  }

  @Test
  void shouldNotInteractWithPostSpecialtyQueueAfterSaveWhenNoRelatedPostSpecialties() {
    Specialty specialty = new Specialty();
    specialty.setTisId("specialty1");
    AfterSaveEvent<Specialty> event = new AfterSaveEvent<>(specialty, null, null);

    when(postSpecialtyService.findBySpecialtyId("specialty1"))
        .thenReturn(Set.of());

    listener.onAfterSave(event);

    verifyNoInteractions(fifoMessagingService);
  }

  @Test
  void shouldSendRelatedPostSpecialtiesToQueueAfterSave() {
    Specialty specialty = new Specialty();
    specialty.setTisId("specialty1");

    PostSpecialty postSpecialty1 = new PostSpecialty();
    postSpecialty1.setTisId("postSpecialty1");

    PostSpecialty postSpecialty2 = new PostSpecialty();
    postSpecialty2.setTisId("postSpecialty2");

    when(postSpecialtyService.findBySpecialtyId("specialty1"))
        .thenReturn(Set.of(postSpecialty1, postSpecialty2));

    AfterSaveEvent<Specialty> event = new AfterSaveEvent<>(specialty, null, null);
    listener.onAfterSave(event);

    verify(fifoMessagingService).sendMessageToFifoQueue(
        eq(POST_SPECIALTY_QUEUE_URL), eq(postSpecialty1), any());
    assertThat("Unexpected table operation.", postSpecialty1.getOperation(),
        is(Operation.LOAD));

    verify(fifoMessagingService).sendMessageToFifoQueue(
        eq(POST_SPECIALTY_QUEUE_URL), eq(postSpecialty2), any());
    assertThat("Unexpected table operation.", postSpecialty2.getOperation(),
        is(Operation.LOAD));
  }

  @Test
  void shouldNotInteractWithPostSpecialtyQueueAfterDeleteWhenNoRelatedPostSpecialties() {
    Document document = new Document();
    document.append("_id", "specialty1");
    AfterDeleteEvent<Specialty> event = new AfterDeleteEvent<>(document, Specialty.class,
        "specialty");

    when(postSpecialtyService.findBySpecialtyId("specialty1")).thenReturn(Set.of());

    listener.onAfterDelete(event);

    verifyNoInteractions(fifoMessagingService);
  }

  @Test
  void shouldSendRelatedPostSpecialtiesToQueueAfterDeleteWhenRelatedPostSpecialties() {
    Document document = new Document();
    document.append("_id", "specialty1");

    PostSpecialty postSpecialty1 = new PostSpecialty();
    postSpecialty1.setTisId("postSpecialty1");

    PostSpecialty postSpecialty2 = new PostSpecialty();
    postSpecialty2.setTisId("postSpecialty2");

    when(postSpecialtyService.findBySpecialtyId("specialty1"))
        .thenReturn(Set.of(postSpecialty1, postSpecialty2));

    AfterDeleteEvent<Specialty> event = new AfterDeleteEvent<>(document, Specialty.class,
        "specialty");
    listener.onAfterDelete(event);

    verify(fifoMessagingService).sendMessageToFifoQueue(
        eq(POST_SPECIALTY_QUEUE_URL), eq(postSpecialty1), any());
    assertThat("Unexpected table operation.", postSpecialty1.getOperation(),
        is(Operation.DELETE));

    verify(fifoMessagingService).sendMessageToFifoQueue(
        eq(POST_SPECIALTY_QUEUE_URL), eq(postSpecialty2), any());
    assertThat("Unexpected table operation.", postSpecialty2.getOperation(),
        is(Operation.DELETE));
  }
}

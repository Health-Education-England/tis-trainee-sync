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
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.PlacementSpecialty;
import uk.nhs.hee.tis.trainee.sync.model.Specialty;
import uk.nhs.hee.tis.trainee.sync.service.PlacementSpecialtySyncService;

class SpecialtyEventListenerTest {

  private static final String PLACEMENT_SPECIALTY_QUEUE_URL = "https://queue.placement-specialty";

  private SpecialtyEventListener listener;
  private PlacementSpecialtySyncService placementSpecialtyService;
  private QueueMessagingTemplate messagingTemplate;

  @BeforeEach
  void setUp() {
    placementSpecialtyService = mock(PlacementSpecialtySyncService.class);
    messagingTemplate = mock(QueueMessagingTemplate.class);
    listener = new SpecialtyEventListener(placementSpecialtyService, messagingTemplate,
        PLACEMENT_SPECIALTY_QUEUE_URL);
  }

  @Test
  void shouldNotInteractWithPlacementSpecialtyQueueAfterSaveWhenNoRelatedPlacementSpecialties() {
    Specialty specialty = new Specialty();
    specialty.setTisId("specialty1");
    AfterSaveEvent<Specialty> event = new AfterSaveEvent<>(specialty, null, null);

    when(placementSpecialtyService.findPrimaryPlacementSpecialtiesBySpecialtyId("specialty1"))
        .thenReturn(Collections.emptySet());

    listener.onAfterSave(event);

    verifyNoInteractions(messagingTemplate);
  }

  @Test
  void shouldSendRelatedPlacementSpecialtiesToQueueAfterSaveWhenRelatedPlacementSpecialties() {
    Specialty specialty = new Specialty();
    specialty.setTisId("specialty1");

    PlacementSpecialty placementSpecialty1 = new PlacementSpecialty();
    placementSpecialty1.setTisId("placementSpecialty1");

    PlacementSpecialty placementSpecialty2 = new PlacementSpecialty();
    placementSpecialty2.setTisId("placementSpecialty2");

    when(placementSpecialtyService.findPrimaryPlacementSpecialtiesBySpecialtyId("specialty1"))
        .thenReturn(Set.of(placementSpecialty1, placementSpecialty2));

    AfterSaveEvent<Specialty> event = new AfterSaveEvent<>(specialty, null, null);
    listener.onAfterSave(event);

    verify(messagingTemplate).convertAndSend(PLACEMENT_SPECIALTY_QUEUE_URL, placementSpecialty1);
    assertThat("Unexpected table operation.", placementSpecialty1.getOperation(),
        is(Operation.LOAD));

    verify(messagingTemplate).convertAndSend(PLACEMENT_SPECIALTY_QUEUE_URL, placementSpecialty2);
    assertThat("Unexpected table operation.", placementSpecialty2.getOperation(),
        is(Operation.LOAD));
  }

  @Test
  void shouldNotInteractWithPlacementSpecialtyQueueAfterDeleteWhenNoRelatedPlacementSpecialties() {
    Document document = new Document();
    document.append("_id", "specialty1");
    AfterDeleteEvent<Specialty> event = new AfterDeleteEvent<>(document, Specialty.class,
        "specialty");

    when(placementSpecialtyService.findPrimaryPlacementSpecialtiesBySpecialtyId("specialty1"))
        .thenReturn(Collections.emptySet());

    listener.onAfterDelete(event);

    verifyNoInteractions(messagingTemplate);
  }

  @Test
  void shouldSendRelatedPlacementSpecialtiesToQueueAfterDeleteWhenRelatedPlacementSpecialties() {
    Document document = new Document();
    document.append("_id", "specialty1");

    PlacementSpecialty placementSpecialty1 = new PlacementSpecialty();
    placementSpecialty1.setTisId("placementSpecialty1");

    PlacementSpecialty placementSpecialty2 = new PlacementSpecialty();
    placementSpecialty2.setTisId("placementSpecialty2");

    when(placementSpecialtyService.findPrimaryPlacementSpecialtiesBySpecialtyId("specialty1"))
        .thenReturn(Set.of(placementSpecialty1, placementSpecialty2));

    AfterDeleteEvent<Specialty> event = new AfterDeleteEvent<>(document, Specialty.class,
        "specialty");
    listener.onAfterDelete(event);

    verify(messagingTemplate).convertAndSend(PLACEMENT_SPECIALTY_QUEUE_URL, placementSpecialty1);
    assertThat("Unexpected table operation.", placementSpecialty1.getOperation(),
        is(Operation.DELETE));

    verify(messagingTemplate).convertAndSend(PLACEMENT_SPECIALTY_QUEUE_URL, placementSpecialty2);
    assertThat("Unexpected table operation.", placementSpecialty2.getOperation(),
        is(Operation.DELETE));
  }
}

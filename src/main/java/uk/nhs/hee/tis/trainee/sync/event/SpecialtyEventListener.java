package uk.nhs.hee.tis.trainee.sync.event;

import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.PlacementSpecialty;
import uk.nhs.hee.tis.trainee.sync.model.Specialty;
import uk.nhs.hee.tis.trainee.sync.service.PlacementSpecialtySyncService;

@Component
public class SpecialtyEventListener extends AbstractMongoEventListener<Specialty> {

  private final PlacementSpecialtySyncService placementSpecialtyService;

  private final QueueMessagingTemplate messagingTemplate;

  private final String placementSpecialtyQueueUrl;

  SpecialtyEventListener(PlacementSpecialtySyncService placementSpecialtyService,
      QueueMessagingTemplate messagingTemplate,
      @Value("${application.aws.sqs.placement-specialty}") String placementSpecialtyQueueUrl
  ) {
    this.placementSpecialtyService = placementSpecialtyService;
    this.messagingTemplate = messagingTemplate;
    this.placementSpecialtyQueueUrl = placementSpecialtyQueueUrl;
  }

  @Override
  public void onAfterSave(AfterSaveEvent<Specialty> event) {
    super.onAfterSave(event);

    Specialty specialty = event.getSource();
    sendPlacementSpecialtyMessages(specialty.getTisId(), Operation.LOAD);
  }

  @Override
  public void onAfterDelete(AfterDeleteEvent<Specialty> event) {
    super.onAfterDelete(event);

    String specialtyId = event.getSource().getString("_id");
    sendPlacementSpecialtyMessages(specialtyId, Operation.DELETE);
  }

  /**
   * Send messages for all associated placement specialties.
   *
   * @param specialtyId The ID of the specialty to get associated placement specialties for.
   * @param operation   The operation to set on the message, e.g. DELETE.
   */
  private void sendPlacementSpecialtyMessages(String specialtyId, Operation operation) {
    Set<PlacementSpecialty> placementSpecialties = placementSpecialtyService
        .findPrimaryPlacementSpecialtiesBySpecialtyId(specialtyId);

    for (PlacementSpecialty placementSpecialty : placementSpecialties) {
      // Default each placement specialty's operation.
      placementSpecialty.setOperation(operation);
      messagingTemplate.convertAndSend(placementSpecialtyQueueUrl, placementSpecialty);
    }
  }
}

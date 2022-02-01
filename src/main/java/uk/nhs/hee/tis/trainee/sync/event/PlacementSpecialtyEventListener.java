package uk.nhs.hee.tis.trainee.sync.event;

import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.Placement;
import uk.nhs.hee.tis.trainee.sync.model.PlacementSpecialty;
import uk.nhs.hee.tis.trainee.sync.service.PlacementSyncService;

@Slf4j
@Component
public class PlacementSpecialtyEventListener extends
    AbstractMongoEventListener<PlacementSpecialty> {

  private final PlacementSyncService placementService;

  private final QueueMessagingTemplate messagingTemplate;

  private final String placementQueueUrl;

  PlacementSpecialtyEventListener(PlacementSyncService placementService,
      QueueMessagingTemplate messagingTemplate,
      @Value("${application.aws.sqs.placement}") String placementQueueUrl) {
    this.placementService = placementService;
    this.messagingTemplate = messagingTemplate;
    this.placementQueueUrl = placementQueueUrl;
  }

  @Override
  public void onAfterSave(AfterSaveEvent<PlacementSpecialty> event) {
    super.onAfterSave(event);

    PlacementSpecialty placementSpecialty = event.getSource();
    String placementId = placementSpecialty.getData().get("placementId");

    if (placementId != null) {
      Optional<Placement> optionalPlacement = placementService.findById(placementId);

      if (optionalPlacement.isPresent()) {
        // Default the placement to LOAD.
        Placement placement = optionalPlacement.get();
        placement.setOperation(Operation.LOAD);
        messagingTemplate.convertAndSend(placementQueueUrl, placement);
      }
    }
  }
}


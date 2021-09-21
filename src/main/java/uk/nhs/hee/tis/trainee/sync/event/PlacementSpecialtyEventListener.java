package uk.nhs.hee.tis.trainee.sync.event;

import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.trainee.sync.facade.PlacementEnricherFacade;
import uk.nhs.hee.tis.trainee.sync.model.Placement;
import uk.nhs.hee.tis.trainee.sync.model.PlacementSpecialty;
import uk.nhs.hee.tis.trainee.sync.service.PlacementSyncService;

@Slf4j
@Component
public class PlacementSpecialtyEventListener extends
    AbstractMongoEventListener<PlacementSpecialty> {

  private final PlacementSyncService placementService;

  private final PlacementEnricherFacade placementEnricher;

  private final QueueMessagingTemplate messagingTemplate;

  private final String placementQueueUrl;

  PlacementSpecialtyEventListener(PlacementEnricherFacade placementEnricher,
      PlacementSyncService placementService,
      QueueMessagingTemplate messagingTemplate,
      @Value("${application.aws.sqs.placement}") String placementQueueUrl) {
    this.placementEnricher = placementEnricher;
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
      Optional<Placement> placement = placementService.findById(placementId);

      if (placement.isPresent()) {
        messagingTemplate.convertAndSend(placementQueueUrl, placement.get());
      } else {
        placementService.request(placementId);
      }
    }
  }

  @Override
  public void onAfterDelete(AfterDeleteEvent<PlacementSpecialty> event) {
    // make sure that:
    //  1) the Placement was also deleted (OR)
    //  2) avoid making the old 'delete' PS erase the PS if been overwritten by new one already

    String placementId = (String) Objects.requireNonNull(event.getDocument()).get("_id");
    placementEnricher.restartPlacementEnrichmentIfDeletionIncorrect(placementId);

    super.onAfterDelete(event);
  }
}


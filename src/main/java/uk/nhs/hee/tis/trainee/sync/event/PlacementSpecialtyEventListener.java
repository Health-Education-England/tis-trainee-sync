package uk.nhs.hee.tis.trainee.sync.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeDeleteEvent;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.trainee.sync.facade.PlacementEnricherFacade;
import uk.nhs.hee.tis.trainee.sync.model.Placement;
import uk.nhs.hee.tis.trainee.sync.model.PlacementSpecialty;
import uk.nhs.hee.tis.trainee.sync.service.PlacementSyncService;

@Slf4j
@Component
public class PlacementSpecialtyEventListener extends
    AbstractMongoEventListener<PlacementSpecialty> {

  private final PlacementEnricherFacade placementEnricher;

  PlacementSpecialtyEventListener(PlacementEnricherFacade placementEnricher) {
    this.placementEnricher = placementEnricher;
  }

  @Override
  public void onAfterSave(AfterSaveEvent<PlacementSpecialty> event) {
    super.onAfterSave(event);

    PlacementSpecialty placementSpecialty = event.getSource();
    placementEnricher.enrich(placementSpecialty);
  }

  @Override
  public void onAfterDelete(AfterDeleteEvent<PlacementSpecialty> event) {
    // TODO: make sure that:
    //  1) the Placement was also deleted (OR)
    //  2) the Placement has a new placementSpecialty

    String placementId = (String) event.getDocument().get("_id");
    boolean placementSpecialtyDeletedCorrectly = placementEnricher.placementSpecialtyDeletedCorrectly(placementId);
    if (!placementSpecialtyDeletedCorrectly) {
      log.warn("placementSpecialty with placementId {} deleted incorrectly", placementId);
    }

    super.onAfterDelete(event);
  }
}


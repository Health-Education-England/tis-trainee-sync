package uk.nhs.hee.tis.trainee.sync.event;

import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.trainee.sync.facade.PlacementEnricherFacade;
import uk.nhs.hee.tis.trainee.sync.model.PlacementSpecialty;

@Component
public class PlacementSpecialtyEventListener extends AbstractMongoEventListener<PlacementSpecialty> {

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
    // TODO: Implement.
    super.onAfterDelete(event);
  }
}


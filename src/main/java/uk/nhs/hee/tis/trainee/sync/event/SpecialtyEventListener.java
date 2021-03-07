package uk.nhs.hee.tis.trainee.sync.event;

import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.trainee.sync.facade.PlacementEnricherFacade;
import uk.nhs.hee.tis.trainee.sync.model.Specialty;

@Component
public class SpecialtyEventListener extends AbstractMongoEventListener<Specialty> {

  private final PlacementEnricherFacade placementEnricher;

  SpecialtyEventListener(PlacementEnricherFacade placementEnricher) {
    this.placementEnricher = placementEnricher;
  }

  @Override
  public void onAfterSave(AfterSaveEvent<Specialty> event) {
    super.onAfterSave(event);

    Specialty specialty = event.getSource();
    placementEnricher.enrich(specialty);
  }

  @Override
  public void onAfterDelete(AfterDeleteEvent<Specialty> event) {
    // TODO: Implement.
    super.onAfterDelete(event);
  }
}

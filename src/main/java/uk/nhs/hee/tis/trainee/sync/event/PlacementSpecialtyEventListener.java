package uk.nhs.hee.tis.trainee.sync.event;

import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.trainee.sync.facade.PlacementEnricherFacade;
import uk.nhs.hee.tis.trainee.sync.model.PlacementSpecialty;

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
    // make sure that:
    //  1) the Placement was also deleted (OR)
    //  2) avoid making the old 'delete' PS erase the PS if been overwritten by new one already

    String placementId = (String) Objects.requireNonNull(event.getDocument()).get("_id");
    placementEnricher.placementSpecialtyDeletedCorrectly(placementId);

    super.onAfterDelete(event);
  }
}


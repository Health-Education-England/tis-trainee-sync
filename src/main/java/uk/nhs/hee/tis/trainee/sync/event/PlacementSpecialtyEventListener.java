package uk.nhs.hee.tis.trainee.sync.event;

import java.util.Optional;
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
    // TODO: make sure that:
    //  1) the Placement was also deleted (OR)
    //  2) the Placement has a new placementSpecialty

    String placementId = (String) event.getDocument().get("_id");
    boolean placementSpecialtyDeletedCorrectly = placementEnricher
        .placementSpecialtyDeletedCorrectly(placementId);

    if (!placementSpecialtyDeletedCorrectly) {
      log.warn(
          "PlacementSpecialty with placementId {} got deleted but its placement is still existing "
              + "without an associated placementSpecialty. Enrichment of Placement has been "
              + "restarted.",
          placementId);
    }

    super.onAfterDelete(event);
  }
}


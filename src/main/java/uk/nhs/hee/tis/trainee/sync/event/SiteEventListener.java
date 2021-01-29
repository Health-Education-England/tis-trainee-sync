package uk.nhs.hee.tis.trainee.sync.event;

import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.trainee.sync.facade.PlacementEnricherFacade;
import uk.nhs.hee.tis.trainee.sync.model.Site;

@Component
public class SiteEventListener extends AbstractMongoEventListener<Site> {

  private final PlacementEnricherFacade placementEnricher;

  SiteEventListener(PlacementEnricherFacade placementEnricher) {
    this.placementEnricher = placementEnricher;
  }

  @Override
  public void onAfterSave(AfterSaveEvent<Site> event) {
    super.onAfterSave(event);

    Site site = event.getSource();
    placementEnricher.enrich(site);
  }

  @Override
  public void onAfterDelete(AfterDeleteEvent<Site> event) {
    // TODO: Implement.
    super.onAfterDelete(event);
  }
}

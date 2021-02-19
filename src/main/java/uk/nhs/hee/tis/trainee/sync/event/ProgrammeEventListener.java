package uk.nhs.hee.tis.trainee.sync.event;

import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.trainee.sync.facade.ProgrammeMembershipEnricherFacade;
import uk.nhs.hee.tis.trainee.sync.model.Programme;

@Component
public class ProgrammeEventListener extends AbstractMongoEventListener<Programme> {

  private final ProgrammeMembershipEnricherFacade programmeMembershipEnricher;

  ProgrammeEventListener(ProgrammeMembershipEnricherFacade programmeMembershipEnricher) {
    this.programmeMembershipEnricher = programmeMembershipEnricher;
  }

  @Override
  public void onAfterSave(AfterSaveEvent<Programme> event) {
    super.onAfterSave(event);

    Programme programme = event.getSource();
    programmeMembershipEnricher.enrich(programme);
  }

  @Override
  public void onAfterDelete(AfterDeleteEvent<Programme> event) {
    // TODO: Implement.
    super.onAfterDelete(event);
  }
}

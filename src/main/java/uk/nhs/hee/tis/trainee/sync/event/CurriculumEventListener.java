package uk.nhs.hee.tis.trainee.sync.event;

import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.trainee.sync.facade.ProgrammeMembershipEnricherFacade;
import uk.nhs.hee.tis.trainee.sync.model.Curriculum;

@Component
public class CurriculumEventListener extends AbstractMongoEventListener<Curriculum> {

  private final ProgrammeMembershipEnricherFacade programmeMembershipEnricher;

  CurriculumEventListener(ProgrammeMembershipEnricherFacade programmeMembershipEnricher) {
    this.programmeMembershipEnricher = programmeMembershipEnricher;
  }

  @Override
  public void onAfterSave(AfterSaveEvent<Curriculum> event) {
    super.onAfterSave(event);

    Curriculum curriculum = event.getSource();
    programmeMembershipEnricher.enrich(curriculum);
  }

  @Override
  public void onAfterDelete(AfterDeleteEvent<Curriculum> event) {
    // TODO: Implement.
    super.onAfterDelete(event);
  }
}

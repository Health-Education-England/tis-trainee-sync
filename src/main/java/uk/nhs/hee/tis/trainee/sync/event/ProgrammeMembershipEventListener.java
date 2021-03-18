/*
 * The MIT License (MIT)
 *
 * Copyright 2021 Crown Copyright (Health Education England)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package uk.nhs.hee.tis.trainee.sync.event;

import java.util.Optional;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeDeleteEvent;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.trainee.sync.facade.ProgrammeMembershipEnricherFacade;
import uk.nhs.hee.tis.trainee.sync.model.ProgrammeMembership;
import uk.nhs.hee.tis.trainee.sync.service.ProgrammeMembershipSyncService;

@Component
public class ProgrammeMembershipEventListener
    extends AbstractMongoEventListener<ProgrammeMembership> {

  private final ProgrammeMembershipEnricherFacade programmeMembershipEnricher;
  private Optional<ProgrammeMembership> optionalDeletedProgrammeMembership;

  @Autowired
  ProgrammeMembershipSyncService programmeMembershipSyncService;

  ProgrammeMembershipEventListener(ProgrammeMembershipEnricherFacade programmeMembershipEnricher) {
    this.programmeMembershipEnricher = programmeMembershipEnricher;
    optionalDeletedProgrammeMembership = Optional.empty();
  }

  @Override
  public void onAfterSave(AfterSaveEvent<ProgrammeMembership> event) {
    super.onAfterSave(event);

    ProgrammeMembership programmeMembership = event.getSource();
    programmeMembershipEnricher.enrich(programmeMembership);
  }

  @Override
  public void onBeforeDelete(BeforeDeleteEvent<ProgrammeMembership> event) {
    Document document = event.getSource();
    optionalDeletedProgrammeMembership = programmeMembershipSyncService
        .findById(document.getString("_id"));
  }

  @Override
  public void onAfterDelete(AfterDeleteEvent<ProgrammeMembership> event) {
    super.onAfterDelete(event);

    optionalDeletedProgrammeMembership.ifPresent(programmeMembershipEnricher::delete);
  }
}

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

import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.trainee.sync.facade.ProgrammeMembershipEnricherFacade;
import uk.nhs.hee.tis.trainee.sync.model.Curriculum;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.ProgrammeMembership;
import uk.nhs.hee.tis.trainee.sync.service.ProgrammeMembershipSyncService;

@Component
public class CurriculumEventListener extends AbstractMongoEventListener<Curriculum> {

  private final ProgrammeMembershipSyncService programmeMembershipService;

  private final QueueMessagingTemplate messagingTemplate;

  private final String programmeMembershipQueueUrl;

  private final ProgrammeMembershipEnricherFacade programmeMembershipEnricher;
  private final Cache cache;

  CurriculumEventListener(ProgrammeMembershipSyncService programmeMembershipService,
                          QueueMessagingTemplate messagingTemplate,
                          ProgrammeMembershipEnricherFacade programmeMembershipEnricher,
                          CacheManager cacheManager,
                          @Value("${application.aws.sqs.programme-membership}")
                              String programmeMembershipQueueUrl) {
    this.programmeMembershipService = programmeMembershipService;
    this.messagingTemplate = messagingTemplate;
    this.programmeMembershipQueueUrl = programmeMembershipQueueUrl;
    this.programmeMembershipEnricher = programmeMembershipEnricher;
    cache = cacheManager.getCache(Curriculum.ENTITY_NAME);
  }

  @Override
  public void onAfterSave(AfterSaveEvent<Curriculum> event) {
    super.onAfterSave(event);

    Curriculum curriculum = event.getSource();
    cache.put(curriculum.getTisId(), curriculum);
    programmeMembershipEnricher.enrich(curriculum);
  }

  @Override
  public void onAfterDelete(AfterDeleteEvent<Curriculum> event) {
    super.onAfterDelete(event);

    String curriculumId = event.getSource().getString("_id");
    sendProgrammeMembershipMessages(curriculumId, Operation.DELETE);
  }

  /**
   * Send messages for all associated programme memberships.
   *
   * @param curriculumId    The ID of the curriculum to get associated programme memberships for.
   * @param operation       The operation to set on the message, e.g. DELETE.
   */
  private void sendProgrammeMembershipMessages(String curriculumId, Operation operation) {
    Set<ProgrammeMembership> programmeMemberships =
        programmeMembershipService.findByCurriculumId(curriculumId);

    for (ProgrammeMembership programmeMembership : programmeMemberships) {
      programmeMembership.setOperation(operation);
      messagingTemplate.convertAndSend(programmeMembershipQueueUrl, programmeMembership);
    }
  }
}

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
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.trainee.sync.model.CurriculumMembership;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.Programme;
import uk.nhs.hee.tis.trainee.sync.service.CurriculumMembershipSyncService;

@Component
public class ProgrammeEventListener extends AbstractMongoEventListener<Programme> {

  private final CurriculumMembershipSyncService curriculumMembershipSyncService;

  private final QueueMessagingTemplate messagingTemplate;

  private final String curriculumMembershipQueueUrl;

  ProgrammeEventListener(CurriculumMembershipSyncService curriculumMembershipSyncService,
      QueueMessagingTemplate messagingTemplate,
      @Value("${application.aws.sqs.curriculum-membership}") String curriculumMembershipQueueUrl) {
    this.curriculumMembershipSyncService = curriculumMembershipSyncService;
    this.messagingTemplate = messagingTemplate;
    this.curriculumMembershipQueueUrl = curriculumMembershipQueueUrl;
  }

  @Override
  public void onAfterSave(AfterSaveEvent<Programme> event) {
    super.onAfterSave(event);

    Programme programme = event.getSource();
    Set<CurriculumMembership> curriculumMemberships =
        curriculumMembershipSyncService.findByProgrammeId(programme.getTisId());

    for (CurriculumMembership curriculumMembership : curriculumMemberships) {
      // Default each message to LOAD.
      curriculumMembership.setOperation(Operation.LOAD);
      messagingTemplate.convertAndSend(curriculumMembershipQueueUrl, curriculumMembership);
    }
  }
}

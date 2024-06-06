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

import java.time.Instant;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.trainee.sync.model.Curriculum;
import uk.nhs.hee.tis.trainee.sync.model.CurriculumMembership;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.service.CurriculumMembershipSyncService;
import uk.nhs.hee.tis.trainee.sync.service.FifoMessagingService;

@Component
public class CurriculumEventListener extends AbstractMongoEventListener<Curriculum> {

  private final CurriculumMembershipSyncService curriculumMembershipService;

  private final FifoMessagingService fifoMessagingService;

  private final String curriculumMembershipQueueUrl;

  private final Cache cache;

  CurriculumEventListener(CurriculumMembershipSyncService curriculumMembershipService,
      FifoMessagingService fifoMessagingService,
      @Value("${application.aws.sqs.curriculum-membership}") String curriculumMembershipQueueUrl,
      CacheManager cacheManager) {
    this.curriculumMembershipService = curriculumMembershipService;
    this.fifoMessagingService = fifoMessagingService;
    this.curriculumMembershipQueueUrl = curriculumMembershipQueueUrl;
    cache = cacheManager.getCache(Curriculum.ENTITY_NAME);
  }

  @Override
  public void onAfterSave(AfterSaveEvent<Curriculum> event) {
    super.onAfterSave(event);

    Curriculum curriculum = event.getSource();
    cache.put(curriculum.getTisId(), curriculum);

    Set<CurriculumMembership> curriculumMemberships =
        curriculumMembershipService.findByCurriculumId(curriculum.getTisId());

    for (CurriculumMembership curriculumMembership : curriculumMemberships) {
      // Default each message to LOAD.
      curriculumMembership.setOperation(Operation.LOAD);
      String deduplicationId = fifoMessagingService
          .getUniqueDeduplicationId("CurriculumMembership", curriculumMembership.getTisId());
      fifoMessagingService.sendMessageToFifoQueue(curriculumMembershipQueueUrl,
          curriculumMembership, deduplicationId);
    }
  }
}

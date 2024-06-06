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
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeDeleteEvent;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.trainee.sync.mapper.ProgrammeMembershipMapper;
import uk.nhs.hee.tis.trainee.sync.model.CurriculumMembership;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.ProgrammeMembership;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.service.CurriculumMembershipSyncService;
import uk.nhs.hee.tis.trainee.sync.service.FifoMessagingService;
import uk.nhs.hee.tis.trainee.sync.service.ProgrammeMembershipSyncService;

@Component
public class CurriculumMembershipEventListener
    extends AbstractMongoEventListener<CurriculumMembership> {

  private static final String PROGRAMME_MEMBERSHIP_UUID = "programmeMembershipUuid";

  private final CurriculumMembershipSyncService curriculumMembershipSyncService;

  private final ProgrammeMembershipSyncService programmeMembershipSyncService;

  private final FifoMessagingService fifoMessagingService;

  private final ProgrammeMembershipMapper programmeMembershipMapper;

  private final Cache curriculumMembershipCache;

  private final String programmeMembershipQueueUrl;

  CurriculumMembershipEventListener(CurriculumMembershipSyncService curriculumMembershipSyncService,
      ProgrammeMembershipSyncService programmeMembershipSyncService,
      ProgrammeMembershipMapper programmeMembershipMapper, CacheManager cacheManager,
      FifoMessagingService fifoMessagingService,
      @Value("${application.aws.sqs.programme-membership}") String programmeMembershipQueueUrl) {
    this.programmeMembershipSyncService = programmeMembershipSyncService;
    this.programmeMembershipMapper = programmeMembershipMapper;
    this.programmeMembershipQueueUrl = programmeMembershipQueueUrl;
    this.curriculumMembershipSyncService = curriculumMembershipSyncService;
    this.fifoMessagingService = fifoMessagingService;
    curriculumMembershipCache = cacheManager.getCache(CurriculumMembership.ENTITY_NAME);
  }

  @Override
  public void onAfterSave(AfterSaveEvent<CurriculumMembership> event) {
    super.onAfterSave(event);

    CurriculumMembership curriculumMembership = event.getSource();
    queueRelatedProgrammeMembership(curriculumMembership, true);
  }

  /**
   * Before deleting a curriculum membership, ensure it is cached.
   *
   * @param event The before-delete event for the curriculum membership.
   */
  @Override
  public void onBeforeDelete(BeforeDeleteEvent<CurriculumMembership> event) {
    String id = event.getSource().getString("_id");
    CurriculumMembership curriculumMembership =
        curriculumMembershipCache.get(id, CurriculumMembership.class);
    if (curriculumMembership == null) {
      Optional<CurriculumMembership> newCurriculumMembership =
          curriculumMembershipSyncService.findById(id);
      newCurriculumMembership.ifPresent(membership ->
          curriculumMembershipCache.put(id, membership));
    }
  }

  /**
   * After delete retrieve cached values and re-sync related Programme Memberships.
   *
   * @param event The after-delete event for the curriculum membership.
   */
  @Override
  public void onAfterDelete(AfterDeleteEvent<CurriculumMembership> event) {
    super.onAfterDelete(event);
    CurriculumMembership curriculumMembership =
        curriculumMembershipCache.get(event.getSource().getString("_id"),
            CurriculumMembership.class);
    if (curriculumMembership != null) {
      queueRelatedProgrammeMembership(curriculumMembership, false);
    }
  }

  /**
   * Queue the programme membership related to the given curriculum membership.
   *
   * @param curriculumMembership The curriculum membership to get related programme memberships
   *                             for.
   * @param requestIfMissing     Whether missing programme memberships should be requested.
   */
  private void queueRelatedProgrammeMembership(CurriculumMembership curriculumMembership,
      boolean requestIfMissing) {
    String programmeMembershipUuid = curriculumMembership.getData().get(PROGRAMME_MEMBERSHIP_UUID);
    Optional<ProgrammeMembership> programmeMembership = programmeMembershipSyncService.findById(
        programmeMembershipUuid);

    if (programmeMembership.isPresent()) {
      Record programmeMembershipRecord = programmeMembershipMapper.toRecord(
          programmeMembership.get());
      // Default the message to LOOKUP.
      programmeMembershipRecord.setOperation(Operation.LOOKUP);
      String deduplicationId = fifoMessagingService.getUniqueDeduplicationId(
          "ProgrammeMembership", String.valueOf(programmeMembership.get().getUuid()));
      fifoMessagingService.sendMessageToFifoQueue(programmeMembershipQueueUrl,
          programmeMembershipRecord, deduplicationId);
    } else if (requestIfMissing) {
      // Request the missing Programme Membership record.
      programmeMembershipSyncService.request(UUID.fromString(programmeMembershipUuid));
    }
  }
}

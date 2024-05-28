/*
 * The MIT License (MIT)
 *
 * Copyright 2023 Crown Copyright (Health Education England)
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
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeDeleteEvent;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.trainee.sync.mapper.ProgrammeMembershipMapper;
import uk.nhs.hee.tis.trainee.sync.model.ConditionsOfJoining;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.ProgrammeMembership;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.service.ConditionsOfJoiningSyncService;
import uk.nhs.hee.tis.trainee.sync.service.FifoMessagingService;
import uk.nhs.hee.tis.trainee.sync.service.ProgrammeMembershipSyncService;

/**
 * A listener for Mongo events associated with ConditionsOfJoining data.
 */
@Component
public class ConditionsOfJoiningEventListener
    extends AbstractMongoEventListener<ConditionsOfJoining> {

  private final ConditionsOfJoiningSyncService conditionsOfJoiningService;

  private final ProgrammeMembershipSyncService programmeMembershipSyncService;

  private final FifoMessagingService fifoMessagingService;

  private final ProgrammeMembershipMapper programmeMembershipMapper;

  private final Cache conditionsOfJoiningCache;

  private final String programmeMembershipQueueUrl;

  /**
   * Construct a listener for ConditionsOfJoining Mongo events.
   *
   * @param conditionsOfJoiningService     The Conditions of joining sync service.
   * @param programmeMembershipSyncService The Programme membership service.
   * @param fifoMessagingService           The FIFO queue messaging service.
   * @param cacheManager                   The cache for deleted records.
   * @param programmeMembershipQueueUrl    The queue to expand programme memberships into.
   */
  ConditionsOfJoiningEventListener(ConditionsOfJoiningSyncService conditionsOfJoiningService,
      ProgrammeMembershipSyncService programmeMembershipSyncService,
      ProgrammeMembershipMapper programmeMembershipMapper, CacheManager cacheManager,
      FifoMessagingService fifoMessagingService,
      @Value("${application.aws.sqs.programme-membership}") String programmeMembershipQueueUrl) {
    this.programmeMembershipSyncService = programmeMembershipSyncService;
    this.programmeMembershipMapper = programmeMembershipMapper;
    this.programmeMembershipQueueUrl = programmeMembershipQueueUrl;
    this.conditionsOfJoiningService = conditionsOfJoiningService;
    this.fifoMessagingService = fifoMessagingService;
    conditionsOfJoiningCache = cacheManager.getCache(ConditionsOfJoining.ENTITY_NAME);
  }

  /**
   * Before converting a Conditions of joining, set when it was synced from TIS, if not set.
   *
   * @param event the before-convert event for the Conditions of joining.
   */
  @Override
  public void onBeforeConvert(BeforeConvertEvent<ConditionsOfJoining> event) {
    super.onBeforeConvert(event);

    ConditionsOfJoining conditionsOfJoining = event.getSource();
    if (conditionsOfJoining.getSyncedAt() == null) {
      conditionsOfJoining.setSyncedAt(Instant.now());
    }
  }

  /**
   * After saving a Conditions of joining the related Programme Membership should be handled.
   *
   * @param event the after-save event for the Conditions of joining.
   */
  @Override
  public void onAfterSave(AfterSaveEvent<ConditionsOfJoining> event) {
    super.onAfterSave(event);

    ConditionsOfJoining conditionsOfJoining = event.getSource();
    queueRelatedProgrammeMembership(conditionsOfJoining, true);
  }

  /**
   * Before deleting a ConditionsOfJoining, ensure it is cached.
   *
   * @param event The before-delete event for the ConditionsOfJoining.
   */
  @Override
  public void onBeforeDelete(BeforeDeleteEvent<ConditionsOfJoining> event) {
    String id = event.getSource().getString("_id");
    ConditionsOfJoining conditionsOfJoining =
        conditionsOfJoiningCache.get(id, ConditionsOfJoining.class);
    if (conditionsOfJoining == null) {
      Optional<ConditionsOfJoining> newConditionsOfJoining =
          conditionsOfJoiningService.findById(id);
      newConditionsOfJoining.ifPresent(coj ->
          conditionsOfJoiningCache.put(id, coj));
    }
  }

  /**
   * After delete retrieve cached values and re-sync the related Programme Membership.
   *
   * @param event The after-delete event for the Conditions of joining.
   */
  @Override
  public void onAfterDelete(AfterDeleteEvent<ConditionsOfJoining> event) {
    super.onAfterDelete(event);
    ConditionsOfJoining conditionsOfJoining =
        conditionsOfJoiningCache.get(event.getSource().getString("_id"),
            ConditionsOfJoining.class);
    if (conditionsOfJoining != null) {
      queueRelatedProgrammeMembership(conditionsOfJoining, false);
    }
  }

  /**
   * Queue the programme membership related to the given Conditions of joining.
   *
   * @param conditionsOfJoining The Conditions of joining to get the related programme membership
   *                            for.
   * @param requestIfMissing    Whether a missing programme membership should be requested.
   */
  private void queueRelatedProgrammeMembership(ConditionsOfJoining conditionsOfJoining,
      boolean requestIfMissing) {
    String programmeMembershipUuid = conditionsOfJoining.getProgrammeMembershipUuid();
    Optional<ProgrammeMembership> programmeMembership = programmeMembershipSyncService.findById(
        programmeMembershipUuid);

    if (programmeMembership.isPresent()) {
      Record programmeMembershipRecord = programmeMembershipMapper.toRecord(
          programmeMembership.get());
      // Default the message to LOOKUP.
      programmeMembershipRecord.setOperation(Operation.LOOKUP);
      fifoMessagingService.sendMessageToFifoQueue(programmeMembershipQueueUrl,
          programmeMembershipRecord);
    } else if (requestIfMissing) {
      // Request the missing Programme Membership record.
      programmeMembershipSyncService.request(UUID.fromString(programmeMembershipUuid));
    }
  }
}

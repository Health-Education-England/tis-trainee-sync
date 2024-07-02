/*
 * The MIT License (MIT)
 *
 * Copyright 2024 Crown Copyright (Health Education England)
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
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeDeleteEvent;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.trainee.sync.model.Programme;
import uk.nhs.hee.tis.trainee.sync.model.ProgrammeMembership;
import uk.nhs.hee.tis.trainee.sync.model.UserDesignatedBody;
import uk.nhs.hee.tis.trainee.sync.service.FifoMessagingService;
import uk.nhs.hee.tis.trainee.sync.service.ProgrammeSyncService;
import uk.nhs.hee.tis.trainee.sync.service.UserDesignatedBodySyncService;

@Slf4j
@Component
public class UserDesignatedBodyEventListener extends AbstractMongoEventListener<UserDesignatedBody> {

  private static final String USER_NAME = "userName";
  private static final String DESIGNATED_BODY_CODE = "designatedBodyCode";
  private static final String PROGRAMME_ID = "id";
  private static final String PROGRAMME_NAME = "programmeName";

  private final UserDesignatedBodySyncService userDesignatedBodySyncService;
  private final ProgrammeSyncService programmeSyncService;

  private final FifoMessagingService fifoMessagingService;

  private final String programmeQueueUrl;

  private final Cache cache;

  UserDesignatedBodyEventListener(UserDesignatedBodySyncService userDesignatedBodySyncService,
      FifoMessagingService fifoMessagingService,
      ProgrammeSyncService programmeSyncService,
      @Value("${application.aws.sqs.programme}") String programmeQueueUrl,
      CacheManager cacheManager) {
    this.userDesignatedBodySyncService = userDesignatedBodySyncService;

    this.fifoMessagingService = fifoMessagingService;

    this.programmeSyncService = programmeSyncService;
    this.programmeQueueUrl = programmeQueueUrl;
    this.cache = cacheManager.getCache(UserDesignatedBody.ENTITY_NAME);
  }

  @Override
  public void onAfterSave(AfterSaveEvent<UserDesignatedBody> event) {
    super.onAfterSave(event);

    UserDesignatedBody userDesignatedBody = event.getSource();
    String userName = userDesignatedBody.getData().get(USER_NAME);
    String designatedBodyCode = userDesignatedBody.getData().get(DESIGNATED_BODY_CODE);

    if (userName != null && designatedBodyCode != null) {
      //TODO:
      String owner = referenceService.getDbc(designatedBodyCode);
      //add index
        Set<Programme> programmes = programmeSyncService.findByOwner(owner);
        log.debug("After designated body save, search for programme owned by {} to re-sync",
            owner);
        programmes.forEach(p -> {
          log.debug("Programme {}: {} found, queuing for re-sync.",
              p.getData().get("PROGRAMME_ID"), p.getData().get("PROGRAMME_NAME"));
          String deduplicationId = fifoMessagingService
              .getUniqueDeduplicationId(Programme.ENTITY_NAME, p.getTisId());
          fifoMessagingService.sendMessageToFifoQueue(programmeQueueUrl, p, deduplicationId);
        });
    }
  }

  /**
   * Before deleting a user designated body, ensure it is cached.
   *
   * @param event The before-delete event for the user designated body.
   */
  @Override
  public void onBeforeDelete(BeforeDeleteEvent<UserDesignatedBody> event) {
    super.onBeforeDelete(event);
    String id = event.getSource().get("_id").toString();
    UserDesignatedBody userDesignatedBody = cache.get(id, UserDesignatedBody.class);

    if (userDesignatedBody == null) {
      Optional<UserDesignatedBody> newUserDesignatedBody = userDesignatedBodySyncService.findById(id);
      newUserDesignatedBody.ifPresent(udbToCache -> cache.put(id, udbToCache));
    }
  }

  //Wait, userDesignatedBody only applies to a specific user.
  //need to trigger a sync of all related programmes when a DBC changes
  /**
   * Retrieve the deleted user designated body from the cache and sync the updated programmes.
   *
   * @param event The after-delete event for the user designated body.
   */
  @Override
  public void onAfterDelete(AfterDeleteEvent<UserDesignatedBody> event) {
    super.onAfterDelete(event);
    String id = event.getSource().get("_id").toString();
    UserDesignatedBody userDesignatedBody = cache.get(id, UserDesignatedBody.class);

    if (userDesignatedBody != null) {
      String userName = userDesignatedBody.getData().get(USER_NAME);
      String designatedBodyCode = userDesignatedBody.getData().get(DESIGNATED_BODY_CODE);
      //TODO
//
//        Optional<HeeUser> heeUserOptional = programmeMembershipSyncService.findByUserName(userName);
//        log.debug("After Responsible Officer role delete, search for user {} to re-sync.",
//            userName);
//        if (heeUserOptional.isPresent()) {
//          HeeUser heeUser = heeUserOptional.get();
//          log.debug("HEE user {} found, queuing for re-sync.", heeUser);
//          heeUser.setOperation(Operation.LOAD);
//          String deduplicationId = fifoMessagingService
//              .getUniqueDeduplicationId(HeeUser.ENTITY_NAME, heeUser.getTisId());
//          fifoMessagingService.sendMessageToFifoQueue(programmeMembershipQueueUrl, heeUser, deduplicationId);
//        }

    }
  }
}


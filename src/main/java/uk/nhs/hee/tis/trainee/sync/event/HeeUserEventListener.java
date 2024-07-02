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
import uk.nhs.hee.tis.trainee.sync.model.HeeUser;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.UserDesignatedBody;
import uk.nhs.hee.tis.trainee.sync.service.FifoMessagingService;
import uk.nhs.hee.tis.trainee.sync.service.HeeUserSyncService;
import uk.nhs.hee.tis.trainee.sync.service.UserDesignatedBodySyncService;

@Slf4j
@Component
public class HeeUserEventListener extends AbstractMongoEventListener<HeeUser> {

  private static final String USER_NAME = "userName";
  private static final String DESIGNATED_BODY = "designatedBodyCode";

  private final HeeUserSyncService heeUserSyncService;

  private final FifoMessagingService fifoMessagingService;

  private final UserDesignatedBodySyncService userDbSyncService;
  private final String userDesignatedBodyQueueUrl;

  private final Cache cache;

  HeeUserEventListener(HeeUserSyncService heeUserSyncService,
      FifoMessagingService fifoMessagingService,
      UserDesignatedBodySyncService userDbSyncService,
      @Value("${application.aws.sqs.user-designated-body}") String userDesignatedBodyQueueUrl,
      CacheManager cacheManager) {
    this.heeUserSyncService = heeUserSyncService;
    this.fifoMessagingService = fifoMessagingService;
    this.userDbSyncService = userDbSyncService;
    this.userDesignatedBodyQueueUrl = userDesignatedBodyQueueUrl;
    this.cache = cacheManager.getCache(HeeUser.ENTITY_NAME);
  }

  @Override
  public void onAfterSave(AfterSaveEvent<HeeUser> event) {
    super.onAfterSave(event);

    HeeUser heeUser = event.getSource();
    String userName = heeUser.getData().get(USER_NAME);

    if (userName != null) {
      Set<UserDesignatedBody> userDesignatedBodies = userDbSyncService.findByUserName(userName);
      log.debug("After HEE user save, search for designated bodies for {} to re-sync", userName);
      userDesignatedBodies.forEach(udb -> {
        log.debug("Designated body {} found, queuing for re-sync.", udb.getData().get(DESIGNATED_BODY));
        udb.setOperation(Operation.LOAD);
        String deduplicationId = fifoMessagingService
            .getUniqueDeduplicationId(UserDesignatedBody.ENTITY_NAME, udb.getTisId());
        fifoMessagingService.sendMessageToFifoQueue(userDesignatedBodyQueueUrl, udb, deduplicationId);
      });
    }
  }

  /**
   * Before deleting a HEE user, ensure it is cached.
   *
   * @param event The before-delete event for the HEE user.
   */
  @Override
  public void onBeforeDelete(BeforeDeleteEvent<HeeUser> event) {
    super.onBeforeDelete(event);
    String id = event.getSource().get("_id").toString();
    HeeUser heeUser = cache.get(id, HeeUser.class);

    if (heeUser == null) {
      Optional<HeeUser> newHeeUser = heeUserSyncService.findById(id);
      newHeeUser.ifPresent(psToCache -> cache.put(id, psToCache));
    }
  }

  /**
   * Retrieve the deleted HEE user from the cache and sync the updated designated bodies.
   *
   * @param event The after-delete event for the HEE user.
   */
  @Override
  public void onAfterDelete(AfterDeleteEvent<HeeUser> event) {
    super.onAfterDelete(event);
    String id = event.getSource().get("_id").toString();
    HeeUser heeUser = cache.get(id, HeeUser.class);

    if (heeUser != null) {
      String userName = heeUser.getData().get(USER_NAME);
      Set<UserDesignatedBody> userDesignatedBodies = userDbSyncService.findByUserName(userName);
      log.debug("After HEE user delete, search for designated bodies for {} to re-sync.",
          userName);
      userDesignatedBodies.forEach(udb -> {
        log.debug("User designated body {} found, queuing for re-sync.", udb);
        udb.setOperation(Operation.LOAD);
        String deduplicationId = fifoMessagingService
            .getUniqueDeduplicationId(UserDesignatedBody.ENTITY_NAME, udb.getTisId());
        fifoMessagingService.sendMessageToFifoQueue(userDesignatedBodyQueueUrl, udb, deduplicationId);
      });
    }
  }
}


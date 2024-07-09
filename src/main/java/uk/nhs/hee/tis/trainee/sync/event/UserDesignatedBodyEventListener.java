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
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeDeleteEvent;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.trainee.sync.model.HeeUser;
import uk.nhs.hee.tis.trainee.sync.model.UserDesignatedBody;
import uk.nhs.hee.tis.trainee.sync.service.DbcSyncService;
import uk.nhs.hee.tis.trainee.sync.service.HeeUserSyncService;
import uk.nhs.hee.tis.trainee.sync.service.UserDesignatedBodySyncService;

/**
 * A listener for UserDesignatedBody mongo events.
 */
@Slf4j
@Component
public class UserDesignatedBodyEventListener extends
    AbstractMongoEventListener<UserDesignatedBody> {

  private static final String USER_NAME = "userName";
  public static final String DESIGNATED_BODY_CODE = "designatedBodyCode";

  private final UserDesignatedBodySyncService userDesignatedBodySyncService;
  private final DbcSyncService dbcSyncService;
  private final HeeUserSyncService heeUserSyncService;

  private final Cache cache;

  UserDesignatedBodyEventListener(UserDesignatedBodySyncService userDesignatedBodySyncService,
      DbcSyncService dbcSyncService, HeeUserSyncService heeUserSyncService,
      CacheManager cacheManager) {
    this.userDesignatedBodySyncService = userDesignatedBodySyncService;
    this.dbcSyncService = dbcSyncService;
    this.heeUserSyncService = heeUserSyncService;
    this.cache = cacheManager.getCache(UserDesignatedBody.ENTITY_NAME);
  }

  @Override
  public void onAfterSave(AfterSaveEvent<UserDesignatedBody> event) {
    //NOTE: this assumes an update to a UserDesignatedBody record is received as a delete and then
    //an insert. Given the composite primary key on both table fields, this seems reasonable but
    //needs to be checked, otherwise we could have stale RO details attached to programmes where
    //a UserDesignatedBody record is modified to refer to a non-RO user.
    super.onAfterSave(event);

    UserDesignatedBody userDesignatedBody = event.getSource();
    String userName = userDesignatedBody.getData().get(USER_NAME);
    String designatedBodyCode = userDesignatedBody.getData().get(DESIGNATED_BODY_CODE);

    Optional<HeeUser> optionalHeeUser = heeUserSyncService.findByName(userName);

    if (optionalHeeUser.isPresent()) {
      log.debug("User designated body {} saved and HEE user {} found.", designatedBodyCode,
          userName);
      dbcSyncService
          .resyncProgrammesForSingleDbcIfUserIsResponsibleOfficer(userName, designatedBodyCode);
    } else {
      log.info("User designated body {} saved but HEE user {} not found, requesting data.",
          designatedBodyCode, userName);
      heeUserSyncService.request(userName);
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
      Optional<UserDesignatedBody> newUserDesignatedBody = userDesignatedBodySyncService.findById(
          id);
      newUserDesignatedBody.ifPresent(udbToCache -> cache.put(id, udbToCache));
    }
  }

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
      Optional<HeeUser> optionalHeeUser = heeUserSyncService.findByName(userName);

      if (optionalHeeUser.isPresent()) {
        log.debug("User designated body {} deleted and HEE user {} found.", designatedBodyCode,
            userName);
        dbcSyncService
            .resyncProgrammesForSingleDbcIfUserIsResponsibleOfficer(userName, designatedBodyCode);
      } else {
        log.info("User designated body {} deleted but HEE user {} not found, requesting data.",
            designatedBodyCode, userName);
        heeUserSyncService.request(userName);
      }
    }
  }
}


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
import uk.nhs.hee.tis.trainee.sync.service.DbcSyncService;
import uk.nhs.hee.tis.trainee.sync.service.HeeUserSyncService;

/**
 * A listener for HEE user mongo events.
 */
@Slf4j
@Component
public class HeeUserEventListener extends AbstractMongoEventListener<HeeUser> {

  private static final String USER_NAME = "userName";

  private final HeeUserSyncService heeUserSyncService;
  private final DbcSyncService dbcSyncService;

  private final Cache cache;

  HeeUserEventListener(HeeUserSyncService heeUserSyncService,
      DbcSyncService dbcSyncService, CacheManager cacheManager) {
    this.heeUserSyncService = heeUserSyncService;
    this.dbcSyncService = dbcSyncService;
    this.cache = cacheManager.getCache(HeeUser.ENTITY_NAME);
  }

  @Override
  public void onAfterSave(AfterSaveEvent<HeeUser> event) {
    super.onAfterSave(event);

    HeeUser heeUser = event.getSource();
    String userName = heeUser.getData().get(USER_NAME);

    dbcSyncService.resyncProgrammesIfUserIsResponsibleOfficer(userName);
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
   * Retrieve the deleted HEE user from the cache and sync related programmes if they are a
   * responsible officer.
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
      dbcSyncService.resyncProgrammesIfUserIsResponsibleOfficer(userName);
    }
  }
}


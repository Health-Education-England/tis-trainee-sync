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
import uk.nhs.hee.tis.trainee.sync.model.UserRole;
import uk.nhs.hee.tis.trainee.sync.service.DbcSyncService;
import uk.nhs.hee.tis.trainee.sync.service.HeeUserSyncService;
import uk.nhs.hee.tis.trainee.sync.service.UserRoleSyncService;

/**
 * A listener for UserRole mongo events.
 */
@Slf4j
@Component
public class UserRoleEventListener extends AbstractMongoEventListener<UserRole> {

  public static final String USER_ROLE_USER_NAME = "userName";
  public static final String USER_ROLE_ROLE_NAME = "roleName";
  public static final String RESPONSIBLE_OFFICER_ROLE = "RVOfficer";

  private final UserRoleSyncService userRoleSyncService;

  private final DbcSyncService dbcSyncService;
  private final HeeUserSyncService heeUserSyncService;

  private final Cache cache;

  UserRoleEventListener(UserRoleSyncService userRoleSyncService,
      DbcSyncService dbcSyncService, HeeUserSyncService heeUserSyncService,
      CacheManager cacheManager) {
    this.userRoleSyncService = userRoleSyncService;
    this.dbcSyncService = dbcSyncService;
    this.heeUserSyncService = heeUserSyncService;
    this.cache = cacheManager.getCache(UserRole.ENTITY_NAME);
  }

  @Override
  public void onAfterSave(AfterSaveEvent<UserRole> event) {
    //NOTE: this assumes an update to a UserRole record is received as a delete and then
    //an insert. Given the composite primary key on both table fields, this seems reasonable but
    //needs to be checked, otherwise we could have stale RO details attached to programmes where
    //a UserRole record is modified from a RO to a non-RO role.
    super.onAfterSave(event);

    syncOrRequestMissingData(event.getSource(), "saved");
  }

  /**
   * Before deleting a user role, ensure it is cached.
   *
   * @param event The before-delete event for the user role.
   */
  @Override
  public void onBeforeDelete(BeforeDeleteEvent<UserRole> event) {
    super.onBeforeDelete(event);
    String id = event.getSource().get("_id").toString();
    UserRole userRole = cache.get(id, UserRole.class);

    if (userRole == null) {
      Optional<UserRole> newUserRole = userRoleSyncService.findById(id);
      newUserRole.ifPresent(psToCache -> cache.put(id, psToCache));
    }
  }

  /**
   * Retrieve the deleted user role from the cache and sync the updated HEE user if it affects
   * Responsible Officer status.
   *
   * @param event The after-delete event for the user role.
   */
  @Override
  public void onAfterDelete(AfterDeleteEvent<UserRole> event) {
    super.onAfterDelete(event);
    String id = event.getSource().get("_id").toString();
    UserRole userRole = cache.get(id, UserRole.class);

    if (userRole != null) {
      syncOrRequestMissingData(userRole, "deleted");
    }
  }

  /**
   * Sync associated DBC programmes, or request missing HEE user data.
   *
   * @param userRole     The user role to sync from.
   * @param eventContext The event context (for logging purposes).
   */
  private void syncOrRequestMissingData(UserRole userRole, String eventContext) {
    String userName = userRole.getData().get(USER_ROLE_USER_NAME);
    String roleName = userRole.getData().get(USER_ROLE_ROLE_NAME);
    if (roleName.equalsIgnoreCase(RESPONSIBLE_OFFICER_ROLE)) {
      Optional<HeeUser> optionalHeeUser = heeUserSyncService.findByName(userName);
      if (optionalHeeUser.isPresent()) {
        log.debug("User role {} {} and HEE user {} found.", userRole, eventContext, userName);
        dbcSyncService.resyncProgrammesIfUserIsResponsibleOfficer(userName);
      } else {
        log.info("User role {} {} but HEE user {} not found, requesting data.",
            userRole, eventContext, userName);
        heeUserSyncService.request(userName);
      }
    } else {
      log.debug("Ignoring non-Responsible Officer {} role {} for HEE user {}.",
          userRole, eventContext, userName);
    }
  }
}


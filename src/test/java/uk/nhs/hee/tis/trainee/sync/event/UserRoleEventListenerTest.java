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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.nhs.hee.tis.trainee.sync.event.UserRoleEventListener.RESPONSIBLE_OFFICER_ROLE;
import static uk.nhs.hee.tis.trainee.sync.event.UserRoleEventListener.ROLE_NAME;
import static uk.nhs.hee.tis.trainee.sync.event.UserRoleEventListener.USER_NAME;

import java.util.Optional;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeDeleteEvent;
import uk.nhs.hee.tis.trainee.sync.model.HeeUser;
import uk.nhs.hee.tis.trainee.sync.model.UserRole;
import uk.nhs.hee.tis.trainee.sync.service.DbcSyncService;
import uk.nhs.hee.tis.trainee.sync.service.HeeUserSyncService;
import uk.nhs.hee.tis.trainee.sync.service.UserRoleSyncService;

class UserRoleEventListenerTest {

  private static final String USER_ROLE_ID = "99";
  private static final String USER_NAME_VALUE = "the user";

  private UserRoleEventListener listener;
  private UserRoleSyncService userRoleService;
  private DbcSyncService dbcService;
  private HeeUserSyncService heeUserService;
  private Cache cache;

  @BeforeEach
  void setUp() {
    userRoleService = mock(UserRoleSyncService.class);
    dbcService = mock(DbcSyncService.class);
    heeUserService = mock(HeeUserSyncService.class);
    CacheManager cacheManager = mock(CacheManager.class);
    cache = mock(Cache.class);
    when(cacheManager.getCache(UserRole.ENTITY_NAME)).thenReturn(cache);
    listener = new UserRoleEventListener(userRoleService, dbcService, heeUserService, cacheManager);
  }

  @Test
  void shouldNotResyncRelatedDbcsAfterSaveWhenNotRoRole() {
    UserRole userRole = new UserRole();
    userRole.setTisId(USER_ROLE_ID);
    userRole.getData().put(ROLE_NAME, "some other role");
    userRole.getData().put(USER_NAME, USER_NAME_VALUE);
    AfterSaveEvent<UserRole> event = new AfterSaveEvent<>(userRole, null, null);

    listener.onAfterSave(event);

    verifyNoInteractions(heeUserService);
    verifyNoInteractions(dbcService);
  }

  @Test
  void shouldResyncRelatedDbcsAfterSaveWhenRoRoleAndHeeUserPresent() {
    UserRole userRole = new UserRole();
    userRole.setTisId(USER_ROLE_ID);
    userRole.getData().put(ROLE_NAME, RESPONSIBLE_OFFICER_ROLE);
    userRole.getData().put(USER_NAME, USER_NAME_VALUE);
    AfterSaveEvent<UserRole> event = new AfterSaveEvent<>(userRole, null, null);

    when(heeUserService.findByName(USER_NAME_VALUE)).thenReturn(Optional.of(new HeeUser()));

    listener.onAfterSave(event);

    verify(heeUserService).findByName(USER_NAME_VALUE);
    verify(dbcService).resyncProgrammesIfUserIsResponsibleOfficer(USER_NAME_VALUE);
  }

  @Test
  void shouldRequestRelatedHeeUserAfterSaveWhenRoRoleAndHeeUserNotPresent() {
    UserRole userRole = new UserRole();
    userRole.setTisId(USER_ROLE_ID);
    userRole.getData().put(ROLE_NAME, RESPONSIBLE_OFFICER_ROLE);
    userRole.getData().put(USER_NAME, USER_NAME_VALUE);
    AfterSaveEvent<UserRole> event = new AfterSaveEvent<>(userRole, null, null);

    when(heeUserService.findByName(USER_NAME_VALUE)).thenReturn(Optional.empty());

    listener.onAfterSave(event);

    verify(heeUserService).findByName(USER_NAME_VALUE);
    verify(heeUserService).request(USER_NAME_VALUE);
    verifyNoInteractions(dbcService);
  }

  @Test
  void shouldFindAndCacheUserRoleIfNotInCacheBeforeDelete() {
    Document document = new Document();
    document.append("_id", "1");
    UserRole userRole = new UserRole();
    BeforeDeleteEvent<UserRole> event = new BeforeDeleteEvent<>(document, null, null);

    when(cache.get("1", UserRole.class)).thenReturn(null);
    when(userRoleService.findById(anyString())).thenReturn(Optional.of(userRole));

    listener.onBeforeDelete(event);

    verify(userRoleService).findById("1");
    verify(cache).put("1", userRole);
    verifyNoInteractions(dbcService);
  }

  @Test
  void shouldNotFindAndCacheUserRoleIfInCacheBeforeDelete() {
    Document document = new Document();
    document.append("_id", "1");
    UserRole userRole = new UserRole();
    BeforeDeleteEvent<UserRole> event = new BeforeDeleteEvent<>(document, null, null);

    when(cache.get("1", UserRole.class)).thenReturn(userRole);

    listener.onBeforeDelete(event);

    verifyNoInteractions(userRoleService);
    verifyNoInteractions(dbcService);
  }

  @Test
  void shouldNotResyncRelatedDbcsAfterDeleteWhenNotRoRole() {
    UserRole userRole = new UserRole();
    userRole.setTisId(USER_ROLE_ID);
    userRole.getData().put(ROLE_NAME, "some other role");
    userRole.getData().put(USER_NAME, USER_NAME_VALUE);

    when(cache.get(USER_ROLE_ID, UserRole.class)).thenReturn(userRole);

    Document document = new Document();
    document.append("_id", USER_ROLE_ID);
    AfterDeleteEvent<UserRole> eventAfter = new AfterDeleteEvent<>(document, null, null);

    listener.onAfterDelete(eventAfter);

    verifyNoInteractions(dbcService);
    verifyNoInteractions(heeUserService);
  }

  @Test
  void shouldResyncRelatedDbcsAfterDeleteWhenRoRoleAndHeeUserPresent() {
    UserRole userRole = new UserRole();
    userRole.setTisId(USER_ROLE_ID);
    userRole.getData().put(ROLE_NAME, RESPONSIBLE_OFFICER_ROLE);
    userRole.getData().put(USER_NAME, USER_NAME_VALUE);

    when(cache.get(USER_ROLE_ID, UserRole.class)).thenReturn(userRole);
    when(heeUserService.findByName(USER_NAME_VALUE)).thenReturn(Optional.of(new HeeUser()));

    Document document = new Document();
    document.append("_id", USER_ROLE_ID);
    AfterDeleteEvent<UserRole> eventAfter = new AfterDeleteEvent<>(document, null, null);

    listener.onAfterDelete(eventAfter);

    verify(dbcService).resyncProgrammesIfUserIsResponsibleOfficer(USER_NAME_VALUE);
    verify(heeUserService).findByName(USER_NAME_VALUE);
  }

  @Test
  void shouldRequestRelatedHeeUserAfterDeleteWhenRoRoleAndHeeUserNotPresent() {
    UserRole userRole = new UserRole();
    userRole.setTisId(USER_ROLE_ID);
    userRole.getData().put(ROLE_NAME, RESPONSIBLE_OFFICER_ROLE);
    userRole.getData().put(USER_NAME, USER_NAME_VALUE);

    when(cache.get(USER_ROLE_ID, UserRole.class)).thenReturn(userRole);
    when(heeUserService.findByName(USER_NAME_VALUE)).thenReturn(Optional.empty());

    Document document = new Document();
    document.append("_id", USER_ROLE_ID);
    AfterDeleteEvent<UserRole> eventAfter = new AfterDeleteEvent<>(document, null, null);

    listener.onAfterDelete(eventAfter);

    verifyNoInteractions(dbcService);
    verify(heeUserService).request(USER_NAME_VALUE);
  }

  @Test
  void shouldNotResyncRelatedDbcsAfterDeleteIfUserRoleNotInCache() {
    when(cache.get(USER_ROLE_ID, UserRole.class)).thenReturn(null);

    Document document = new Document();
    document.append("_id", USER_ROLE_ID);
    AfterDeleteEvent<UserRole> eventAfter = new AfterDeleteEvent<>(document, null, null);

    listener.onAfterDelete(eventAfter);

    verifyNoInteractions(dbcService);
  }
}

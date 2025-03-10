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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.nhs.hee.tis.trainee.sync.event.UserDesignatedBodyEventListener.USER_DB_DBC;
import static uk.nhs.hee.tis.trainee.sync.event.UserRoleEventListener.USER_ROLE_USER_NAME;

import java.util.Optional;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeDeleteEvent;
import uk.nhs.hee.tis.trainee.sync.model.Dbc;
import uk.nhs.hee.tis.trainee.sync.model.HeeUser;
import uk.nhs.hee.tis.trainee.sync.model.UserDesignatedBody;
import uk.nhs.hee.tis.trainee.sync.service.DbcSyncService;
import uk.nhs.hee.tis.trainee.sync.service.HeeUserSyncService;
import uk.nhs.hee.tis.trainee.sync.service.UserDesignatedBodySyncService;

class UserDesignatedBodyEventListenerTest {

  private static final String USER_DB_ID = "99";
  private static final String USER_NAME_VALUE = "the user";
  private static final String DESIGNATED_BODY_CODE_VALUE = "the DBC";

  private UserDesignatedBodyEventListener listener;
  private UserDesignatedBodySyncService userDbService;
  private DbcSyncService dbcService;
  private HeeUserSyncService heeUserService;
  private Cache cache;

  @BeforeEach
  void setUp() {
    userDbService = mock(UserDesignatedBodySyncService.class);
    dbcService = mock(DbcSyncService.class);
    heeUserService = mock(HeeUserSyncService.class);
    CacheManager cacheManager = mock(CacheManager.class);
    cache = mock(Cache.class);
    when(cacheManager.getCache(UserDesignatedBody.ENTITY_NAME)).thenReturn(cache);
    listener = new UserDesignatedBodyEventListener(userDbService, dbcService, heeUserService,
        cacheManager);
  }

  @Test
  void shouldResyncRelatedDbcsAfterSaveIfHeeUserAndDbcPresent() {
    UserDesignatedBody userDesignatedBody = new UserDesignatedBody();
    userDesignatedBody.setTisId(USER_DB_ID);
    userDesignatedBody.getData().put(USER_DB_DBC, DESIGNATED_BODY_CODE_VALUE);
    userDesignatedBody.getData().put(USER_ROLE_USER_NAME, USER_NAME_VALUE);
    AfterSaveEvent<UserDesignatedBody> event = new AfterSaveEvent<>(userDesignatedBody, null, null);

    when(heeUserService.findByName(USER_NAME_VALUE)).thenReturn(Optional.of(new HeeUser()));
    when(dbcService.findByDbc(DESIGNATED_BODY_CODE_VALUE)).thenReturn(Optional.of(new Dbc()));

    listener.onAfterSave(event);

    verify(dbcService).resyncProgrammesForSingleDbcIfUserIsResponsibleOfficer(USER_NAME_VALUE,
        DESIGNATED_BODY_CODE_VALUE);
    verify(heeUserService).findByName(USER_NAME_VALUE);
    verify(dbcService).findByDbc(DESIGNATED_BODY_CODE_VALUE);
    verify(heeUserService, never()).request(any());
    verify(dbcService, never()).requestByDbc(any());
  }

  @Test
  void shouldRequestRelatedHeeUserAfterSaveIfHeeUserNotPresent() {
    UserDesignatedBody userDesignatedBody = new UserDesignatedBody();
    userDesignatedBody.setTisId(USER_DB_ID);
    userDesignatedBody.getData().put(USER_DB_DBC, DESIGNATED_BODY_CODE_VALUE);
    userDesignatedBody.getData().put(USER_ROLE_USER_NAME, USER_NAME_VALUE);
    AfterSaveEvent<UserDesignatedBody> event = new AfterSaveEvent<>(userDesignatedBody, null, null);

    when(heeUserService.findByName(USER_NAME_VALUE)).thenReturn(Optional.empty());
    when(dbcService.findByDbc(DESIGNATED_BODY_CODE_VALUE)).thenReturn(Optional.of(new Dbc()));

    listener.onAfterSave(event);

    verify(heeUserService).request(USER_NAME_VALUE);
    verify(dbcService).findByDbc(DESIGNATED_BODY_CODE_VALUE);
    verifyNoMoreInteractions(dbcService);
  }

  @Test
  void shouldRequestRelatedDbcAfterSaveIfDbcNotPresent() {
    UserDesignatedBody userDesignatedBody = new UserDesignatedBody();
    userDesignatedBody.setTisId(USER_DB_ID);
    userDesignatedBody.getData().put(USER_DB_DBC, DESIGNATED_BODY_CODE_VALUE);
    userDesignatedBody.getData().put(USER_ROLE_USER_NAME, USER_NAME_VALUE);
    AfterSaveEvent<UserDesignatedBody> event = new AfterSaveEvent<>(userDesignatedBody, null, null);

    when(heeUserService.findByName(USER_NAME_VALUE)).thenReturn(Optional.of(new HeeUser()));
    when(dbcService.findByDbc(DESIGNATED_BODY_CODE_VALUE)).thenReturn(Optional.empty());

    listener.onAfterSave(event);

    verify(heeUserService, never()).request(any());
    verify(dbcService).findByDbc(DESIGNATED_BODY_CODE_VALUE);
    verify(dbcService).requestByDbc(DESIGNATED_BODY_CODE_VALUE);
    verifyNoMoreInteractions(dbcService);
  }

  @Test
  void shouldFindAndCacheUserDesignatedBodyIfNotInCacheBeforeDelete() {
    Document document = new Document();
    document.append("_id", "1");
    UserDesignatedBody userDesignatedBody = new UserDesignatedBody();
    BeforeDeleteEvent<UserDesignatedBody> event = new BeforeDeleteEvent<>(document, null, null);

    when(cache.get("1", UserDesignatedBody.class)).thenReturn(null);
    when(userDbService.findById(anyString())).thenReturn(Optional.of(userDesignatedBody));

    listener.onBeforeDelete(event);

    verify(userDbService).findById("1");
    verify(cache).put("1", userDesignatedBody);
    verifyNoInteractions(dbcService);
  }

  @Test
  void shouldNotFindAndCacheUserDesignatedBodyIfInCacheBeforeDelete() {
    Document document = new Document();
    document.append("_id", "1");
    UserDesignatedBody userDesignatedBody = new UserDesignatedBody();
    BeforeDeleteEvent<UserDesignatedBody> event = new BeforeDeleteEvent<>(document, null, null);

    when(cache.get("1", UserDesignatedBody.class)).thenReturn(userDesignatedBody);

    listener.onBeforeDelete(event);

    verifyNoInteractions(userDbService);
    verifyNoInteractions(dbcService);
  }

  @Test
  void shouldResyncRelatedDbcsAfterDeleteIfHeeUserAndDbcPresent() {
    UserDesignatedBody userDesignatedBody = new UserDesignatedBody();
    userDesignatedBody.setTisId(USER_DB_ID);
    userDesignatedBody.getData().put(USER_DB_DBC, DESIGNATED_BODY_CODE_VALUE);
    userDesignatedBody.getData().put(USER_ROLE_USER_NAME, USER_NAME_VALUE);

    when(cache.get(USER_DB_ID, UserDesignatedBody.class)).thenReturn(userDesignatedBody);
    when(heeUserService.findByName(USER_NAME_VALUE)).thenReturn(Optional.of(new HeeUser()));
    when(dbcService.findByDbc(DESIGNATED_BODY_CODE_VALUE)).thenReturn(Optional.of(new Dbc()));

    Document document = new Document();
    document.append("_id", USER_DB_ID);
    AfterDeleteEvent<UserDesignatedBody> eventAfter = new AfterDeleteEvent<>(document, null, null);

    listener.onAfterDelete(eventAfter);

    verify(dbcService).resyncProgrammesForSingleDbcIfUserIsResponsibleOfficer(USER_NAME_VALUE,
        DESIGNATED_BODY_CODE_VALUE);
    verify(heeUserService).findByName(USER_NAME_VALUE);
    verify(dbcService).findByDbc(DESIGNATED_BODY_CODE_VALUE);
    verify(heeUserService, never()).request(any());
    verify(dbcService, never()).requestByDbc(any());
  }

  @Test
  void shouldRequestRelatedHeeUserAfterDeleteIfHeeUserNotPresent() {
    UserDesignatedBody userDesignatedBody = new UserDesignatedBody();
    userDesignatedBody.setTisId(USER_DB_ID);
    userDesignatedBody.getData().put(USER_DB_DBC, DESIGNATED_BODY_CODE_VALUE);
    userDesignatedBody.getData().put(USER_ROLE_USER_NAME, USER_NAME_VALUE);

    when(cache.get(USER_DB_ID, UserDesignatedBody.class)).thenReturn(userDesignatedBody);
    when(heeUserService.findByName(USER_NAME_VALUE)).thenReturn(Optional.empty());
    when(dbcService.findByDbc(DESIGNATED_BODY_CODE_VALUE)).thenReturn(Optional.of(new Dbc()));

    Document document = new Document();
    document.append("_id", USER_DB_ID);
    AfterDeleteEvent<UserDesignatedBody> eventAfter = new AfterDeleteEvent<>(document, null, null);

    listener.onAfterDelete(eventAfter);

    verify(dbcService).findByDbc(DESIGNATED_BODY_CODE_VALUE);
    verify(heeUserService).request(USER_NAME_VALUE);
    verifyNoMoreInteractions(dbcService);
  }

  @Test
  void shouldRequestRelatedDbcAfterDeleteIfDbcNotPresent() {
    UserDesignatedBody userDesignatedBody = new UserDesignatedBody();
    userDesignatedBody.setTisId(USER_DB_ID);
    userDesignatedBody.getData().put(USER_DB_DBC, DESIGNATED_BODY_CODE_VALUE);
    userDesignatedBody.getData().put(USER_ROLE_USER_NAME, USER_NAME_VALUE);

    when(cache.get(USER_DB_ID, UserDesignatedBody.class)).thenReturn(userDesignatedBody);
    when(heeUserService.findByName(USER_NAME_VALUE)).thenReturn(Optional.of(new HeeUser()));
    when(dbcService.findByDbc(DESIGNATED_BODY_CODE_VALUE)).thenReturn(Optional.empty());

    Document document = new Document();
    document.append("_id", USER_DB_ID);
    AfterDeleteEvent<UserDesignatedBody> eventAfter = new AfterDeleteEvent<>(document, null, null);

    listener.onAfterDelete(eventAfter);

    verify(heeUserService, never()).request(any());
    verify(dbcService).findByDbc(DESIGNATED_BODY_CODE_VALUE);
    verify(dbcService).requestByDbc(DESIGNATED_BODY_CODE_VALUE);
    verifyNoMoreInteractions(dbcService);
  }

  @Test
  void shouldNotResyncRelatedDbcsAfterDeleteIfDbcNotInCache() {
    when(cache.get(USER_DB_ID, UserDesignatedBody.class)).thenReturn(null);

    Document document = new Document();
    document.append("_id", USER_DB_ID);
    AfterDeleteEvent<UserDesignatedBody> eventAfter = new AfterDeleteEvent<>(document, null, null);

    listener.onAfterDelete(eventAfter);

    verifyNoInteractions(dbcService);
  }
}

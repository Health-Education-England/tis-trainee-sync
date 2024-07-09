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
import static uk.nhs.hee.tis.trainee.sync.event.HeeUserEventListener.HEE_USER_NAME;

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
import uk.nhs.hee.tis.trainee.sync.service.DbcSyncService;
import uk.nhs.hee.tis.trainee.sync.service.HeeUserSyncService;

class HeeUserEventListenerTest {

  private static final String HEE_USER_ID = "99";
  private static final String USER_NAME_VALUE = "the user";

  private HeeUserEventListener listener;
  private HeeUserSyncService heeUserService;
  private DbcSyncService dbcService;
  private Cache cache;

  @BeforeEach
  void setUp() {
    heeUserService = mock(HeeUserSyncService.class);
    dbcService = mock(DbcSyncService.class);
    CacheManager cacheManager = mock(CacheManager.class);
    cache = mock(Cache.class);
    when(cacheManager.getCache(HeeUser.ENTITY_NAME)).thenReturn(cache);
    listener = new HeeUserEventListener(heeUserService, dbcService, cacheManager);
  }

  @Test
  void shouldResyncRelatedDbcsAfterSave() {
    HeeUser heeUser = new HeeUser();
    heeUser.setTisId(HEE_USER_ID);
    heeUser.getData().put(HEE_USER_NAME, USER_NAME_VALUE);
    AfterSaveEvent<HeeUser> event = new AfterSaveEvent<>(heeUser, null, null);

    listener.onAfterSave(event);

    verify(dbcService).resyncProgrammesIfUserIsResponsibleOfficer(USER_NAME_VALUE);
  }

  @Test
  void shouldFindAndCacheHeeUserIfNotInCacheBeforeDelete() {
    Document document = new Document();
    document.append("_id", "1");
    HeeUser heeUser = new HeeUser();
    BeforeDeleteEvent<HeeUser> event = new BeforeDeleteEvent<>(document, null, null);

    when(cache.get("1", HeeUser.class)).thenReturn(null);
    when(heeUserService.findById(anyString())).thenReturn(Optional.of(heeUser));

    listener.onBeforeDelete(event);

    verify(heeUserService).findById("1");
    verify(cache).put("1", heeUser);
    verifyNoInteractions(dbcService);
  }

  @Test
  void shouldNotFindAndCacheHeeUserIfInCacheBeforeDelete() {
    Document document = new Document();
    document.append("_id", "1");
    HeeUser heeUser = new HeeUser();
    BeforeDeleteEvent<HeeUser> event = new BeforeDeleteEvent<>(document, null, null);

    when(cache.get("1", HeeUser.class)).thenReturn(heeUser);

    listener.onBeforeDelete(event);

    verifyNoInteractions(heeUserService);
    verifyNoInteractions(dbcService);
  }

  @Test
  void shouldResyncRelatedDbcsAfterDelete() {
    HeeUser heeUser = new HeeUser();
    heeUser.setTisId(HEE_USER_ID);
    heeUser.getData().put(HEE_USER_NAME, USER_NAME_VALUE);

    when(cache.get(HEE_USER_ID, HeeUser.class)).thenReturn(heeUser);

    Document document = new Document();
    document.append("_id", HEE_USER_ID);
    AfterDeleteEvent<HeeUser> eventAfter
        = new AfterDeleteEvent<>(document, null, null);

    listener.onAfterDelete(eventAfter);

    verify(dbcService).resyncProgrammesIfUserIsResponsibleOfficer(USER_NAME_VALUE);
  }

  @Test
  void shouldNotResyncRelatedDbcsAfterDeleteIfHeeUserNotInCache() {
    when(cache.get(HEE_USER_ID, HeeUser.class)).thenReturn(null);

    Document document = new Document();
    document.append("_id", HEE_USER_ID);
    AfterDeleteEvent<HeeUser> eventAfter = new AfterDeleteEvent<>(document, null, null);

    listener.onAfterDelete(eventAfter);

    verifyNoInteractions(dbcService);
  }
}

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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeDeleteEvent;
import uk.nhs.hee.tis.trainee.sync.facade.CurriculumMembershipEnricherFacade;
import uk.nhs.hee.tis.trainee.sync.facade.ProgrammeMembershipEnricherFacade;
import uk.nhs.hee.tis.trainee.sync.model.CurriculumMembership;
import uk.nhs.hee.tis.trainee.sync.service.CurriculumMembershipSyncService;
import uk.nhs.hee.tis.trainee.sync.service.ProgrammeMembershipSyncService;

class CurriculumMembershipEventListenerTest {

  private CurriculumMembershipEventListener listener;

  private CurriculumMembershipEnricherFacade mockEnricher;

  private CurriculumMembershipSyncService mockCurriculumMembershipSyncService;

  CacheManager mockCacheManager;

  Cache mockCache;

  @BeforeEach
  void setUp() {
    mockEnricher = mock(CurriculumMembershipEnricherFacade.class);
    mockCurriculumMembershipSyncService = mock(CurriculumMembershipSyncService.class);
    mockCacheManager = mock(CacheManager.class);
    mockCache = mock(Cache.class);

    when(mockCacheManager.getCache(anyString())).thenReturn(mockCache);
    listener = new CurriculumMembershipEventListener(mockEnricher,
        mockCurriculumMembershipSyncService, mockCacheManager);
  }

  @Test
  void shouldCallEnricherAfterSave() {
    CurriculumMembership curriculumMembership = new CurriculumMembership();
    AfterSaveEvent<CurriculumMembership> event = new AfterSaveEvent<>(curriculumMembership, null,
        null);

    listener.onAfterSave(event);

    verify(mockEnricher).enrich(curriculumMembership);
    verifyNoMoreInteractions(mockEnricher);
  }

  @Test
  void shouldFindAndCacheProgrammeMembershipIfNotInCacheBeforeDelete() {
    Document document = new Document();
    document.append("_id", "1");
    CurriculumMembership curriculumMembership = new CurriculumMembership();
    BeforeDeleteEvent<CurriculumMembership> event = new BeforeDeleteEvent<>(document, null, null);

    when(mockCache.get("1", CurriculumMembership.class)).thenReturn(null);
    when(mockCurriculumMembershipSyncService.findById(anyString()))
        .thenReturn(Optional.of(curriculumMembership));

    listener.onBeforeDelete(event);

    verify(mockCurriculumMembershipSyncService).findById("1");
    verify(mockCache).put("1", curriculumMembership);
    verifyNoMoreInteractions(mockEnricher);
  }

  @Test
  void shouldNotFindAndCacheProgrammeMembershipIfInCacheBeforeDelete() {
    Document document = new Document();
    document.append("_id", "1");
    CurriculumMembership curriculumMembership = new CurriculumMembership();
    BeforeDeleteEvent<CurriculumMembership> event = new BeforeDeleteEvent<>(document, null, null);

    when(mockCache.get("1", CurriculumMembership.class)).thenReturn(curriculumMembership);

    listener.onBeforeDelete(event);

    verifyNoInteractions(mockCurriculumMembershipSyncService);
    verifyNoMoreInteractions(mockEnricher);
  }

  @Test
  void shouldCallFacadeDeleteAfterDelete() {
    Document document = new Document();
    document.append("_id", "1");
    CurriculumMembership curriculumMembership = new CurriculumMembership();
    AfterDeleteEvent<CurriculumMembership> eventAfter = new AfterDeleteEvent<>(document, null, null);

    when(mockCache.get("1", CurriculumMembership.class)).thenReturn(curriculumMembership);

    listener.onAfterDelete(eventAfter);

    verify(mockEnricher).delete(curriculumMembership);
    verifyNoMoreInteractions(mockEnricher);
  }

  @Test
  void shouldNotCallFacadeDeleteIfNoProgrammeMembership() {
    Document document = new Document();
    document.append("_id", "1");
    AfterDeleteEvent<CurriculumMembership> eventAfter = new AfterDeleteEvent<>(document, null, null);

    when(mockCache.get("1", CurriculumMembership.class)).thenReturn(null);

    listener.onAfterDelete(eventAfter);

    verifyNoInteractions(mockEnricher);
  }
}

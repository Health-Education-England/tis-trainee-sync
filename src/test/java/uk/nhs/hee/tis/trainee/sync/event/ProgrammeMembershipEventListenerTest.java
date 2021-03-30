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
import uk.nhs.hee.tis.trainee.sync.facade.ProgrammeMembershipEnricherFacade;
import uk.nhs.hee.tis.trainee.sync.model.ProgrammeMembership;
import uk.nhs.hee.tis.trainee.sync.service.ProgrammeMembershipSyncService;

class ProgrammeMembershipEventListenerTest {

  private ProgrammeMembershipEventListener listener;

  private ProgrammeMembershipEnricherFacade mockEnricher;

  private ProgrammeMembershipSyncService mockProgrammeMembershipSyncService;

  CacheManager mockCacheManager;

  Cache mockCache;

  @BeforeEach
  void setUp() {
    mockEnricher = mock(ProgrammeMembershipEnricherFacade.class);
    mockProgrammeMembershipSyncService = mock(ProgrammeMembershipSyncService.class);
    mockCacheManager = mock(CacheManager.class);
    mockCache = mock(Cache.class);

    when(mockCacheManager.getCache(anyString())).thenReturn(mockCache);
    listener = new ProgrammeMembershipEventListener(mockEnricher,
        mockProgrammeMembershipSyncService, mockCacheManager);
  }

  @Test
  void shouldCallEnricherAfterSave() {
    ProgrammeMembership record = new ProgrammeMembership();
    AfterSaveEvent<ProgrammeMembership> event = new AfterSaveEvent<>(record, null, null);

    listener.onAfterSave(event);

    verify(mockEnricher).enrich(record);
    verifyNoMoreInteractions(mockEnricher);
  }

  @Test
  void shouldFindAndCacheProgrammeMembershipIfNotInCacheBeforeDelete() {
    Document document = new Document();
    document.append("_id", "1");
    ProgrammeMembership record = new ProgrammeMembership();
    BeforeDeleteEvent<ProgrammeMembership> event = new BeforeDeleteEvent<>(document, null, null);

    when(mockCache.get("1", ProgrammeMembership.class)).thenReturn(null);
    when(mockProgrammeMembershipSyncService.findById(anyString())).thenReturn(Optional.of(record));

    listener.onBeforeDelete(event);

    verify(mockProgrammeMembershipSyncService).findById("1");
    verify(mockCache).put("1", record);
    verifyNoMoreInteractions(mockEnricher);
  }

  @Test
  void shouldNotFindAndCacheProgrammeMembershipIfInCacheBeforeDelete() {
    Document document = new Document();
    document.append("_id", "1");
    ProgrammeMembership record = new ProgrammeMembership();
    BeforeDeleteEvent<ProgrammeMembership> event = new BeforeDeleteEvent<>(document, null, null);

    when(mockCache.get("1", ProgrammeMembership.class)).thenReturn(record);

    listener.onBeforeDelete(event);

    verifyNoInteractions(mockProgrammeMembershipSyncService);
    verifyNoMoreInteractions(mockEnricher);
  }

  @Test
  void shouldCallFacadeDeleteAfterDelete() {
    Document document = new Document();
    document.append("_id", "1");
    ProgrammeMembership record = new ProgrammeMembership();
    AfterDeleteEvent<ProgrammeMembership> eventAfter = new AfterDeleteEvent<>(document, null, null);

    when(mockCache.get("1", ProgrammeMembership.class)).thenReturn(record);

    listener.onAfterDelete(eventAfter);

    verify(mockEnricher).delete(record);
    verifyNoMoreInteractions(mockEnricher);
  }
}

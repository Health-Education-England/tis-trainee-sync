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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.nhs.hee.tis.trainee.sync.model.Operation.DELETE;

import java.util.Optional;
import java.util.UUID;
import org.bson.BsonString;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeDeleteEvent;
import uk.nhs.hee.tis.trainee.sync.facade.ProgrammeMembershipEnricherFacade;
import uk.nhs.hee.tis.trainee.sync.mapper.ProgrammeMembershipMapperImpl;
import uk.nhs.hee.tis.trainee.sync.model.ProgrammeMembership;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.service.ProgrammeMembershipSyncService;
import uk.nhs.hee.tis.trainee.sync.service.TcsSyncService;

class ProgrammeMembershipEventListenerTest {

  private static final UUID ID = UUID.randomUUID();

  private ProgrammeMembershipEventListener listener;

  private ProgrammeMembershipEnricherFacade mockEnricher;
  private ProgrammeMembershipSyncService programmeMembershipService;
  private TcsSyncService tcsService;

  private Cache cache;

  @BeforeEach
  void setUp() {
    mockEnricher = mock(ProgrammeMembershipEnricherFacade.class);
    programmeMembershipService = mock(ProgrammeMembershipSyncService.class);
    tcsService = mock(TcsSyncService.class);

    CacheManager cacheManager = mock(CacheManager.class);
    cache = mock(Cache.class);
    when(cacheManager.getCache(anyString())).thenReturn(cache);

    listener = new ProgrammeMembershipEventListener(mockEnricher, programmeMembershipService,
        tcsService, new ProgrammeMembershipMapperImpl(), cacheManager);
  }

  @Test
  void shouldCallEnricherAfterSave() {
    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    AfterSaveEvent<ProgrammeMembership> event = new AfterSaveEvent<>(programmeMembership, null,
        null);

    listener.onAfterSave(event);

    verify(mockEnricher).enrich(programmeMembership);
    verifyNoMoreInteractions(mockEnricher);
  }

  @Test
  void shouldCallEnricherAfterSaveIfRoutingDocumentKeyNotRecognised() {
    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    Document routingDoc = new Document();
    routingDoc.append("some_key",
        new BsonString(ProgrammeMembershipSyncService.COJ_EVENT_ROUTING));
    AfterSaveEvent<ProgrammeMembership> event = new AfterSaveEvent<>(programmeMembership,
        routingDoc, null);

    listener.onAfterSave(event);

    verify(mockEnricher).enrich(programmeMembership);
    verifyNoMoreInteractions(mockEnricher);
  }

  @Test
  void shouldCallEnricherAfterSaveIfRoutingDocumentValueNotRecognised() {
    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    Document routingDoc = new Document();
    routingDoc.append("event_type",
        new BsonString("another value"));
    AfterSaveEvent<ProgrammeMembership> event = new AfterSaveEvent<>(programmeMembership,
        routingDoc, null);

    listener.onAfterSave(event);

    verify(mockEnricher).enrich(programmeMembership);
    verifyNoMoreInteractions(mockEnricher);
  }

  @Test
  void shouldBroadcastCojAfterSaveIfCorrectRoutingDocument() {
    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    Document routingDoc = new Document();
    routingDoc.append("event_type",
        new BsonString(ProgrammeMembershipSyncService.COJ_EVENT_ROUTING));
    AfterSaveEvent<ProgrammeMembership> event = new AfterSaveEvent<>(programmeMembership,
        routingDoc, null);

    listener.onAfterSave(event);

    verify(mockEnricher, never()).enrich(programmeMembership);
    verify(mockEnricher).broadcastCoj(programmeMembership);
    verifyNoMoreInteractions(mockEnricher);
  }

  @Test
  void shouldFindAndCacheProgrammeMembershipIfNotInCacheBeforeDelete() {
    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    when(cache.get(ID, ProgrammeMembership.class)).thenReturn(null);
    when(programmeMembershipService.findById(ID.toString())).thenReturn(
        Optional.of(programmeMembership));

    Document document = new Document();
    document.append("_id", ID);
    BeforeDeleteEvent<ProgrammeMembership> event = new BeforeDeleteEvent<>(document, null, null);

    listener.onBeforeDelete(event);

    verify(cache).put(ID, programmeMembership);
  }

  @Test
  void shouldNotFindAndCacheProgrammeMembershipIfInCacheBeforeDelete() {
    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    when(cache.get(ID, ProgrammeMembership.class)).thenReturn(programmeMembership);

    Document document = new Document();
    document.append("_id", ID);
    BeforeDeleteEvent<ProgrammeMembership> event = new BeforeDeleteEvent<>(document, null, null);

    listener.onBeforeDelete(event);

    verifyNoInteractions(programmeMembershipService);
    verify(cache, never()).put(any(), any());
  }

  @Test
  void shouldNotTriggerSyncWhenProgrammeMembershipNotInCacheAfterDelete() {
    when(cache.get(ID, ProgrammeMembership.class)).thenReturn(null);

    Document document = new Document();
    document.append("_id", ID);
    AfterDeleteEvent<ProgrammeMembership> event = new AfterDeleteEvent<>(document, null, null);

    listener.onAfterDelete(event);

    verifyNoInteractions(tcsService);
  }

  @Test
  void shouldSyncDeleteWhenProgrammeMembershipInCacheAfterDelete() {
    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    programmeMembership.setUuid(ID);
    programmeMembership.setPersonId(40L);
    when(cache.get(ID, ProgrammeMembership.class)).thenReturn(programmeMembership);

    Document document = new Document();
    document.append("_id", ID);
    AfterDeleteEvent<ProgrammeMembership> event = new AfterDeleteEvent<>(document, null, null);

    listener.onAfterDelete(event);

    ArgumentCaptor<Record> recordCaptor = ArgumentCaptor.forClass(Record.class);
    verify(tcsService).syncRecord(recordCaptor.capture());

    Record programmeMembershipRecord = recordCaptor.getValue();
    assertThat("Unexpected operation.", programmeMembershipRecord.getOperation(), is(DELETE));
    assertThat("Unexpected schema.", programmeMembershipRecord.getSchema(), is("tcs"));
    assertThat("Unexpected table.", programmeMembershipRecord.getTable(),
        is("ProgrammeMembership"));
    assertThat("Unexpected TIS ID.", programmeMembershipRecord.getTisId(), is(ID.toString()));
    assertThat("Unexpected person ID.", programmeMembershipRecord.getData().get("personId"),
        is("40"));
  }
}

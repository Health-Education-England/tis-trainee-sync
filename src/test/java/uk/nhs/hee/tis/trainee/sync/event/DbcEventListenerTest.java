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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.nhs.hee.tis.trainee.sync.event.DbcEventListener.DBC_TYPE;
import static uk.nhs.hee.tis.trainee.sync.event.DbcEventListener.DBC_TYPE_RELEVANT;
import static uk.nhs.hee.tis.trainee.sync.event.LocalOfficeEventListener.LOCAL_OFFICE_NAME;
import static uk.nhs.hee.tis.trainee.sync.model.Operation.LOOKUP;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeDeleteEvent;
import uk.nhs.hee.tis.trainee.sync.model.Dbc;
import uk.nhs.hee.tis.trainee.sync.model.LocalOffice;
import uk.nhs.hee.tis.trainee.sync.model.Programme;
import uk.nhs.hee.tis.trainee.sync.service.DbcSyncService;
import uk.nhs.hee.tis.trainee.sync.service.FifoMessagingService;
import uk.nhs.hee.tis.trainee.sync.service.LocalOfficeSyncService;
import uk.nhs.hee.tis.trainee.sync.service.ProgrammeSyncService;

class DbcEventListenerTest {

  private static final String DBC_ID = "99";
  private static final String OWNER = "heeOwner";
  private static final String ABBR = "ABCDE";
  private static final String PROGRAMME_1_ID = "1";
  private static final String PROGRAMME_2_ID = "2";

  private static final String PROGRAMME_QUEUE_URL = "queue";

  private DbcEventListener listener;
  private DbcSyncService dbcService;
  private ProgrammeSyncService programmeService;
  private LocalOfficeSyncService localOfficeService;
  private FifoMessagingService fifoMessagingService;
  private Cache cache;

  private Dbc dbc;

  @BeforeEach
  void setUp() {
    dbcService = mock(DbcSyncService.class);
    programmeService = mock(ProgrammeSyncService.class);
    localOfficeService = mock(LocalOfficeSyncService.class);
    fifoMessagingService = mock(FifoMessagingService.class);
    CacheManager cacheManager = mock(CacheManager.class);
    cache = mock(Cache.class);
    when(cacheManager.getCache(Dbc.ENTITY_NAME)).thenReturn(cache);
    listener = new DbcEventListener(dbcService, programmeService, localOfficeService,
        fifoMessagingService, PROGRAMME_QUEUE_URL, cacheManager);

    dbc = new Dbc();
    dbc.setTisId(DBC_ID);
    dbc.setData(Map.of("name", "some name", "abbr", ABBR, DBC_TYPE, DBC_TYPE_RELEVANT));
  }

  @Test
  void shouldCacheAfterSave() {
    AfterSaveEvent<Dbc> event = new AfterSaveEvent<>(dbc, null, null);

    listener.onAfterSave(event);

    verify(cache).put(DBC_ID, dbc);
  }

  @Test
  void shouldNotInteractWithProgrammeQueueAfterSaveWhenNotRelevantType() {
    dbc.setData(Map.of("name", "some name", "abbr", ABBR, DBC_TYPE, "another type"));
    AfterSaveEvent<Dbc> event = new AfterSaveEvent<>(dbc, null, null);

    listener.onAfterSave(event);

    verifyNoInteractions(fifoMessagingService);
  }

  @Test
  void shouldNotInteractWithProgrammeQueueAfterSaveWhenNoRelatedLocalOffice() {
    AfterSaveEvent<Dbc> event = new AfterSaveEvent<>(dbc, null, null);

    when(localOfficeService.findByAbbreviation(ABBR)).thenReturn(Optional.empty());

    listener.onAfterSave(event);

    verifyNoInteractions(fifoMessagingService);
  }

  @Test
  void shouldNotInteractWithProgrammeQueueAfterSaveWhenNoRelatedProgrammes() {
    final AfterSaveEvent<Dbc> event = new AfterSaveEvent<>(dbc, null, null);

    LocalOffice localOffice = new LocalOffice();
    localOffice.setData(Map.of(LOCAL_OFFICE_NAME, OWNER, "abbr", ABBR));

    when(localOfficeService.findByAbbreviation(ABBR)).thenReturn(Optional.of(localOffice));
    when(programmeService.findByOwner(OWNER)).thenReturn(Collections.emptySet());

    listener.onAfterSave(event);

    verifyNoInteractions(fifoMessagingService);
  }

  @Test
  void shouldSendRelatedProgrammesAfterSaveWhenRelatedProgrammes() {
    LocalOffice localOffice = new LocalOffice();
    localOffice.setData(Map.of(LOCAL_OFFICE_NAME, OWNER, "abbr", ABBR));

    when(localOfficeService.findByAbbreviation(ABBR)).thenReturn(Optional.of(localOffice));

    Programme programme1 = new Programme();
    programme1.setTisId(PROGRAMME_1_ID);
    Programme programme2 = new Programme();
    programme2.setTisId(PROGRAMME_2_ID);

    when(programmeService.findByOwner(OWNER)).thenReturn(Set.of(programme1, programme2));

    AfterSaveEvent<Dbc> event = new AfterSaveEvent<>(dbc, null, null);
    listener.onAfterSave(event);

    verify(fifoMessagingService).sendMessageToFifoQueue(
        eq(PROGRAMME_QUEUE_URL), eq(programme1), any());
    assertThat("Unexpected table operation.", programme1.getOperation(), is(LOOKUP));

    verify(fifoMessagingService).sendMessageToFifoQueue(
        eq(PROGRAMME_QUEUE_URL), eq(programme2), any());
    assertThat("Unexpected table operation.", programme2.getOperation(), is(LOOKUP));
  }

  @Test
  void shouldFindAndCacheDbcIfNotInCacheBeforeDelete() {
    Document document = new Document();
    document.append("_id", "1");
    BeforeDeleteEvent<Dbc> event = new BeforeDeleteEvent<>(document, null, null);

    when(cache.get("1", Dbc.class)).thenReturn(null);
    when(dbcService.findById(anyString())).thenReturn(Optional.of(dbc));

    listener.onBeforeDelete(event);

    verify(dbcService).findById("1");
    verify(cache).put("1", dbc);
    verifyNoInteractions(programmeService);
  }

  @Test
  void shouldNotFindAndCacheDbcIfInCacheBeforeDelete() {
    Document document = new Document();
    document.append("_id", "1");
    BeforeDeleteEvent<Dbc> event = new BeforeDeleteEvent<>(document, null, null);

    when(cache.get("1", Dbc.class)).thenReturn(dbc);

    listener.onBeforeDelete(event);

    verifyNoInteractions(dbcService);
    verifyNoInteractions(programmeService);
  }

  @Test
  void shouldQueueProgrammesAfterDelete() {
    LocalOffice localOffice = new LocalOffice();
    localOffice.setData(Map.of(LOCAL_OFFICE_NAME, OWNER, "abbr", ABBR));

    when(localOfficeService.findByAbbreviation(ABBR)).thenReturn(Optional.of(localOffice));

    Programme programme1 = new Programme();
    programme1.setTisId(PROGRAMME_1_ID);
    Programme programme2 = new Programme();
    programme2.setTisId(PROGRAMME_2_ID);

    when(programmeService.findByOwner(OWNER)).thenReturn(Set.of(programme1, programme2));

    when(cache.get(DBC_ID, Dbc.class)).thenReturn(dbc);

    Document document = new Document();
    document.append("_id", DBC_ID);

    AfterDeleteEvent<Dbc> eventAfter
        = new AfterDeleteEvent<>(document, null, null);

    listener.onAfterDelete(eventAfter);

    verify(fifoMessagingService).sendMessageToFifoQueue(
        eq(PROGRAMME_QUEUE_URL), eq(programme1), any());
    assertThat("Unexpected table operation.", programme1.getOperation(), is(LOOKUP));

    verify(fifoMessagingService).sendMessageToFifoQueue(
        eq(PROGRAMME_QUEUE_URL), eq(programme2), any());
    assertThat("Unexpected table operation.", programme2.getOperation(), is(LOOKUP));
  }

  @Test
  void shouldNotQueueProgrammesIfNoDbc() {
    Document document = new Document();
    document.append("_id", "1");
    AfterDeleteEvent<Dbc> eventAfter
        = new AfterDeleteEvent<>(document, null, null);

    when(cache.get("1", Dbc.class)).thenReturn(null);

    listener.onAfterDelete(eventAfter);

    verifyNoInteractions(programmeService);
  }
}

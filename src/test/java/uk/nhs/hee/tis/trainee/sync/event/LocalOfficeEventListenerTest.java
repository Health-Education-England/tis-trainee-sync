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
import static uk.nhs.hee.tis.trainee.sync.event.LocalOfficeEventListener.LOCAL_OFFICE_NAME;

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
import uk.nhs.hee.tis.trainee.sync.model.LocalOffice;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.Programme;
import uk.nhs.hee.tis.trainee.sync.service.FifoMessagingService;
import uk.nhs.hee.tis.trainee.sync.service.LocalOfficeSyncService;
import uk.nhs.hee.tis.trainee.sync.service.ProgrammeSyncService;

class LocalOfficeEventListenerTest {

  private static final String LOCAL_OFFICE_ID = "99";
  private static final String OWNER = "heeOwner";
  private static final String PROGRAMME_1_ID = "1";
  private static final String PROGRAMME_2_ID = "2";

  private static final String PROGRAMME_QUEUE_URL = "queue";

  private LocalOfficeEventListener listener;
  private LocalOfficeSyncService localOfficeService;
  private ProgrammeSyncService programmeService;
  private FifoMessagingService fifoMessagingService;
  private Cache cache;

  @BeforeEach
  void setUp() {
    localOfficeService = mock(LocalOfficeSyncService.class);
    programmeService = mock(ProgrammeSyncService.class);
    fifoMessagingService = mock(FifoMessagingService.class);
    CacheManager cacheManager = mock(CacheManager.class);
    cache = mock(Cache.class);
    when(cacheManager.getCache(LocalOffice.ENTITY_NAME)).thenReturn(cache);
    listener = new LocalOfficeEventListener(localOfficeService, programmeService,
        fifoMessagingService, PROGRAMME_QUEUE_URL, cacheManager);
  }

  @Test
  void shouldCacheAfterSave() {
    LocalOffice localOffice = new LocalOffice();
    localOffice.setTisId(LOCAL_OFFICE_ID);
    AfterSaveEvent<LocalOffice> event = new AfterSaveEvent<>(localOffice, null, null);

    listener.onAfterSave(event);

    verify(cache).put(LOCAL_OFFICE_ID, localOffice);
  }

  @Test
  void shouldNotInteractWithProgrammeQueueAfterSaveWhenNoRelatedProgrammes() {
    LocalOffice localOffice = new LocalOffice();
    localOffice.setTisId(LOCAL_OFFICE_ID);
    localOffice.setData(Map.of(LOCAL_OFFICE_NAME, OWNER));
    AfterSaveEvent<LocalOffice> event = new AfterSaveEvent<>(localOffice, null, null);

    when(programmeService.findByOwner(OWNER)).thenReturn(Collections.emptySet());

    listener.onAfterSave(event);

    verifyNoInteractions(fifoMessagingService);
  }

  @Test
  void shouldSendRelatedProgrammesAfterSaveWhenRelatedProgrammes() {
    LocalOffice localOffice = new LocalOffice();
    localOffice.setTisId(LOCAL_OFFICE_ID);
    localOffice.setData(Map.of(LOCAL_OFFICE_NAME, OWNER));

    Programme programme1 = new Programme();
    programme1.setTisId(PROGRAMME_1_ID);
    Programme programme2 = new Programme();
    programme2.setTisId(PROGRAMME_2_ID);

    when(programmeService.findByOwner(OWNER)).thenReturn(Set.of(programme1, programme2));

    AfterSaveEvent<LocalOffice> event = new AfterSaveEvent<>(localOffice, null, null);
    listener.onAfterSave(event);

    verify(fifoMessagingService).sendMessageToFifoQueue(
        eq(PROGRAMME_QUEUE_URL), eq(programme1), any());
    assertThat("Unexpected table operation.", programme1.getOperation(),
        is(Operation.LOAD));

    verify(fifoMessagingService).sendMessageToFifoQueue(
        eq(PROGRAMME_QUEUE_URL), eq(programme2), any());
    assertThat("Unexpected table operation.", programme2.getOperation(),
        is(Operation.LOAD));
  }

  @Test
  void shouldFindAndCacheLocalOfficeIfNotInCacheBeforeDelete() {
    Document document = new Document();
    document.append("_id", "1");
    LocalOffice localOffice = new LocalOffice();
    BeforeDeleteEvent<LocalOffice> event = new BeforeDeleteEvent<>(document, null, null);

    when(cache.get("1", LocalOffice.class)).thenReturn(null);
    when(localOfficeService.findById(anyString())).thenReturn(Optional.of(localOffice));

    listener.onBeforeDelete(event);

    verify(localOfficeService).findById("1");
    verify(cache).put("1", localOffice);
    verifyNoInteractions(programmeService);
  }

  @Test
  void shouldNotFindAndCacheLocalOfficeIfInCacheBeforeDelete() {
    Document document = new Document();
    document.append("_id", "1");
    LocalOffice localOffice = new LocalOffice();
    BeforeDeleteEvent<LocalOffice> event = new BeforeDeleteEvent<>(document, null, null);

    when(cache.get("1", LocalOffice.class)).thenReturn(localOffice);

    listener.onBeforeDelete(event);

    verifyNoInteractions(localOfficeService);
    verifyNoInteractions(programmeService);
  }

  @Test
  void shouldQueueProgrammesAfterDelete() {
    LocalOffice localOffice = new LocalOffice();
    localOffice.setTisId(LOCAL_OFFICE_ID);
    localOffice.setData(Map.of(LOCAL_OFFICE_NAME, OWNER));

    Programme programme1 = new Programme();
    programme1.setTisId(PROGRAMME_1_ID);
    Programme programme2 = new Programme();
    programme2.setTisId(PROGRAMME_2_ID);

    when(programmeService.findByOwner(OWNER)).thenReturn(Set.of(programme1, programme2));

    when(cache.get(LOCAL_OFFICE_ID, LocalOffice.class)).thenReturn(localOffice);

    Document document = new Document();
    document.append("_id", LOCAL_OFFICE_ID);

    AfterDeleteEvent<LocalOffice> eventAfter
        = new AfterDeleteEvent<>(document, null, null);

    listener.onAfterDelete(eventAfter);

    verify(fifoMessagingService).sendMessageToFifoQueue(
        eq(PROGRAMME_QUEUE_URL), eq(programme1), any());
    assertThat("Unexpected table operation.", programme1.getOperation(),
        is(Operation.LOAD));

    verify(fifoMessagingService).sendMessageToFifoQueue(
        eq(PROGRAMME_QUEUE_URL), eq(programme2), any());
    assertThat("Unexpected table operation.", programme2.getOperation(),
        is(Operation.LOAD));
  }

  @Test
  void shouldNotQueueProgrammesIfNoLocalOffice() {
    Document document = new Document();
    document.append("_id", "1");
    AfterDeleteEvent<LocalOffice> eventAfter
        = new AfterDeleteEvent<>(document, null, null);

    when(cache.get("1", LocalOffice.class)).thenReturn(null);

    listener.onAfterDelete(eventAfter);

    verifyNoInteractions(programmeService);
  }
}

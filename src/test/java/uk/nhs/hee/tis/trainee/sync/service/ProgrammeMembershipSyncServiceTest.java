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

package uk.nhs.hee.tis.trainee.sync.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.nhs.hee.tis.trainee.sync.model.Operation.DELETE;
import static uk.nhs.hee.tis.trainee.sync.model.Operation.LOOKUP;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import uk.nhs.hee.tis.trainee.sync.mapper.ProgrammeMembershipMapperImpl;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.ProgrammeMembership;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.repository.ProgrammeMembershipRepository;

class ProgrammeMembershipSyncServiceTest {

  private static final UUID ID = UUID.randomUUID();
  private static final UUID ID_2 = UUID.randomUUID();

  private static final Long CURRICULUM_ID = 1L;
  private static final Long PERSON_ID = 2L;
  private static final Long PROGRAMME_ID = 3L;
  private static final String PROGRAMME_MEMBERSHIP_TYPE = "SUBSTANTIVE";
  private static final LocalDate PROGRAMME_START_DATE = LocalDate.parse("2020-01-01");
  private static final LocalDate PROGRAMME_END_DATE = LocalDate.parse("2021-01-02");

  private ProgrammeMembershipSyncService service;

  private ProgrammeMembershipRepository repository;

  private FifoMessagingService fifoMessagingService;

  private Record programmeMembershipRecord;

  private ProgrammeMembership programmeMembership;

  private DataRequestService dataRequestService;

  private RequestCacheService requestCacheService;

  private ApplicationEventPublisher eventPublisher;

  private TcsSyncService tcsService;

  private Map<String, String> whereMap;

  private Map<String, String> whereMap2;

  @BeforeEach
  void setUp() {
    dataRequestService = mock(DataRequestService.class);
    repository = mock(ProgrammeMembershipRepository.class);
    fifoMessagingService = mock(FifoMessagingService.class);
    requestCacheService = mock(RequestCacheService.class);
    eventPublisher = mock(ApplicationEventPublisher.class);
    tcsService = mock(TcsSyncService.class);

    service = new ProgrammeMembershipSyncService(repository, dataRequestService,
        fifoMessagingService, "http://queue.programme-membership", requestCacheService,
        new ProgrammeMembershipMapperImpl(), eventPublisher, tcsService);
    programmeMembership = new ProgrammeMembership();
    programmeMembership.setUuid(ID);

    programmeMembershipRecord = new Record();
    programmeMembershipRecord.getData().put("uuid", ID.toString());

    whereMap = Map.of("uuid", ID.toString());
    whereMap2 = Map.of("uuid", ID_2.toString());
  }

  @Test
  void shouldThrowExceptionIfRecordNotProgrammeMembership() {
    Record recrd = new Record();
    recrd.setTable("not-pm");
    assertThrows(IllegalArgumentException.class, () -> service.syncRecord(recrd));
  }

  @ParameterizedTest(
      name = "Should send programme membership records to queue when operation is {0}.")
  @EnumSource(value = Operation.class, names = {"LOAD", "INSERT", "UPDATE", "DELETE"})
  void shouldSendProgrammeMembershipRecordsToQueue(Operation operation) {
    programmeMembershipRecord.setOperation(operation);
    programmeMembershipRecord.setTable(ProgrammeMembership.ENTITY_NAME);

    service.syncRecord(programmeMembershipRecord);

    verify(fifoMessagingService).sendMessageToFifoQueue("http://queue.programme-membership",
        programmeMembershipRecord);
    verifyNoInteractions(repository);
  }

  @ParameterizedTest(name = "Should store records when operation is {0}.")
  @EnumSource(value = Operation.class, names = {"LOAD", "INSERT", "UPDATE"})
  void shouldStoreRecords(Operation operation) {
    programmeMembershipRecord.setOperation(operation);

    service.syncProgrammeMembership(programmeMembershipRecord);

    verify(repository).save(programmeMembership);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldPublishSaveEventWhenLookupFound() {
    programmeMembershipRecord.setOperation(LOOKUP);
    when(repository.findById(ID)).thenReturn(Optional.of(programmeMembership));

    service.syncProgrammeMembership(programmeMembershipRecord);

    verify(repository, never()).save(any());

    ArgumentCaptor<AfterSaveEvent<ProgrammeMembership>> eventCaptor = ArgumentCaptor.forClass(
        AfterSaveEvent.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());

    ProgrammeMembership eventProgrammeMembership = eventCaptor.getValue().getSource();
    assertThat("Unexpected event source.", eventProgrammeMembership,
        sameInstance(programmeMembership));
  }

  @Test
  void shouldRequestDataWhenLookupNotFound() throws JsonProcessingException {
    programmeMembershipRecord.setOperation(LOOKUP);
    when(repository.findById(ID)).thenReturn(Optional.empty());

    service.syncProgrammeMembership(programmeMembershipRecord);

    verify(dataRequestService).sendRequest("ProgrammeMembership", whereMap);

    // The request is cached after it is sent, ensure it is not deleted straight away.
    verify(requestCacheService).addItemToCache(eq(ProgrammeMembership.ENTITY_NAME),
        eq(ID.toString()), any());
    verify(requestCacheService, never()).deleteItemFromCache(any(), any());
  }

  @Test
  void shouldDeleteRecordFromStore() {
    programmeMembershipRecord.setOperation(DELETE);

    service.syncProgrammeMembership(programmeMembershipRecord);

    verify(repository).deleteById(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldSyncDeletedRecordToDownstreamServices() {
    programmeMembershipRecord.setOperation(DELETE);

    service.syncProgrammeMembership(programmeMembershipRecord);

    verify(tcsService).syncRecord(programmeMembershipRecord);
    verifyNoMoreInteractions(tcsService);
  }

  @Test
  void shouldFindRecordByPersonIdWhenExists() {
    when(repository.findByPersonId(PERSON_ID)).thenReturn(
        Collections.singleton(programmeMembership));

    Set<ProgrammeMembership> foundRecords = service.findByPersonId(PERSON_ID.toString());
    assertThat("Unexpected record count.", foundRecords.size(), is(1));

    ProgrammeMembership foundRecord = foundRecords.iterator().next();
    assertThat("Unexpected record.", foundRecord, sameInstance(programmeMembership));

    verify(repository).findByPersonId(PERSON_ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotFindRecordByIdPersonWhenNotExists() {
    when(repository.findByPersonId(PERSON_ID)).thenReturn(Collections.emptySet());

    Set<ProgrammeMembership> foundRecords = service.findByPersonId(PERSON_ID.toString());
    assertThat("Unexpected record count.", foundRecords.size(), is(0));

    verify(repository).findByPersonId(PERSON_ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotFindRecordByCurriculumId() {
    Set<ProgrammeMembership> foundRecords = service.findByCurriculumId(CURRICULUM_ID.toString());
    assertThat("Unexpected record count.", foundRecords.size(), is(0));

    verifyNoInteractions(repository);
  }

  @Test
  void shouldFindRecordByProgrammeIdWhenExists() {
    when(repository.findByProgrammeId(PROGRAMME_ID)).thenReturn(
        Collections.singleton(programmeMembership));

    Set<ProgrammeMembership> foundRecords = service.findByProgrammeId(PROGRAMME_ID.toString());
    assertThat("Unexpected record count.", foundRecords.size(), is(1));

    ProgrammeMembership foundRecord = foundRecords.iterator().next();
    assertThat("Unexpected record.", foundRecord, sameInstance(programmeMembership));

    verify(repository).findByProgrammeId(PROGRAMME_ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotFindRecordByIdProgrammeWhenNotExists() {
    when(repository.findByProgrammeId(PROGRAMME_ID)).thenReturn(Collections.emptySet());

    Set<ProgrammeMembership> foundRecords = service.findByProgrammeId(PROGRAMME_ID.toString());
    assertThat("Unexpected record count.", foundRecords.size(), is(0));

    verify(repository).findByProgrammeId(PROGRAMME_ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldFindRecordBySimilarPmWhenExists() {
    when(repository.findBySimilar(PERSON_ID,
        PROGRAMME_ID, PROGRAMME_MEMBERSHIP_TYPE, PROGRAMME_START_DATE, PROGRAMME_END_DATE))
        .thenReturn(Collections.singleton(programmeMembership));

    Set<ProgrammeMembership> foundRecords = service.findBySimilar(PERSON_ID.toString(),
        PROGRAMME_ID.toString(), PROGRAMME_MEMBERSHIP_TYPE, PROGRAMME_START_DATE.toString(),
        PROGRAMME_END_DATE.toString());
    assertThat("Unexpected record count.", foundRecords.size(), is(1));

    ProgrammeMembership foundRecord = foundRecords.iterator().next();
    assertThat("Unexpected record.", foundRecord, sameInstance(programmeMembership));

    verify(repository).findBySimilar(PERSON_ID,
        PROGRAMME_ID, PROGRAMME_MEMBERSHIP_TYPE, PROGRAMME_START_DATE, PROGRAMME_END_DATE);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotFindRecordBySimilarPmWhenNotExists() {
    when(repository.findBySimilar(PERSON_ID,
        PROGRAMME_ID, PROGRAMME_MEMBERSHIP_TYPE, PROGRAMME_START_DATE, PROGRAMME_END_DATE))
        .thenReturn(Collections.emptySet());

    Set<ProgrammeMembership> foundRecords = service.findBySimilar(PERSON_ID.toString(),
        PROGRAMME_ID.toString(), PROGRAMME_MEMBERSHIP_TYPE, PROGRAMME_START_DATE.toString(),
        PROGRAMME_END_DATE.toString());
    assertThat("Unexpected record count.", foundRecords.size(), is(0));

    verify(repository).findBySimilar(PERSON_ID,
        PROGRAMME_ID, PROGRAMME_MEMBERSHIP_TYPE, PROGRAMME_START_DATE, PROGRAMME_END_DATE);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldSendRequestWhenNotAlreadyRequested() throws JsonProcessingException {
    when(requestCacheService.isItemInCache(ProgrammeMembership.ENTITY_NAME, ID.toString()))
        .thenReturn(false);
    service.request(ID);
    verify(dataRequestService).sendRequest("ProgrammeMembership", whereMap);
  }

  @Test
  void shouldNotSendRequestWhenAlreadyRequested() throws JsonProcessingException {
    when(requestCacheService.isItemInCache(ProgrammeMembership.ENTITY_NAME, ID.toString()))
        .thenReturn(true);
    service.request(ID);
    verify(dataRequestService, never()).sendRequest("ProgrammeMembership", whereMap);
    verifyNoMoreInteractions(dataRequestService);
  }

  @Test
  void shouldSendRequestWhenSyncedBetweenRequests() throws JsonProcessingException {
    when(requestCacheService.isItemInCache(ProgrammeMembership.ENTITY_NAME, ID.toString()))
        .thenReturn(false);
    service.request(ID);
    verify(requestCacheService).addItemToCache(eq(ProgrammeMembership.ENTITY_NAME),
        eq(ID.toString()), any());

    programmeMembershipRecord.setOperation(DELETE);
    service.syncProgrammeMembership(programmeMembershipRecord);
    verify(requestCacheService).deleteItemFromCache(ProgrammeMembership.ENTITY_NAME, ID.toString());

    service.request(ID);
    verify(dataRequestService, times(2))
        .sendRequest("ProgrammeMembership", whereMap);
  }

  @Test
  void shouldSendRequestWhenRequestedDifferentIds() throws JsonProcessingException {
    service.request(ID);
    service.request(ID_2);
    verify(dataRequestService, atMostOnce()).sendRequest("ProgrammeMembership", whereMap);
    verify(dataRequestService, atMostOnce()).sendRequest("ProgrammeMembership", whereMap2);
  }

  @Test
  void shouldSendRequestWhenFirstRequestFails() throws JsonProcessingException {
    doThrow(JsonProcessingException.class).when(dataRequestService)
        .sendRequest(anyString(), anyMap());

    service.request(ID);
    service.request(ID);

    verify(dataRequestService, times(2))
        .sendRequest("ProgrammeMembership", whereMap);
  }

  @Test
  void shouldCatchJsonProcessingExceptionIfThrown() throws JsonProcessingException {
    doThrow(JsonProcessingException.class).when(dataRequestService)
        .sendRequest(anyString(), anyMap());
    assertDoesNotThrow(() -> service.request(ID));
  }

  @Test
  void shouldThrowAnExceptionIfNotJsonProcessingException() throws JsonProcessingException {
    IllegalStateException illegalStateException = new IllegalStateException("error");
    doThrow(illegalStateException).when(dataRequestService).sendRequest(anyString(),
        anyMap());
    assertThrows(IllegalStateException.class, () -> service.request(ID));
    assertEquals("error", illegalStateException.getMessage());
  }
}

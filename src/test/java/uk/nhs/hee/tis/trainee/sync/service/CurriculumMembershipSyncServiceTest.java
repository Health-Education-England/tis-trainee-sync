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

import com.fasterxml.jackson.core.JsonProcessingException;
import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.nhs.hee.tis.trainee.sync.model.CurriculumMembership;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.repository.CurriculumMembershipRepository;

class CurriculumMembershipSyncServiceTest {

  private static final String ID = "40";
  private static final String ID_2 = "140";

  private static final String PERSON_ID = "1";
  private static final String PROGRAMME_ID = "1";

  private static final String PROGRAMME_MEMBERSHIP_ID = UUID.randomUUID().toString();
  private static final String PROGRAMME_MEMBERSHIP_TYPE = "SUBSTANTIVE";
  private static final String PROGRAMME_START_DATE = "2020-01-01";
  private static final String PROGRAMME_END_DATE = "2021-01-02";

  private CurriculumMembershipSyncService service;

  private CurriculumMembershipRepository repository;

  private FifoMessagingService fifoMessagingService;

  private CurriculumMembership curriculumMembership;

  private DataRequestService dataRequestService;

  private RequestCacheService requestCacheService;

  private Map<String, String> whereMap;
  private Map<String, String> whereMap2;

  private Map<String, String> whereMapPmUuid1;
  private Map<String, String> whereMapPmUuid2;

  @BeforeEach
  void setUp() {
    dataRequestService = mock(DataRequestService.class);
    repository = mock(CurriculumMembershipRepository.class);
    fifoMessagingService = mock(FifoMessagingService.class);
    requestCacheService = mock(RequestCacheService.class);

    service = new CurriculumMembershipSyncService(repository, dataRequestService,
        fifoMessagingService, "http://queue.curriculum-membership", requestCacheService);

    curriculumMembership = new CurriculumMembership();
    curriculumMembership.setTisId(ID);

    whereMap = Map.of("id", ID);
    whereMap2 = Map.of("id", ID_2);
    whereMapPmUuid1 = Map.of("programmeMembershipUuid", ID);
    whereMapPmUuid2 = Map.of("programmeMembershipUuid", ID_2);
  }

  @Test
  void shouldThrowExceptionIfRecordNotCurriculumMembership() {
    Record recrd = new Record();
    assertThrows(IllegalArgumentException.class, () -> service.syncRecord(recrd));
  }

  @ParameterizedTest(
      name = "Should send curriculum membership records to queue when operation is {0}.")
  @EnumSource(value = Operation.class, names = {"LOAD", "INSERT", "UPDATE", "DELETE"})
  void shouldSendCurriculumMembershipRecordsToQueue(Operation operation) {
    curriculumMembership.setOperation(operation);

    service.syncRecord(curriculumMembership);

    verify(fifoMessagingService).sendMessageToFifoQueue("http://queue.curriculum-membership",
        curriculumMembership);
    verifyNoInteractions(repository);
  }

  @ParameterizedTest(name = "Should store records when operation is {0}.")
  @EnumSource(value = Operation.class, names = {"LOAD", "INSERT", "UPDATE"})
  void shouldStoreRecords(Operation operation) {
    curriculumMembership.setOperation(operation);

    service.syncCurriculumMembership(curriculumMembership);

    verify(repository).save(curriculumMembership);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldDeleteRecordFromStore() {
    curriculumMembership.setOperation(DELETE);

    service.syncCurriculumMembership(curriculumMembership);

    verify(repository).deleteById(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldFindRecordByPersonIdWhenExists() {
    when(repository.findByPersonId(ID)).thenReturn(Collections.singleton(curriculumMembership));

    Set<CurriculumMembership> foundRecords = service.findByPersonId(ID);
    assertThat("Unexpected record count.", foundRecords.size(), is(1));

    CurriculumMembership foundRecord = foundRecords.iterator().next();
    assertThat("Unexpected record.", foundRecord, sameInstance(curriculumMembership));

    verify(repository).findByPersonId(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotFindRecordByIdPersonWhenNotExists() {
    when(repository.findByPersonId(ID)).thenReturn(Collections.emptySet());

    Set<CurriculumMembership> foundRecords = service.findByPersonId(ID);
    assertThat("Unexpected record count.", foundRecords.size(), is(0));

    verify(repository).findByPersonId(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldFindRecordByCurriculumIdWhenExists() {
    when(repository.findByCurriculumId(ID)).thenReturn(Collections.singleton(curriculumMembership));

    Set<CurriculumMembership> foundRecords = service.findByCurriculumId(ID);
    assertThat("Unexpected record count.", foundRecords.size(), is(1));

    CurriculumMembership foundRecord = foundRecords.iterator().next();
    assertThat("Unexpected record.", foundRecord, sameInstance(curriculumMembership));

    verify(repository).findByCurriculumId(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotFindRecordByIdCurriculumWhenNotExists() {
    when(repository.findByCurriculumId(ID)).thenReturn(Collections.emptySet());

    Set<CurriculumMembership> foundRecords = service.findByCurriculumId(ID);
    assertThat("Unexpected record count.", foundRecords.size(), is(0));

    verify(repository).findByCurriculumId(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldFindRecordByProgrammeIdWhenExists() {
    when(repository.findByProgrammeId(ID)).thenReturn(Collections.singleton(curriculumMembership));

    Set<CurriculumMembership> foundRecords = service.findByProgrammeId(ID);
    assertThat("Unexpected record count.", foundRecords.size(), is(1));

    CurriculumMembership foundRecord = foundRecords.iterator().next();
    assertThat("Unexpected record.", foundRecord, sameInstance(curriculumMembership));

    verify(repository).findByProgrammeId(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotFindRecordByIdProgrammeWhenNotExists() {
    when(repository.findByProgrammeId(ID)).thenReturn(Collections.emptySet());

    Set<CurriculumMembership> foundRecords = service.findByProgrammeId(ID);
    assertThat("Unexpected record count.", foundRecords.size(), is(0));

    verify(repository).findByProgrammeId(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldFindRecordByProgrammeMembershipUuidWhenExists() {
    when(repository.findByProgrammeMembershipUuid(PROGRAMME_MEMBERSHIP_ID)).thenReturn(
        Collections.singleton(curriculumMembership));

    Set<CurriculumMembership> foundRecords = service.findByProgrammeMembershipUuid(
        PROGRAMME_MEMBERSHIP_ID);
    assertThat("Unexpected record count.", foundRecords.size(), is(1));

    CurriculumMembership foundRecord = foundRecords.iterator().next();
    assertThat("Unexpected record.", foundRecord, sameInstance(curriculumMembership));

    verify(repository).findByProgrammeMembershipUuid(PROGRAMME_MEMBERSHIP_ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotFindRecordByIdProgrammeMembershipUuidWhenNotExists() {
    when(repository.findByProgrammeMembershipUuid(PROGRAMME_MEMBERSHIP_ID)).thenReturn(
        Collections.emptySet());

    Set<CurriculumMembership> foundRecords = service.findByProgrammeMembershipUuid(
        PROGRAMME_MEMBERSHIP_ID);
    assertThat("Unexpected record count.", foundRecords.size(), is(0));

    verify(repository).findByProgrammeMembershipUuid(PROGRAMME_MEMBERSHIP_ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldFindRecordBySimilarCmWhenExists() {

    when(repository.findBySimilar(PERSON_ID,
        PROGRAMME_ID, PROGRAMME_MEMBERSHIP_TYPE, PROGRAMME_START_DATE, PROGRAMME_END_DATE))
        .thenReturn(Collections.singleton(curriculumMembership));

    Set<CurriculumMembership> foundRecords = service.findBySimilar(PERSON_ID,
        PROGRAMME_ID, PROGRAMME_MEMBERSHIP_TYPE, PROGRAMME_START_DATE, PROGRAMME_END_DATE);
    assertThat("Unexpected record count.", foundRecords.size(), is(1));

    CurriculumMembership foundRecord = foundRecords.iterator().next();
    assertThat("Unexpected record.", foundRecord, sameInstance(curriculumMembership));

    verify(repository).findBySimilar(PERSON_ID,
        PROGRAMME_ID, PROGRAMME_MEMBERSHIP_TYPE, PROGRAMME_START_DATE, PROGRAMME_END_DATE);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotFindRecordBySimilarCmWhenNotExists() {
    when(repository.findBySimilar(PERSON_ID,
        PROGRAMME_ID, PROGRAMME_MEMBERSHIP_TYPE, PROGRAMME_START_DATE, PROGRAMME_END_DATE))
        .thenReturn(Collections.emptySet());

    Set<CurriculumMembership> foundRecords = service.findBySimilar(PERSON_ID,
        PROGRAMME_ID, PROGRAMME_MEMBERSHIP_TYPE, PROGRAMME_START_DATE, PROGRAMME_END_DATE);
    assertThat("Unexpected record count.", foundRecords.size(), is(0));

    verify(repository).findBySimilar(PERSON_ID,
        PROGRAMME_ID, PROGRAMME_MEMBERSHIP_TYPE, PROGRAMME_START_DATE, PROGRAMME_END_DATE);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldSendRequestForProgrammeMembershipWhenNotAlreadyRequested()
      throws JsonProcessingException {
    when(requestCacheService.isItemInCache(CurriculumMembership.ENTITY_NAME, ID))
        .thenReturn(false);
    service.requestForProgrammeMembership(ID);
    verify(dataRequestService).sendRequest("CurriculumMembership", whereMapPmUuid1);
  }

  @Test
  void shouldNotSendRequestForProgrammeMembershipWhenAlreadyRequested()
      throws JsonProcessingException {
    when(requestCacheService.isItemInCache(CurriculumMembership.ENTITY_NAME, ID))
        .thenReturn(true);
    service.requestForProgrammeMembership(ID);
    verify(dataRequestService, never()).sendRequest("CurriculumMembership", whereMapPmUuid1);
    verifyNoMoreInteractions(dataRequestService);
  }

  @Test
  void shouldSendRequestForProgrammeMembershipWhenSyncedBetweenRequests()
      throws JsonProcessingException {
    when(requestCacheService.isItemInCache(CurriculumMembership.ENTITY_NAME, ID))
        .thenReturn(false);
    service.requestForProgrammeMembership(ID);
    verify(requestCacheService).addItemToCache(eq(CurriculumMembership.ENTITY_NAME), eq(ID), any());

    curriculumMembership.setOperation(DELETE);
    service.syncCurriculumMembership(curriculumMembership);
    verify(requestCacheService).deleteItemFromCache(CurriculumMembership.ENTITY_NAME, ID);

    service.requestForProgrammeMembership(ID);
    verify(dataRequestService, times(2))
        .sendRequest("CurriculumMembership", whereMapPmUuid1);
  }

  @Test
  void shouldSendRequestForProgrammeMembershipWhenRequestedDifferentIds()
      throws JsonProcessingException {
    service.requestForProgrammeMembership(ID);
    service.requestForProgrammeMembership(ID_2);
    verify(dataRequestService, atMostOnce()).sendRequest("CurriculumMembership", whereMapPmUuid1);
    verify(dataRequestService, atMostOnce()).sendRequest("CurriculumMembership", whereMapPmUuid2);
  }

  @Test
  void shouldSendRequestForProgrammeMembershipWhenFirstRequestFails()
      throws JsonProcessingException {
    doThrow(JsonProcessingException.class).when(dataRequestService)
        .sendRequest(anyString(), anyMap());

    service.requestForProgrammeMembership(ID);
    service.requestForProgrammeMembership(ID);

    verify(dataRequestService, times(2))
        .sendRequest("CurriculumMembership", whereMapPmUuid1);
  }

  @Test
  void shouldCatchJsonProcessingExceptionIfThrownWhenRequestingForProgrammeMembership()
      throws JsonProcessingException {
    doThrow(JsonProcessingException.class).when(dataRequestService)
        .sendRequest(anyString(), anyMap());
    assertDoesNotThrow(() -> service.requestForProgrammeMembership(ID));
  }

  @Test
  void shouldThrowAnExceptionIfNotJsonProcessingExceptionWhenRequestingForProgrammeMembership()
      throws JsonProcessingException {
    IllegalStateException illegalStateException = new IllegalStateException("error");
    doThrow(illegalStateException).when(dataRequestService).sendRequest(anyString(),
        anyMap());
    assertThrows(IllegalStateException.class, () -> service.requestForProgrammeMembership(ID));
    assertEquals("error", illegalStateException.getMessage());
  }
}

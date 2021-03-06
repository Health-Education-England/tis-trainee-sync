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
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.nhs.hee.tis.trainee.sync.model.Operation.DELETE;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.ProgrammeMembership;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.repository.ProgrammeMembershipRepository;

class ProgrammeMembershipSyncServiceTest {

  private static final String ID = "40";
  private static final String ID_2 = "140";

  private static final String personId = "1";
  private static final String programmeId = "1";
  private static final String programmeMembershipType = "SUBSTANTIVE";
  private static final String programmeStartDate = "2020-01-01";
  private static final String programmeEndDate = "2021-01-02";

  private ProgrammeMembershipSyncService service;

  private ProgrammeMembershipRepository repository;

  private ProgrammeMembership record;

  private DataRequestService dataRequestService;

  private PostSyncService postSyncService;

  private Map<String, String> whereMap;

  private Map<String, String> whereMap2;

  @BeforeEach
  void setUp() {
    dataRequestService = mock(DataRequestService.class);
    postSyncService = mock(PostSyncService.class);
    repository = mock(ProgrammeMembershipRepository.class);
    service = new ProgrammeMembershipSyncService(repository, dataRequestService);

    record = new ProgrammeMembership();
    record.setTisId(ID);

    whereMap = Map.of("id", ID);
    whereMap2 = Map.of("id", ID_2);
  }

  @Test
  void shouldThrowExceptionIfRecordNotProgrammeMembership() {
    Record record = new Record();
    assertThrows(IllegalArgumentException.class, () -> service.syncRecord(record));
  }

  @ParameterizedTest(name = "Should store records when operation is {0}.")
  @EnumSource(value = Operation.class, names = {"LOAD", "INSERT", "UPDATE"})
  void shouldStoreRecords(Operation operation) {
    record.setOperation(operation);

    service.syncRecord(record);

    verify(repository).save(record);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldDeleteRecordFromStore() {
    record.setOperation(DELETE);

    service.syncRecord(record);

    verify(repository).deleteById(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldFindRecordByPersonIdWhenExists() {
    when(repository.findByPersonId(ID)).thenReturn(Collections.singleton(record));

    Set<ProgrammeMembership> foundRecords = service.findByPersonId(ID);
    assertThat("Unexpected record count.", foundRecords.size(), is(1));

    ProgrammeMembership foundRecord = foundRecords.iterator().next();
    assertThat("Unexpected record.", foundRecord, sameInstance(record));

    verify(repository).findByPersonId(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotFindRecordByIdPersonWhenNotExists() {
    when(repository.findByPersonId(ID)).thenReturn(Collections.emptySet());

    Set<ProgrammeMembership> foundRecords = service.findByPersonId(ID);
    assertThat("Unexpected record count.", foundRecords.size(), is(0));

    verify(repository).findByPersonId(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldFindRecordByCurriculumIdWhenExists() {
    when(repository.findByCurriculumId(ID)).thenReturn(Collections.singleton(record));

    Set<ProgrammeMembership> foundRecords = service.findByCurriculumId(ID);
    assertThat("Unexpected record count.", foundRecords.size(), is(1));

    ProgrammeMembership foundRecord = foundRecords.iterator().next();
    assertThat("Unexpected record.", foundRecord, sameInstance(record));

    verify(repository).findByCurriculumId(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotFindRecordByIdCurriculumWhenNotExists() {
    when(repository.findByCurriculumId(ID)).thenReturn(Collections.emptySet());

    Set<ProgrammeMembership> foundRecords = service.findByCurriculumId(ID);
    assertThat("Unexpected record count.", foundRecords.size(), is(0));

    verify(repository).findByCurriculumId(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldFindRecordByProgrammeIdWhenExists() {
    when(repository.findByProgrammeId(ID)).thenReturn(Collections.singleton(record));

    Set<ProgrammeMembership> foundRecords = service.findByProgrammeId(ID);
    assertThat("Unexpected record count.", foundRecords.size(), is(1));

    ProgrammeMembership foundRecord = foundRecords.iterator().next();
    assertThat("Unexpected record.", foundRecord, sameInstance(record));

    verify(repository).findByProgrammeId(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotFindRecordByIdProgrammeWhenNotExists() {
    when(repository.findByProgrammeId(ID)).thenReturn(Collections.emptySet());

    Set<ProgrammeMembership> foundRecords = service.findByProgrammeId(ID);
    assertThat("Unexpected record count.", foundRecords.size(), is(0));

    verify(repository).findByProgrammeId(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldFindRecordBySimilarPmWhenExists() {

    when(repository.findBySimilar(personId,
        programmeId, programmeMembershipType, programmeStartDate, programmeEndDate))
        .thenReturn(Collections.singleton(record));

    Set<ProgrammeMembership> foundRecords = service.findBySimilar(personId,
        programmeId, programmeMembershipType, programmeStartDate, programmeEndDate);
    assertThat("Unexpected record count.", foundRecords.size(), is(1));

    ProgrammeMembership foundRecord = foundRecords.iterator().next();
    assertThat("Unexpected record.", foundRecord, sameInstance(record));

    verify(repository).findBySimilar(personId,
        programmeId, programmeMembershipType, programmeStartDate, programmeEndDate);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotFindRecordBySimilarPmWhenNotExists() {
    when(repository.findBySimilar(personId,
        programmeId, programmeMembershipType, programmeStartDate, programmeEndDate))
        .thenReturn(Collections.emptySet());

    Set<ProgrammeMembership> foundRecords = service.findBySimilar(personId,
        programmeId, programmeMembershipType, programmeStartDate, programmeEndDate);
    assertThat("Unexpected record count.", foundRecords.size(), is(0));

    verify(repository).findBySimilar(personId,
        programmeId, programmeMembershipType, programmeStartDate, programmeEndDate);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldSendRequestWhenNotAlreadyRequested() throws JsonProcessingException {
    service.request(ID);
    verify(dataRequestService).sendRequest("ProgrammeMembership", whereMap);
  }

  @Test
  void shouldNotSendRequestWhenAlreadyRequested() throws JsonProcessingException {
    service.request(ID);
    service.request(ID);
    verify(dataRequestService, atMostOnce()).sendRequest("ProgrammeMembership", whereMap);
    verifyNoMoreInteractions(dataRequestService);
  }

  @Test
  void shouldSendRequestWhenSyncedBetweenRequests() throws JsonProcessingException {
    service.request(ID);

    record.setOperation(DELETE);
    service.syncRecord(record);

    service.request(ID);
    verify(dataRequestService, times(2))
        .sendRequest("ProgrammeMembership", whereMap);
  }

  @Test
  void shouldSendRequestWhenRequestedDifferentIds() throws JsonProcessingException {
    service.request(ID);
    service.request("140");
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
  void shouldCatchAJsonProcessingExceptionIfThrown() throws JsonProcessingException {
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

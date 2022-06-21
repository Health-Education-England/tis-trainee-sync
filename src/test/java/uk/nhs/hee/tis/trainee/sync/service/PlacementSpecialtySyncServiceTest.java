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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.PlacementSpecialty;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.repository.PlacementSpecialtyRepository;

class PlacementSpecialtySyncServiceTest {

  private static final String ID = "40";
  private static final String ID_2 = "140";
  private static final String ID_3 = "2500";
  private static final String ID_4 = "3600";

  private static final String PLACEMENT_SPECIALTY_SPECIALTY_TYPE = "placementSpecialtyType";
  private static final String PLACEMENT_SPECIALTY_PLACEMENT_ID = "placementId";

  private static final String PLACEMENT_SPECIALTY_DATA_SPECIALTY_TYPE_PRIMARY = "PRIMARY";
  private static final String PLACEMENT_SPECIALTY_DATA_SPECIALTY_TYPE_NOT_PRIMARY = "";

  private PlacementSpecialtySyncService service;

  private PlacementSpecialtyRepository repository;

  private QueueMessagingTemplate queueMessagingTemplate;

  private PlacementSpecialty placementSpecialty;

  private DataRequestService dataRequestService;

  private Map<String, String> whereMap;

  private Map<String, String> whereMap2;

  private Map<String, String> data;

  private Map<String, String> data2;

  @BeforeEach
  void setUp() {
    dataRequestService = mock(DataRequestService.class);
    repository = mock(PlacementSpecialtyRepository.class);
    queueMessagingTemplate = mock(QueueMessagingTemplate.class);
    service = new PlacementSpecialtySyncService(repository, dataRequestService,
        queueMessagingTemplate, "http://queue.placement-specialty");

    placementSpecialty = new PlacementSpecialty();
    placementSpecialty.setTisId(ID);

    whereMap = Map.of("placementId", ID, "placementSpecialtyType", "PRIMARY");
    whereMap2 = Map.of("placementId", ID_2, "placementSpecialtyType", "PRIMARY");
    data = Map.of("placementId", ID, "placementSpecialtyType", "PRIMARY", "specialtyId", ID_3);
    data2 = Map.of("placementId", ID_2, "placementSpecialtyType", "PRIMARY", "specialtyId", ID_4);
  }

  @Test
  void shouldThrowExceptionIfRecordNotPlacementSpecialty() {
    Record recrd = new Record();
    assertThrows(IllegalArgumentException.class, () -> service.syncRecord(recrd));
  }

  @ParameterizedTest(
      name = "Should send placement specialty records to queue when operation is {0}."
  )
  @EnumSource(value = Operation.class, names = {"LOAD", "INSERT", "UPDATE", "DELETE"})
  void shouldSendPlacementSpecialtyRecordsToQueue(Operation operation) {
    placementSpecialty.setOperation(operation);

    service.syncRecord(placementSpecialty);

    verify(queueMessagingTemplate)
        .convertAndSend("http://queue.placement-specialty", placementSpecialty);
    verifyNoInteractions(repository);
  }

  @ParameterizedTest(name = "Should not store non-primary records when operation is {0}.")
  @EnumSource(value = Operation.class, names = {"LOAD", "INSERT", "UPDATE"})
  void shouldNotStoreNonPrimaryPlacementSpecialtyRecords(Operation operation) {
    placementSpecialty.setOperation(operation);
    placementSpecialty.setData(new HashMap<>(Map.of(
        PLACEMENT_SPECIALTY_SPECIALTY_TYPE, PLACEMENT_SPECIALTY_DATA_SPECIALTY_TYPE_NOT_PRIMARY,
        PLACEMENT_SPECIALTY_PLACEMENT_ID, ID)));

    service.syncPlacementSpecialty(placementSpecialty);

    verify(repository, never()).save(placementSpecialty);
    verifyNoMoreInteractions(repository);
  }

  @ParameterizedTest(name = "Should store primary records when operation is {0}.")
  @EnumSource(value = Operation.class, names = {"LOAD", "INSERT", "UPDATE"})
  void shouldStorePrimaryPlacementSpecialtyRecords(Operation operation) {
    placementSpecialty.setOperation(operation);
    placementSpecialty.setData(new HashMap<>(Map.of(
        PLACEMENT_SPECIALTY_SPECIALTY_TYPE, PLACEMENT_SPECIALTY_DATA_SPECIALTY_TYPE_PRIMARY,
        PLACEMENT_SPECIALTY_PLACEMENT_ID, ID)));

    service.syncPlacementSpecialty(placementSpecialty);

    verify(repository).save(placementSpecialty);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldDeleteRecordFromStoreIfThatPlacementSpecialtyHasNotUpdatedYet() {
    placementSpecialty.setOperation(DELETE);
    placementSpecialty.setData(data);

    service.syncPlacementSpecialty(placementSpecialty);

    verify(repository).findById(placementSpecialty.getData().get(PLACEMENT_SPECIALTY_PLACEMENT_ID));
    verify(repository).deleteById(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotDeleteRecordFromStoreIfThatPlacementSpecialtyHasAlreadyUpdated() {
    placementSpecialty.setOperation(DELETE);
    placementSpecialty.setData(data);

    PlacementSpecialty newPlacementSpecialty = new PlacementSpecialty();
    newPlacementSpecialty.setData(data2);

    // newRecord(LOAD) being already present before record(DELETE) is synced
    when(repository.findById(ID)).thenReturn(Optional.of(newPlacementSpecialty));
    service.syncPlacementSpecialty(placementSpecialty);

    verify(repository).findById(placementSpecialty.getData().get(PLACEMENT_SPECIALTY_PLACEMENT_ID));
    // verify deleteById() isn't being called.
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldFindRecordBySpecialtyIdWhenExists() {
    when(repository.findPlacementSpecialtiesPrimaryOnlyBySpecialtyId(ID))
        .thenReturn(Collections.singleton(placementSpecialty));

    Set<PlacementSpecialty> foundRecords = service.findPrimaryPlacementSpecialtiesBySpecialtyId(ID);
    assertThat("Unexpected record count.", foundRecords.size(), is(1));

    PlacementSpecialty foundRecord = foundRecords.iterator().next();
    assertThat("Unexpected record.", foundRecord, sameInstance(placementSpecialty));

    verify(repository).findPlacementSpecialtiesPrimaryOnlyBySpecialtyId(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotFindRecordBySpecialtyIdWhenNotExists() {
    when(repository.findPlacementSpecialtiesPrimaryOnlyBySpecialtyId(ID))
        .thenReturn(Collections.emptySet());

    Set<PlacementSpecialty> foundRecords = service.findPrimaryPlacementSpecialtiesBySpecialtyId(ID);
    assertThat("Unexpected record count.", foundRecords.size(), is(0));

    verify(repository).findPlacementSpecialtiesPrimaryOnlyBySpecialtyId(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldSendRequestWhenNotAlreadyRequested() throws JsonProcessingException {
    service.request(ID);
    verify(dataRequestService).sendRequest("PlacementSpecialty", whereMap);
  }

  @Test
  void shouldNotSendRequestWhenAlreadyRequested() throws JsonProcessingException {
    service.request(ID);
    service.request(ID);
    verify(dataRequestService, atMostOnce()).sendRequest("PlacementSpecialty", whereMap);
    verifyNoMoreInteractions(dataRequestService);
  }

  @Test
  void shouldSendRequestWhenSyncedBetweenRequests() throws JsonProcessingException {
    service.request(ID);

    placementSpecialty.setOperation(DELETE);
    service.syncPlacementSpecialty(placementSpecialty);

    service.request(ID);
    verify(dataRequestService, times(2))
        .sendRequest("PlacementSpecialty", whereMap);
  }

  @Test
  void shouldSendRequestWhenRequestedDifferentIds() throws JsonProcessingException {
    service.request(ID);
    service.request("140");
    verify(dataRequestService, atMostOnce()).sendRequest("PlacementSpecialty", whereMap);
    verify(dataRequestService, atMostOnce()).sendRequest("PlacementSpecialty", whereMap2);
  }

  @Test
  void shouldSendRequestWhenFirstRequestFails() throws JsonProcessingException {
    doThrow(JsonProcessingException.class).when(dataRequestService)
        .sendRequest(anyString(), anyMap());

    service.request(ID);
    service.request(ID);

    verify(dataRequestService, times(2))
        .sendRequest("PlacementSpecialty", whereMap);
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

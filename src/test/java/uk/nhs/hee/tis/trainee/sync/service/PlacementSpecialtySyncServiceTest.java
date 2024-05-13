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
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.PlacementSpecialty;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.repository.PlacementSpecialtyRepository;

class PlacementSpecialtySyncServiceTest {


  private static final String PLACEMENT_SPECIALTY_ID = "a001";
  private static final String PLACEMENT_ID_1 = "40";
  private static final String PLACEMENT_ID_2 = "140";
  private static final String SPECIALTY_ID_1 = "2500";
  private static final String SPECIALTY_ID_2 = "3600";

  private static final String PLACEMENT_SPECIALTY_SPECIALTY_TYPE = "placementSpecialtyType";
  private static final String PLACEMENT_SPECIALTY_PLACEMENT_ID = "placementId";

  private static final String PLACEMENT_SPECIALTY_DATA_SPECIALTY_TYPE_PRIMARY = "PRIMARY";
  private static final String PLACEMENT_SPECIALTY_DATA_SPECIALTY_TYPE_SUB_SPECIALTY =
      "SUB_SPECIALTY";
  private static final String PLACEMENT_SPECIALTY_DATA_SPECIALTY_TYPE_OTHER = "OTHER";

  private PlacementSpecialtySyncService service;

  private PlacementSpecialtyRepository repository;

  private QueueMessagingTemplate queueMessagingTemplate;

  private PlacementSpecialty placementSpecialty;

  private DataRequestService dataRequestService;

  private RequestCacheService requestCacheService;

  private Map<String, String> whereMap;

  private Map<String, String> whereMap2;

  private Map<String, String> data;

  private Map<String, String> data2;

  @BeforeEach
  void setUp() {
    dataRequestService = mock(DataRequestService.class);
    repository = mock(PlacementSpecialtyRepository.class);
    queueMessagingTemplate = mock(QueueMessagingTemplate.class);
    requestCacheService = mock(RequestCacheService.class);

    service = new PlacementSpecialtySyncService(repository, dataRequestService,
        queueMessagingTemplate, "http://queue.placement-specialty", requestCacheService);

    placementSpecialty = new PlacementSpecialty();
    placementSpecialty.setTisId(PLACEMENT_SPECIALTY_ID);

    whereMap = Map.of("placementId", PLACEMENT_ID_1, "placementSpecialtyType", "PRIMARY");
    whereMap2 = Map.of("placementId", PLACEMENT_ID_2, "placementSpecialtyType", "PRIMARY");
    data = Map.of("placementId", PLACEMENT_ID_1, "placementSpecialtyType", "PRIMARY",
        "specialtyId", SPECIALTY_ID_1);
    data2 = Map.of("placementId", PLACEMENT_ID_2, "placementSpecialtyType", "PRIMARY",
        "specialtyId", SPECIALTY_ID_2);
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

  @ParameterizedTest(name =
      "Should not store non primary nor sub-specialty records when operation is {0}.")
  @EnumSource(value = Operation.class, names = {"LOAD", "INSERT", "UPDATE"})
  void shouldNotStoreOtherPlacementSpecialtyRecords(Operation operation) {
    placementSpecialty.setOperation(operation);
    placementSpecialty.setData(new HashMap<>(Map.of(
        PLACEMENT_SPECIALTY_SPECIALTY_TYPE, PLACEMENT_SPECIALTY_DATA_SPECIALTY_TYPE_OTHER,
        PLACEMENT_SPECIALTY_PLACEMENT_ID, PLACEMENT_ID_1)));

    service.syncPlacementSpecialty(placementSpecialty);

    verify(repository).findByPlacementIdAndSpecialtyType(
        PLACEMENT_ID_1, PLACEMENT_SPECIALTY_DATA_SPECIALTY_TYPE_OTHER);
    verify(repository, never()).save(placementSpecialty);
    verifyNoMoreInteractions(repository);
  }

  @ParameterizedTest(name = "Should store primary records when operation is {0}.")
  @EnumSource(value = Operation.class, names = {"LOAD", "INSERT", "UPDATE"})
  void shouldStorePrimaryPlacementSpecialtyRecords(Operation operation) {
    placementSpecialty.setOperation(operation);
    placementSpecialty.setData(new HashMap<>(Map.of(
        PLACEMENT_SPECIALTY_SPECIALTY_TYPE, PLACEMENT_SPECIALTY_DATA_SPECIALTY_TYPE_PRIMARY,
        PLACEMENT_SPECIALTY_PLACEMENT_ID, PLACEMENT_ID_1)));

    service.syncPlacementSpecialty(placementSpecialty);

    verify(repository).findByPlacementIdAndSpecialtyType(
        PLACEMENT_ID_1, PLACEMENT_SPECIALTY_DATA_SPECIALTY_TYPE_PRIMARY);
    verify(repository).save(placementSpecialty);
    verifyNoMoreInteractions(repository);
  }

  @ParameterizedTest(name = "Should store sub-specialty records when operation is {0}.")
  @EnumSource(value = Operation.class, names = {"LOAD", "INSERT", "UPDATE"})
  void shouldStoreSubPlacementSpecialtyRecords(Operation operation) {
    placementSpecialty.setOperation(operation);
    placementSpecialty.setData(new HashMap<>(Map.of(
        PLACEMENT_SPECIALTY_SPECIALTY_TYPE, PLACEMENT_SPECIALTY_DATA_SPECIALTY_TYPE_SUB_SPECIALTY,
        PLACEMENT_SPECIALTY_PLACEMENT_ID, PLACEMENT_ID_1)));

    service.syncPlacementSpecialty(placementSpecialty);

    verify(repository).findByPlacementIdAndSpecialtyType(
        PLACEMENT_ID_1, PLACEMENT_SPECIALTY_DATA_SPECIALTY_TYPE_SUB_SPECIALTY);
    verify(repository).save(placementSpecialty);
    verifyNoMoreInteractions(repository);
  }

  @ParameterizedTest(name = "Should update existing records when operation is {0}.")
  @EnumSource(value = Operation.class, names = {"LOAD", "INSERT", "UPDATE"})
  void shouldUpdateExistingPlacementSpecialtyRecords(Operation operation) {
    placementSpecialty.setOperation(operation);
    placementSpecialty.setData(data);

    PlacementSpecialty existingPlacementSpecialty = new PlacementSpecialty();
    existingPlacementSpecialty.setTisId(PLACEMENT_SPECIALTY_ID);
    existingPlacementSpecialty.setData(new HashMap<>(Map.of(
        PLACEMENT_SPECIALTY_SPECIALTY_TYPE, PLACEMENT_SPECIALTY_DATA_SPECIALTY_TYPE_SUB_SPECIALTY,
        PLACEMENT_SPECIALTY_PLACEMENT_ID, PLACEMENT_ID_1)));

    when(repository.findByPlacementIdAndSpecialtyType(
        PLACEMENT_ID_1, PLACEMENT_SPECIALTY_DATA_SPECIALTY_TYPE_PRIMARY))
        .thenReturn(existingPlacementSpecialty);

    service.syncPlacementSpecialty(placementSpecialty);

    ArgumentCaptor<PlacementSpecialty> captor = ArgumentCaptor.forClass(PlacementSpecialty.class);
    verify(repository).findByPlacementIdAndSpecialtyType(
        PLACEMENT_ID_1, PLACEMENT_SPECIALTY_DATA_SPECIALTY_TYPE_PRIMARY);
    verify(repository).save(captor.capture());
    PlacementSpecialty captorPlacementSpecialty = captor.getValue();
    assertThat("Unexpected ID.", captorPlacementSpecialty.getTisId(),
        is(PLACEMENT_SPECIALTY_ID));
    assertThat("Unexpected Placement Specialty details.", captorPlacementSpecialty.getData(),
        is(placementSpecialty.getData()));
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldDeleteRecordFromStoreIfThatPlacementSpecialtyHasNotUpdatedYet() {
    placementSpecialty.setOperation(DELETE);
    placementSpecialty.setData(data);

    PlacementSpecialty newPlacementSpecialty = new PlacementSpecialty();
    newPlacementSpecialty.setTisId(PLACEMENT_SPECIALTY_ID);
    newPlacementSpecialty.setData(data);

    when(repository.findByPlacementIdAndSpecialtyType(
        PLACEMENT_ID_1, PLACEMENT_SPECIALTY_DATA_SPECIALTY_TYPE_PRIMARY))
        .thenReturn(newPlacementSpecialty);
    service.syncPlacementSpecialty(placementSpecialty);

    verify(repository).findByPlacementIdAndSpecialtyType(
        placementSpecialty.getData().get(PLACEMENT_SPECIALTY_PLACEMENT_ID),
        placementSpecialty.getData().get(PLACEMENT_SPECIALTY_SPECIALTY_TYPE));
    verify(repository).deleteById(PLACEMENT_SPECIALTY_ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotDeleteRecordFromStoreIfThatPlacementSpecialtyHasAlreadyUpdated() {
    placementSpecialty.setOperation(DELETE);
    placementSpecialty.setData(data);

    PlacementSpecialty newPlacementSpecialty = new PlacementSpecialty();
    newPlacementSpecialty.setTisId(PLACEMENT_SPECIALTY_ID);
    newPlacementSpecialty.setData(data2);

    // newRecord(LOAD) being already present before record(DELETE) is synced
    when(repository.findByPlacementIdAndSpecialtyType(
        PLACEMENT_ID_1, PLACEMENT_SPECIALTY_DATA_SPECIALTY_TYPE_PRIMARY))
        .thenReturn(newPlacementSpecialty);
    service.syncPlacementSpecialty(placementSpecialty);

    verify(repository).findByPlacementIdAndSpecialtyType(
        placementSpecialty.getData().get(PLACEMENT_SPECIALTY_PLACEMENT_ID),
        placementSpecialty.getData().get(PLACEMENT_SPECIALTY_SPECIALTY_TYPE));
    // verify deleteById() isn't being called.
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotDeleteRecordFromStoreIfThatPlacementSpecialtyNotExist() {
    placementSpecialty.setOperation(DELETE);
    placementSpecialty.setData(data);

    PlacementSpecialty newPlacementSpecialty = new PlacementSpecialty();
    newPlacementSpecialty.setTisId(PLACEMENT_SPECIALTY_ID);
    newPlacementSpecialty.setData(data);

    when(repository.findByPlacementIdAndSpecialtyType(
        PLACEMENT_ID_1, PLACEMENT_SPECIALTY_DATA_SPECIALTY_TYPE_PRIMARY))
        .thenReturn(null);
    service.syncPlacementSpecialty(placementSpecialty);

    verify(repository).findByPlacementIdAndSpecialtyType(
        placementSpecialty.getData().get(PLACEMENT_SPECIALTY_PLACEMENT_ID),
        placementSpecialty.getData().get(PLACEMENT_SPECIALTY_SPECIALTY_TYPE));
    // verify deleteById() isn't being called.
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldFindRecordById() {
    when(repository.findById(PLACEMENT_ID_1)).thenReturn(Optional.of(placementSpecialty));

    Optional<PlacementSpecialty> foundRecord = service.findById(PLACEMENT_ID_1);

    assertTrue("Unexpected record count.", foundRecord.isPresent());
    assertThat("Unexpected record.", foundRecord.get(), is(placementSpecialty));
    verify(repository).findById(PLACEMENT_ID_1);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldFindRecordByPlacementIdAndSpecialtyType() {
    when(repository.findByPlacementIdAndSpecialtyType(
        PLACEMENT_ID_1, PLACEMENT_SPECIALTY_DATA_SPECIALTY_TYPE_PRIMARY))
        .thenReturn(placementSpecialty);

    PlacementSpecialty foundRecord =
        service.findPlacementSpecialtyByPlacementIdAndSpecialtyType(
            PLACEMENT_ID_1, PLACEMENT_SPECIALTY_DATA_SPECIALTY_TYPE_PRIMARY);
    assertThat("Unexpected record.", foundRecord, is(notNullValue()));
    assertThat("Unexpected record.", foundRecord, is(placementSpecialty));
    verify(repository).findByPlacementIdAndSpecialtyType(
        PLACEMENT_ID_1, PLACEMENT_SPECIALTY_DATA_SPECIALTY_TYPE_PRIMARY);
    verifyNoMoreInteractions(repository);
  }


  @Test
  void shouldFindPrimaryAndSubSpecialtyRecordBySpecialtyIdWhenExists() {
    when(repository.findPrimarySubPlacementSpecialtiesBySpecialtyId(PLACEMENT_ID_1))
        .thenReturn(Collections.singleton(placementSpecialty));

    Set<PlacementSpecialty> foundRecords =
        service.findPrimaryAndSubPlacementSpecialtiesBySpecialtyId(PLACEMENT_ID_1);
    assertThat("Unexpected record count.", foundRecords.size(), is(1));

    PlacementSpecialty foundRecord = foundRecords.iterator().next();
    assertThat("Unexpected record.", foundRecord, sameInstance(placementSpecialty));

    verify(repository).findPrimarySubPlacementSpecialtiesBySpecialtyId(PLACEMENT_ID_1);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotFindPrimaryAndSubSpecialtyRecordBySpecialtyIdWhenNotExists() {
    when(repository.findPrimarySubPlacementSpecialtiesBySpecialtyId(PLACEMENT_ID_1))
        .thenReturn(Collections.emptySet());

    Set<PlacementSpecialty> foundRecords =
        service.findPrimaryAndSubPlacementSpecialtiesBySpecialtyId(PLACEMENT_ID_1);
    assertThat("Unexpected record count.", foundRecords.size(), is(0));

    verify(repository).findPrimarySubPlacementSpecialtiesBySpecialtyId(PLACEMENT_ID_1);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldSendRequestWhenNotAlreadyRequested() throws JsonProcessingException {
    when(requestCacheService.isItemInCache(PlacementSpecialty.ENTITY_NAME, PLACEMENT_ID_1))
        .thenReturn(false);
    service.request(PLACEMENT_ID_1);
    verify(dataRequestService).sendRequest("PlacementSpecialty", whereMap);
  }

  @Test
  void shouldNotSendRequestWhenAlreadyRequested() throws JsonProcessingException {
    when(requestCacheService.isItemInCache(PlacementSpecialty.ENTITY_NAME, PLACEMENT_ID_1))
        .thenReturn(true);
    service.request(PLACEMENT_ID_1);
    verify(dataRequestService, never()).sendRequest("PlacementSpecialty", whereMap);
    verifyNoMoreInteractions(dataRequestService);
  }

  @Test
  void shouldSendRequestWhenSyncedBetweenRequests() throws JsonProcessingException {
    when(requestCacheService.isItemInCache(PlacementSpecialty.ENTITY_NAME, PLACEMENT_ID_1))
        .thenReturn(false);
    service.request(PLACEMENT_ID_1);
    verify(requestCacheService).addItemToCache(eq(PlacementSpecialty.ENTITY_NAME),
        eq(PLACEMENT_ID_1), any());

    placementSpecialty.setOperation(DELETE);
    service.syncPlacementSpecialty(placementSpecialty);
    verify(requestCacheService).deleteItemFromCache(
        PlacementSpecialty.ENTITY_NAME, PLACEMENT_SPECIALTY_ID);

    service.request(PLACEMENT_ID_1);
    verify(dataRequestService, times(2))
        .sendRequest("PlacementSpecialty", whereMap);
  }

  @Test
  void shouldSendRequestWhenRequestedDifferentIds() throws JsonProcessingException {
    service.request(PLACEMENT_ID_1);
    service.request("140");
    verify(dataRequestService, atMostOnce()).sendRequest("PlacementSpecialty", whereMap);
    verify(dataRequestService, atMostOnce()).sendRequest("PlacementSpecialty", whereMap2);
  }

  @Test
  void shouldSendRequestWhenFirstRequestFails() throws JsonProcessingException {
    doThrow(JsonProcessingException.class).when(dataRequestService)
        .sendRequest(anyString(), anyMap());

    service.request(PLACEMENT_ID_1);
    service.request(PLACEMENT_ID_1);

    verify(dataRequestService, times(2))
        .sendRequest("PlacementSpecialty", whereMap);
  }

  @Test
  void shouldCatchJsonProcessingExceptionIfThrown() throws JsonProcessingException {
    doThrow(JsonProcessingException.class).when(dataRequestService)
        .sendRequest(anyString(), anyMap());
    assertDoesNotThrow(() -> service.request(PLACEMENT_ID_1));
  }

  @Test
  void shouldThrowAnExceptionIfNotJsonProcessingException() throws JsonProcessingException {
    IllegalStateException illegalStateException = new IllegalStateException("error");
    doThrow(illegalStateException).when(dataRequestService).sendRequest(anyString(),
        anyMap());
    assertThrows(IllegalStateException.class, () -> service.request(PLACEMENT_ID_1));
    assertEquals("error", illegalStateException.getMessage());
  }
}

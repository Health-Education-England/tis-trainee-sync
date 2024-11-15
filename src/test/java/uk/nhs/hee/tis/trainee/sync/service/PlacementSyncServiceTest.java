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
import static org.hamcrest.Matchers.nullValue;
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
import static uk.nhs.hee.tis.trainee.sync.model.Placement.ENTITY_NAME;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.Placement;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.repository.PlacementRepository;

class PlacementSyncServiceTest {

  private static final String ID = "40";
  private static final String ID_2 = "140";

  private PlacementSyncService service;

  private PlacementRepository repository;

  private FifoMessagingService fifoMessagingService;

  private Placement placement;

  private DataRequestService dataRequestService;

  private RequestCacheService requestCacheService;

  private ApplicationEventPublisher eventPublisher;

  private Map<String, String> whereMap;

  private Map<String, String> whereMap2;

  @BeforeEach
  void setUp() {
    dataRequestService = mock(DataRequestService.class);
    repository = mock(PlacementRepository.class);
    fifoMessagingService = mock(FifoMessagingService.class);
    requestCacheService = mock(RequestCacheService.class);
    eventPublisher = mock(ApplicationEventPublisher.class);

    service = new PlacementSyncService(repository, dataRequestService, fifoMessagingService,
        "http://queue.placement", requestCacheService, eventPublisher);
    placement = new Placement();
    placement.setTisId(ID);

    whereMap = Map.of("id", ID);
    whereMap2 = Map.of("id", ID_2);
  }

  @Test
  void shouldThrowExceptionIfRecordNotPlacement() {
    Record recrd = new Record();
    assertThrows(IllegalArgumentException.class, () -> service.syncRecord(recrd));
  }

  @ParameterizedTest(name = "Should send placement records to queue when operation is {0}.")
  @EnumSource(value = Operation.class, names = {"LOAD", "INSERT", "UPDATE", "DELETE"})
  void shouldSendPlacementRecordsToQueue(Operation operation) {
    placement.setOperation(operation);

    service.syncRecord(placement);

    verify(fifoMessagingService).sendMessageToFifoQueue("http://queue.placement", placement);
    verifyNoInteractions(repository);
  }

  @ParameterizedTest(name = "Should store placements when operation is {0}.")
  @EnumSource(value = Operation.class, names = {"LOAD", "INSERT", "UPDATE"})
  void shouldStorePlacements(Operation operation) {
    placement.setOperation(operation);

    service.syncPlacement(placement);

    verify(repository).save(placement);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldRequestMissingPlacementWhenOperationLookupAndPlacementNotFound()
      throws JsonProcessingException {
    placement.setOperation(LOOKUP);
    when(repository.findById(ID)).thenReturn(Optional.empty());

    service.syncPlacement(placement);

    verify(repository).findById(ID);
    verifyNoMoreInteractions(repository);

    verify(dataRequestService).sendRequest(ENTITY_NAME, whereMap);

    // The request is cached after it is sent, ensure it is not deleted straight away.
    verify(requestCacheService).addItemToCache(eq(ENTITY_NAME), eq(ID), any());
    verify(requestCacheService, never()).deleteItemFromCache(any(), any());
  }

  @Test
  void shouldPublishSavePlacementEventWhenOperationLookupAndPlacementFound() {
    placement.setOperation(LOOKUP);

    Placement lookupPlacement = new Placement();
    lookupPlacement.setTisId(ID);
    lookupPlacement.setData(Map.of("dummy", "data"));
    when(repository.findById(ID)).thenReturn(Optional.of(lookupPlacement));

    service.syncPlacement(placement);

    verify(repository).findById(ID);
    verifyNoMoreInteractions(repository);

    ArgumentCaptor<AfterSaveEvent<Placement>> eventCaptor = ArgumentCaptor.captor();
    verify(eventPublisher).publishEvent(eventCaptor.capture());

    AfterSaveEvent<Placement> event = eventCaptor.getValue();
    assertThat("Unexpected event placement.", event.getSource(), sameInstance(lookupPlacement));
    assertThat("Unexpected event collection.", event.getCollectionName(), is(ENTITY_NAME));
    assertThat("Unexpected event document.", event.getDocument(), nullValue());

    verify(requestCacheService).deleteItemFromCache(ENTITY_NAME, ID);
    verifyNoMoreInteractions(requestCacheService);
  }

  @Test
  void shouldDeletePlacementFromStore() {
    placement.setOperation(DELETE);

    service.syncPlacement(placement);

    verify(repository).deleteById(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldFindRecordByIdWhenExists() {
    when(repository.findById(ID)).thenReturn(Optional.of(placement));

    Optional<Placement> foundRecord = service.findById(ID);
    assertThat("Record should be found.", foundRecord.isPresent(), is(true));

    verify(repository).findById(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotFindRecordByIdWhenNotExists() {
    when(repository.findById(ID)).thenReturn(Optional.empty());

    Optional<Placement> foundRecord = service.findById(ID);
    assertThat("Record should not be found.", foundRecord.isPresent(), is(false));

    verify(repository).findById(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldFindRecordBySiteIdWhenExists() {
    when(repository.findBySiteId(ID)).thenReturn(Collections.singleton(placement));

    Set<Placement> foundRecords = service.findBySiteId(ID);
    assertThat("Unexpected record count.", foundRecords.size(), is(1));

    Placement foundRecord = foundRecords.iterator().next();
    assertThat("Unexpected record.", foundRecord, sameInstance(placement));

    verify(repository).findBySiteId(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotFindRecordBySiteIdWhenNotExists() {
    when(repository.findBySiteId(ID)).thenReturn(Collections.emptySet());

    Set<Placement> foundRecords = service.findBySiteId(ID);
    assertThat("Unexpected record count.", foundRecords.size(), is(0));

    verify(repository).findBySiteId(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldFindRecordByPostIdWhenExists() {
    when(repository.findByPostId(ID)).thenReturn(Collections.singleton(placement));

    Set<Placement> foundRecords = service.findByPostId(ID);
    assertThat("Unexpected record count.", foundRecords.size(), is(1));

    Placement foundRecord = foundRecords.iterator().next();
    assertThat("Unexpected record.", foundRecord, sameInstance(placement));

    verify(repository).findByPostId(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotFindRecordByIdPostWhenNotExists() {
    when(repository.findByPostId(ID)).thenReturn(Collections.emptySet());

    Set<Placement> foundRecords = service.findByPostId(ID);
    assertThat("Unexpected record count.", foundRecords.size(), is(0));

    verify(repository).findByPostId(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldFindRecordByGradeIdWhenExists() {
    when(repository.findByGradeId(ID)).thenReturn(Collections.singleton(placement));

    Set<Placement> foundRecords = service.findByGradeId(ID);
    assertThat("Unexpected record count.", foundRecords.size(), is(1));

    Placement foundRecord = foundRecords.iterator().next();
    assertThat("Unexpected record.", foundRecord, sameInstance(placement));

    verify(repository).findByGradeId(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotFindRecordByGradeIdWhenNotExists() {
    when(repository.findByGradeId(ID)).thenReturn(Collections.emptySet());

    Set<Placement> foundRecords = service.findByGradeId(ID);
    assertThat("Unexpected record count.", foundRecords.size(), is(0));

    verify(repository).findByGradeId(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldSendRequestWhenNotAlreadyRequested() throws JsonProcessingException {
    when(requestCacheService.isItemInCache(ENTITY_NAME, ID)).thenReturn(false);
    service.request(ID);
    verify(dataRequestService).sendRequest("Placement", whereMap);
  }

  @Test
  void shouldNotSendRequestWhenAlreadyRequested() throws JsonProcessingException {
    when(requestCacheService.isItemInCache(ENTITY_NAME, ID)).thenReturn(true);
    service.request(ID);
    verify(dataRequestService, never()).sendRequest("Placement", whereMap);
    verifyNoMoreInteractions(dataRequestService);
  }

  @Test
  void shouldSendRequestWhenSyncedBetweenRequests() throws JsonProcessingException {
    when(requestCacheService.isItemInCache(ENTITY_NAME, ID)).thenReturn(false);
    service.request(ID);
    verify(requestCacheService).addItemToCache(eq(ENTITY_NAME), eq(ID), any());

    placement.setOperation(DELETE);
    service.syncPlacement(placement);
    verify(requestCacheService).deleteItemFromCache(ENTITY_NAME, ID);

    service.request(ID);
    verify(dataRequestService, times(2)).sendRequest("Placement", whereMap);
  }

  @Test
  void shouldSendRequestWhenRequestedDifferentIds() throws JsonProcessingException {
    service.request(ID);
    service.request("140");
    verify(dataRequestService, atMostOnce()).sendRequest("Placement", whereMap);
    verify(dataRequestService, atMostOnce()).sendRequest("Placement", whereMap2);
  }

  @Test
  void shouldSendRequestWhenFirstRequestFails() throws JsonProcessingException {
    doThrow(JsonProcessingException.class).when(dataRequestService)
        .sendRequest(anyString(), anyMap());

    service.request(ID);
    service.request(ID);

    verify(dataRequestService, times(2)).sendRequest("Placement", whereMap);
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

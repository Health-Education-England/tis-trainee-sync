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
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.nhs.hee.tis.trainee.sync.model.Operation.DELETE;
import static uk.nhs.hee.tis.trainee.sync.model.Operation.LOOKUP;
import static uk.nhs.hee.tis.trainee.sync.model.Programme.ENTITY_NAME;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import uk.nhs.hee.tis.trainee.sync.model.Programme;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.repository.ProgrammeRepository;

class ProgrammeSyncServiceTest {

  private static final String ID = "40";
  private static final String ID_2 = "140";
  private static final String OWNER = "theOwner";

  private ProgrammeSyncService service;

  private ProgrammeRepository repository;

  private DataRequestService dataRequestService;

  private RequestCacheService requestCacheService;

  private ApplicationEventPublisher eventPublisher;

  private Programme programme;

  private Map<String, String> whereMap;

  private Map<String, String> whereMap2;

  @BeforeEach
  void setUp() {
    repository = mock(ProgrammeRepository.class);
    dataRequestService = mock(DataRequestService.class);
    requestCacheService = mock(RequestCacheService.class);
    eventPublisher = mock(ApplicationEventPublisher.class);

    service = new ProgrammeSyncService(repository, dataRequestService, requestCacheService,
        eventPublisher);

    programme = new Programme();
    programme.setTisId(ID);

    whereMap = Map.of("id", ID);
    whereMap2 = Map.of("id", ID_2);
  }

  @Test
  void shouldThrowExceptionIfRecordNotProgramme() {
    Record recrd = new Record();
    assertThrows(IllegalArgumentException.class, () -> service.syncRecord(recrd));
  }

  @ParameterizedTest(name = "Should store records when operation is {0}.")
  @EnumSource(value = Operation.class, names = {"LOAD", "INSERT", "UPDATE"})
  void shouldStoreRecords(Operation operation) {
    programme.setOperation(operation);

    service.syncRecord(programme);

    verify(repository).save(programme);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldRequestMissingProgrammeWhenOperationLookupAndProgrammeNotFound()
      throws JsonProcessingException {
    programme.setOperation(LOOKUP);
    when(repository.findById(ID)).thenReturn(Optional.empty());

    service.syncRecord(programme);

    verify(repository).findById(ID);
    verifyNoMoreInteractions(repository);

    verify(dataRequestService).sendRequest(ENTITY_NAME, whereMap);

    // The request is cached after it is sent, ensure it is not deleted straight away.
    verify(requestCacheService).addItemToCache(eq(ENTITY_NAME), eq(ID), any());
    verify(requestCacheService, never()).deleteItemFromCache(any(), any());
  }

  @Test
  void shouldPublishSaveProgrammeEventWhenOperationLookupAndProgrammeFound() {
    programme.setOperation(LOOKUP);

    Programme lookupProgramme = new Programme();
    lookupProgramme.setTisId(ID);
    lookupProgramme.setData(Map.of("dummy", "data"));
    when(repository.findById(ID)).thenReturn(Optional.of(lookupProgramme));

    service.syncRecord(programme);

    verify(repository).findById(ID);
    verifyNoMoreInteractions(repository);

    ArgumentCaptor<AfterSaveEvent<Programme>> eventCaptor = ArgumentCaptor.captor();
    verify(eventPublisher).publishEvent(eventCaptor.capture());

    AfterSaveEvent<Programme> event = eventCaptor.getValue();
    assertThat("Unexpected event source.", event.getSource(), sameInstance(lookupProgramme));
    assertThat("Unexpected event collection.", event.getCollectionName(), is(ENTITY_NAME));
    assertThat("Unexpected event document.", event.getDocument(), nullValue());

    verify(requestCacheService).deleteItemFromCache(ENTITY_NAME, ID);
    verifyNoMoreInteractions(requestCacheService);
  }

  @Test
  void shouldDeleteRecordFromStore() {
    programme.setOperation(DELETE);

    service.syncRecord(programme);

    verify(repository).deleteById(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldFindRecordByIdWhenExists() {
    when(repository.findById(ID)).thenReturn(Optional.of(programme));

    Optional<Programme> found = service.findById(ID);
    assertThat("Record not found.", found.isPresent(), is(true));
    assertThat("Unexpected record.", found.orElse(null), sameInstance(programme));

    verify(repository).findById(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotFindRecordByIdWhenNotExists() {
    when(repository.findById(ID)).thenReturn(Optional.empty());

    Optional<Programme> found = service.findById(ID);
    assertThat("Record not found.", found.isEmpty(), is(true));

    verify(repository).findById(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldFindRecordsByOwnerWhenExists() {
    Programme programme2 = new Programme();
    programme2.setTisId(ID_2);
    when(repository.findByOwner(OWNER)).thenReturn(Set.of(programme, programme2));

    Set<Programme> found = service.findByOwner(OWNER);
    assertThat("Record set not found.", found.size(), is(2));
    assertThat("Unexpected record.", found.contains(programme), is(true));
    assertThat("Unexpected record.", found.contains(programme2), is(true));

    verify(repository).findByOwner(OWNER);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotFindRecordsByOwnerWhenNotExists() {
    when(repository.findByOwner(OWNER)).thenReturn(Set.of());

    Set<Programme> found = service.findByOwner(OWNER);
    assertThat("Record set not found.", found.isEmpty(), is(true));

    verify(repository).findByOwner(OWNER);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldSendRequestWhenNotAlreadyRequested() throws JsonProcessingException {
    when(requestCacheService.isItemInCache(Programme.ENTITY_NAME, ID)).thenReturn(false);
    service.request(ID);
    verify(dataRequestService).sendRequest("Programme", whereMap);
  }

  @Test
  void shouldNotSendRequestWhenAlreadyRequested() throws JsonProcessingException {
    when(requestCacheService.isItemInCache(Programme.ENTITY_NAME, ID)).thenReturn(true);
    service.request(ID);
    verify(dataRequestService, never()).sendRequest("Programme", whereMap);
    verifyNoMoreInteractions(dataRequestService);
  }

  @Test
  void shouldSendRequestWhenSyncedBetweenRequests() throws JsonProcessingException {
    when(requestCacheService.isItemInCache(Programme.ENTITY_NAME, ID)).thenReturn(false);
    service.request(ID);
    verify(requestCacheService).addItemToCache(eq(Programme.ENTITY_NAME), eq(ID), any());

    programme.setOperation(DELETE);
    service.syncRecord(programme);
    verify(requestCacheService).deleteItemFromCache(Programme.ENTITY_NAME, ID);

    service.request(ID);
    verify(dataRequestService, times(2)).sendRequest("Programme", whereMap);
  }

  @Test
  void shouldSendRequestWhenRequestedDifferentIds() throws JsonProcessingException {
    service.request(ID);
    service.request("140");
    verify(dataRequestService, atMostOnce()).sendRequest("Programme", whereMap);
    verify(dataRequestService, atMostOnce()).sendRequest("Programme", whereMap2);
  }

  @Test
  void shouldSendRequestWhenFirstRequestFails() throws JsonProcessingException {
    doThrow(JsonProcessingException.class).when(dataRequestService)
        .sendRequest(anyString(), anyMap());

    service.request(ID);
    service.request(ID);

    verify(dataRequestService, times(2)).sendRequest("Programme", whereMap);
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

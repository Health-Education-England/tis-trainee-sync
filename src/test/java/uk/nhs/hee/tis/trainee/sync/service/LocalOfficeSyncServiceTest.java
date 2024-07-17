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
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.nhs.hee.tis.trainee.sync.model.Operation.DELETE;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.nhs.hee.tis.trainee.sync.model.LocalOffice;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.repository.LocalOfficeRepository;

class LocalOfficeSyncServiceTest {

  private static final String ID = "40";
  private static final String ABBR = "ABC";
  private static final String ABBR_2 = "DEFG";

  private LocalOfficeSyncService service;

  private LocalOfficeRepository repository;

  private DataRequestService dataRequestService;

  private ReferenceSyncService referenceSyncService;

  private RequestCacheService requestCacheService;

  private LocalOffice localOffice;

  private Map<String, String> whereMap;

  private Map<String, String> whereMap2;

  @BeforeEach
  void setUp() {
    repository = mock(LocalOfficeRepository.class);
    dataRequestService = mock(DataRequestService.class);
    referenceSyncService = mock(ReferenceSyncService.class);
    requestCacheService = mock(RequestCacheService.class);

    service = new LocalOfficeSyncService(repository, dataRequestService, referenceSyncService,
        requestCacheService);

    localOffice = new LocalOffice();
    localOffice.setTisId(ID);

    whereMap = Map.of("abbreviation", ABBR);
    whereMap2 = Map.of("abbreviation", ABBR_2);
  }

  @Test
  void shouldThrowExceptionIfRecordNotLocalOffice() {
    Record recrd = new Record();
    assertThrows(IllegalArgumentException.class, () -> service.syncRecord(recrd));
  }

  @ParameterizedTest(name = "Should store records when operation is {0}.")
  @EnumSource(value = Operation.class, names = {"LOAD", "INSERT", "UPDATE"})
  void shouldStoreRecords(Operation operation) {
    localOffice.setOperation(operation);

    service.syncRecord(localOffice);

    verify(repository).save(localOffice);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldDeleteRecordFromStore() {
    localOffice.setOperation(DELETE);

    service.syncRecord(localOffice);

    verify(repository).deleteById(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldFindRecordByIdWhenExists() {
    when(repository.findById(ID)).thenReturn(Optional.of(localOffice));

    Optional<LocalOffice> found = service.findById(ID);
    assertThat("Record not found.", found.isPresent(), is(true));
    assertThat("Unexpected record.", found.orElse(null), sameInstance(localOffice));

    verify(repository).findById(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotFindRecordByIdWhenNotExists() {
    when(repository.findById(ID)).thenReturn(Optional.empty());

    Optional<LocalOffice> found = service.findById(ID);
    assertThat("Record not found.", found.isEmpty(), is(true));

    verify(repository).findById(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldFindRecordByAbbreviationWhenExists() {
    when(repository.findByAbbreviation(ABBR)).thenReturn(Optional.of(localOffice));

    Optional<LocalOffice> found = service.findByAbbreviation(ABBR);
    assertThat("Record not found.", found.isPresent(), is(true));
    assertThat("Unexpected record.", found.orElse(null), sameInstance(localOffice));

    verify(repository).findByAbbreviation(ABBR);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotFindRecordByAbbreviationWhenNotExists() {
    when(repository.findByAbbreviation(ABBR)).thenReturn(Optional.empty());

    Optional<LocalOffice> found = service.findByAbbreviation(ABBR);
    assertThat("Record not found.", found.isEmpty(), is(true));

    verify(repository).findByAbbreviation(ABBR);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldSendRequestWhenNotAlreadyRequested() throws JsonProcessingException {
    when(requestCacheService.isItemInCache(LocalOffice.ENTITY_NAME, ABBR)).thenReturn(false);
    service.request(ABBR);
    verify(dataRequestService).sendRequest(LocalOffice.SCHEMA_NAME, LocalOffice.ENTITY_NAME,
        whereMap);
  }

  @Test
  void shouldNotSendRequestWhenAlreadyRequested() throws JsonProcessingException {
    when(requestCacheService.isItemInCache(LocalOffice.ENTITY_NAME, ABBR)).thenReturn(true);
    service.request(ABBR);
    verify(dataRequestService, never()).sendRequest(LocalOffice.SCHEMA_NAME,
        LocalOffice.ENTITY_NAME, whereMap);
    verifyNoMoreInteractions(dataRequestService);
  }

  @Test
  void shouldSendRequestWhenSyncedBetweenRequests() throws JsonProcessingException {
    when(requestCacheService.isItemInCache(LocalOffice.ENTITY_NAME, ABBR)).thenReturn(false);
    service.request(ABBR);
    verify(requestCacheService).addItemToCache(eq(LocalOffice.ENTITY_NAME), eq(ABBR), any());

    localOffice.setOperation(DELETE);
    service.syncRecord(localOffice);
    verify(requestCacheService).deleteItemFromCache(LocalOffice.ENTITY_NAME, ID);

    service.request(ABBR);
    verify(dataRequestService, times(2)).sendRequest(LocalOffice.SCHEMA_NAME,
        LocalOffice.ENTITY_NAME, whereMap);
  }

  @Test
  void shouldSendRequestWhenRequestedDifferentIds() throws JsonProcessingException {
    service.request(ABBR);
    service.request(ABBR_2);

    verify(dataRequestService, atMostOnce())
        .sendRequest(LocalOffice.SCHEMA_NAME, LocalOffice.ENTITY_NAME, whereMap);
    verify(dataRequestService, atMostOnce())
        .sendRequest(LocalOffice.SCHEMA_NAME, LocalOffice.ENTITY_NAME, whereMap2);
  }

  @Test
  void shouldSendRequestWhenFirstRequestFails() throws JsonProcessingException {
    doThrow(JsonProcessingException.class).when(dataRequestService)
        .sendRequest(anyString(), anyString(), anyMap());

    service.request(ABBR);
    service.request(ABBR);

    verify(dataRequestService, times(2)).sendRequest(LocalOffice.SCHEMA_NAME,
        LocalOffice.ENTITY_NAME, whereMap);
  }

  @Test
  void shouldCatchJsonProcessingExceptionIfThrown() throws JsonProcessingException {
    doThrow(JsonProcessingException.class).when(dataRequestService)
        .sendRequest(anyString(), anyString(), anyMap());
    assertDoesNotThrow(() -> service.request(ABBR));
  }

  @Test
  void shouldThrowAnExceptionIfNotJsonProcessingException() throws JsonProcessingException {
    IllegalStateException illegalStateException = new IllegalStateException("error");
    doThrow(illegalStateException).when(dataRequestService).sendRequest(anyString(), anyString(),
        anyMap());
    assertThrows(IllegalStateException.class, () -> service.request(ABBR));
    assertEquals("error", illegalStateException.getMessage());
  }

  @Test
  void shouldForwardRecordToReferenceSyncService() {
    localOffice.setOperation(Operation.LOAD);
    service.syncRecord(localOffice);

    verify(referenceSyncService).syncRecord(localOffice);
  }
}

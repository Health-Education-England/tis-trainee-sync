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
import uk.nhs.hee.tis.trainee.sync.model.HeeUser;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.repository.HeeUserRepository;

class HeeUserSyncServiceTest {

  private static final String ID = "HeeUserId";
  private static final String ID_2 = "HeeUserId2";

  private HeeUserSyncService service;
  private HeeUserRepository repository;

  private DataRequestService dataRequestService;

  private RequestCacheService requestCacheService;

  private HeeUser heeUser;

  private Map<String, String> whereMap;

  private Map<String, String> whereMap2;

  @BeforeEach
  void setUp() {
    repository = mock(HeeUserRepository.class);
    dataRequestService = mock(DataRequestService.class);
    requestCacheService = mock(RequestCacheService.class);

    service = new HeeUserSyncService(repository, dataRequestService, requestCacheService);

    heeUser = new HeeUser();
    heeUser.setTisId(ID);

    whereMap = Map.of("id", ID);
    whereMap2 = Map.of("id", ID_2);
  }

  @Test
  void shouldThrowExceptionIfRecordNotHeeUser() {
    Record theRecord = new Record();
    assertThrows(IllegalArgumentException.class, () -> service.syncRecord(theRecord));
  }

  @ParameterizedTest(name = "Should store records when operation is {0}.")
  @EnumSource(value = Operation.class, names = {"LOAD", "INSERT", "UPDATE"})
  void shouldStoreRecords(Operation operation) {
    heeUser.setOperation(operation);

    service.syncRecord(heeUser);

    verify(repository).save(heeUser);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldDeleteRecordFromStore() {
    heeUser.setOperation(DELETE);

    service.syncRecord(heeUser);

    verify(repository).deleteById(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldFindRecordByIdWhenExists() {
    when(repository.findById(ID)).thenReturn(Optional.of(heeUser));

    Optional<HeeUser> found = service.findById(ID);
    assertThat("Record not found.", found.isPresent(), is(true));
    assertThat("Unexpected record.", found.orElse(null), sameInstance(heeUser));

    verify(repository).findById(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotFindRecordByIdWhenNotExists() {
    when(repository.findById(ID)).thenReturn(Optional.empty());

    Optional<HeeUser> found = service.findById(ID);
    assertThat("Record not found.", found.isEmpty(), is(true));

    verify(repository).findById(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldFindRecordByUserNameWhenExists() {
    when(repository.findByName(ID)).thenReturn(Optional.of(heeUser));

    Optional<HeeUser> found = service.findByName(ID);
    assertThat("Record not found.", found.isPresent(), is(true));
    assertThat("Unexpected record.", found.orElse(null), sameInstance(heeUser));

    verify(repository).findByName(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotFindRecordByUsrNameWhenNotExists() {
    when(repository.findByName(ID)).thenReturn(Optional.empty());

    Optional<HeeUser> found = service.findByName(ID);
    assertThat("Record not found.", found.isEmpty(), is(true));

    verify(repository).findByName(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldSendRequestWhenNotAlreadyRequested() throws JsonProcessingException {
    when(requestCacheService.isItemInCache(HeeUser.ENTITY_NAME, ID)).thenReturn(false);
    service.request(ID);
    verify(dataRequestService).sendRequest("HeeUser", whereMap);
  }

  @Test
  void shouldNotSendRequestWhenAlreadyRequested() throws JsonProcessingException {
    when(requestCacheService.isItemInCache(HeeUser.ENTITY_NAME, ID)).thenReturn(true);
    service.request(ID);
    verify(dataRequestService, never()).sendRequest("HeeUser", whereMap);
    verifyNoMoreInteractions(dataRequestService);
  }

  @Test
  void shouldSendRequestWhenSyncedBetweenRequests() throws JsonProcessingException {
    when(requestCacheService.isItemInCache(HeeUser.ENTITY_NAME, ID)).thenReturn(false);
    service.request(ID);
    verify(requestCacheService).addItemToCache(eq(HeeUser.ENTITY_NAME), eq(ID), any());

    heeUser.setOperation(DELETE);
    service.syncRecord(heeUser);
    verify(requestCacheService).deleteItemFromCache(HeeUser.ENTITY_NAME, ID);

    service.request(ID);
    verify(dataRequestService, times(2)).sendRequest("HeeUser", whereMap);
  }

  @Test
  void shouldSendRequestWhenRequestedDifferentIds() throws JsonProcessingException {
    service.request(ID);
    service.request(ID_2);

    verify(dataRequestService, atMostOnce()).sendRequest("HeeUser", whereMap);
    verify(dataRequestService, atMostOnce()).sendRequest("HeeUser", whereMap2);
  }

  @Test
  void shouldSendRequestWhenFirstRequestFails() throws JsonProcessingException {
    doThrow(JsonProcessingException.class).when(dataRequestService)
        .sendRequest(anyString(), anyMap());

    service.request(ID);
    service.request(ID);

    verify(dataRequestService, times(2)).sendRequest("HeeUser", whereMap);
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

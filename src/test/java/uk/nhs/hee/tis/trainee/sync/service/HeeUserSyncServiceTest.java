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

  private static final String NAME = "HeeUser";
  private static final String NAME_2 = "HeeUser2";

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
    heeUser.setTisId(NAME);
    heeUser.setData(Map.of("name", NAME));

    whereMap = Map.of("name", NAME);
    whereMap2 = Map.of("name", NAME_2);
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
  void shouldDeleteRecordFromStoreIfExists() {
    when(repository.findByName(NAME)).thenReturn(Optional.of(heeUser));

    HeeUser heeUserFromTis = new HeeUser(); //arrives without ID
    heeUserFromTis.setOperation(DELETE);
    heeUserFromTis.setData(Map.of("name", NAME));
    service.syncRecord(heeUserFromTis);

    verify(repository).findByName(NAME);
    verify(repository).deleteById(NAME);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotDeleteRecordFromStoreIfNotExists() {
    heeUser.setOperation(DELETE);
    when(repository.findByName(NAME)).thenReturn(Optional.empty());

    service.syncRecord(heeUser);

    verify(repository).findByName(NAME);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldFindRecordByNameWhenExists() {
    when(repository.findByName(NAME)).thenReturn(Optional.of(heeUser));

    Optional<HeeUser> found = service.findByName(NAME);
    assertThat("Record not found.", found.isPresent(), is(true));
    assertThat("Unexpected record.", found.orElse(null), sameInstance(heeUser));

    verify(repository).findByName(NAME);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotFindRecordByNameWhenNotExists() {
    when(repository.findByName(NAME)).thenReturn(Optional.empty());

    Optional<HeeUser> found = service.findByName(NAME);
    assertThat("Record not found.", found.isEmpty(), is(true));

    verify(repository).findByName(NAME);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldSendRequestWhenNotAlreadyRequested() throws JsonProcessingException {
    when(requestCacheService.isItemInCache(HeeUser.ENTITY_NAME, NAME)).thenReturn(false);
    service.request(NAME);
    verify(dataRequestService).sendRequest("HeeUser", whereMap);
  }

  @Test
  void shouldNotSendRequestWhenAlreadyRequested() throws JsonProcessingException {
    when(requestCacheService.isItemInCache(HeeUser.ENTITY_NAME, NAME)).thenReturn(true);
    service.request(NAME);
    verify(dataRequestService, never()).sendRequest("HeeUser", whereMap);
    verifyNoMoreInteractions(dataRequestService);
  }

  @Test
  void shouldSendRequestWhenSyncedBetweenRequests() throws JsonProcessingException {
    when(requestCacheService.isItemInCache(HeeUser.ENTITY_NAME, NAME)).thenReturn(false);
    service.request(NAME);
    verify(requestCacheService).addItemToCache(eq(HeeUser.ENTITY_NAME), eq(NAME), any());

    heeUser.setOperation(DELETE);
    service.syncRecord(heeUser);
    verify(requestCacheService).deleteItemFromCache(HeeUser.ENTITY_NAME, NAME);

    service.request(NAME);
    verify(dataRequestService, times(2)).sendRequest("HeeUser", whereMap);
  }

  @Test
  void shouldSendRequestWhenRequestedDifferentIds() throws JsonProcessingException {
    service.request(NAME);
    service.request(NAME_2);

    verify(dataRequestService, atMostOnce()).sendRequest("HeeUser", whereMap);
    verify(dataRequestService, atMostOnce()).sendRequest("HeeUser", whereMap2);
  }

  @Test
  void shouldSendRequestWhenFirstRequestFails() throws JsonProcessingException {
    doThrow(JsonProcessingException.class).when(dataRequestService)
        .sendRequest(anyString(), anyMap());

    service.request(NAME);
    service.request(NAME);

    verify(dataRequestService, times(2)).sendRequest("HeeUser", whereMap);
  }

  @Test
  void shouldCatchJsonProcessingExceptionIfThrown() throws JsonProcessingException {
    doThrow(JsonProcessingException.class).when(dataRequestService)
        .sendRequest(anyString(), anyMap());
    assertDoesNotThrow(() -> service.request(NAME));
  }

  @Test
  void shouldThrowAnExceptionIfNotJsonProcessingException() throws JsonProcessingException {
    IllegalStateException illegalStateException = new IllegalStateException("error");
    doThrow(illegalStateException).when(dataRequestService).sendRequest(anyString(),
        anyMap());
    assertThrows(IllegalStateException.class, () -> service.request(NAME));
    assertEquals("error", illegalStateException.getMessage());
  }
}

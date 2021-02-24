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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.Curriculum;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.repository.CurriculumRepository;

class CurriculumSyncServiceTest {

  private static final String ID = "40";

  private CurriculumSyncService service;

  private CurriculumRepository repository;

  private DataRequestService dataRequestService;

  private Curriculum record;

  @BeforeEach
  void setUp() {
    repository = mock(CurriculumRepository.class);
    dataRequestService = mock(DataRequestService.class);
    service = new CurriculumSyncService(repository, dataRequestService);

    record = new Curriculum();
    record.setTisId(ID);
  }

  @Test
  void shouldThrowExceptionIfRecordNotCurriculum() {
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
  void shouldFindRecordByIdWhenExists() {
    when(repository.findById(ID)).thenReturn(Optional.of(record));

    Optional<Curriculum> found = service.findById(ID);
    assertThat("Record not found.", found.isPresent(), is(true));
    assertThat("Unexpected record.", found.orElse(null), sameInstance(record));

    verify(repository).findById(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotFindRecordByIdWhenNotExists() {
    when(repository.findById(ID)).thenReturn(Optional.empty());

    Optional<Curriculum> found = service.findById(ID);
    assertThat("Record not found.", found.isEmpty(), is(true));

    verify(repository).findById(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldSendRequestWhenNotAlreadyRequested() throws JsonProcessingException {
    service.request(ID);
    verify(dataRequestService).sendRequest("Curriculum", ID);
  }

  @Test
  void shouldNotSendRequestWhenAlreadyRequested() throws JsonProcessingException {
    service.request(ID);
    service.request(ID);
    verify(dataRequestService, atMostOnce()).sendRequest("Curriculum", ID);
    verifyNoMoreInteractions(dataRequestService);
  }

  @Test
  void shouldSendRequestWhenSyncedBetweenRequests() throws JsonProcessingException {
    service.request(ID);

    record.setOperation(DELETE);
    service.syncRecord(record);

    service.request(ID);
    verify(dataRequestService, times(2)).sendRequest("Curriculum", ID);
  }

  @Test
  void shouldSendRequestWhenRequestedDifferentIds() throws JsonProcessingException {
    service.request(ID);
    service.request("140");
    verify(dataRequestService, atMostOnce()).sendRequest("Curriculum", ID);
    verify(dataRequestService, atMostOnce()).sendRequest("Curriculum", "140");
  }

  @Test
  void shouldSendRequestWhenFirstRequestFails() throws JsonProcessingException {
    doThrow(JsonProcessingException.class).when(dataRequestService)
        .sendRequest(anyString(), anyString());

    service.request(ID);
    service.request(ID);

    verify(dataRequestService, times(2)).sendRequest("Curriculum", ID);
  }

  @Test
  void shouldCatchAJsonProcessingExceptionIfThrown() throws JsonProcessingException {
    doThrow(JsonProcessingException.class).when(dataRequestService)
        .sendRequest(anyString(), anyString());
    assertDoesNotThrow(() -> service.request(ID));
  }

  @Test
  void shouldThrowAnExceptionIfNotJsonProcessingException() throws JsonProcessingException {
    IllegalStateException illegalStateException = new IllegalStateException("error");
    doThrow(illegalStateException).when(dataRequestService).sendRequest(anyString(),
        anyString());
    assertThrows(IllegalStateException.class, () -> service.request(ID));
    assertEquals("error", illegalStateException.getMessage());
  }
}

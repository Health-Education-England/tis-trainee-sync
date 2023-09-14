/*
 * The MIT License (MIT)
 *
 * Copyright 2023 Crown Copyright (Health Education England)
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
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.nhs.hee.tis.trainee.sync.model.Operation.DELETE;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import uk.nhs.hee.tis.trainee.sync.mapper.ConditionsOfJoiningMapper;
import uk.nhs.hee.tis.trainee.sync.mapper.ConditionsOfJoiningMapperImpl;
import uk.nhs.hee.tis.trainee.sync.model.ConditionsOfJoining;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.repository.ConditionsOfJoiningRepository;

class ConditionsOfJoiningSyncServiceTest {

  private static final String ID = UUID.randomUUID().toString();
  private static final Instant SIGNED_AT = Instant.now();
  private static final Instant RECEIVED_FROM_TIS = Instant.MIN;
  private static final String VERSION = "GG9";

  private ConditionsOfJoiningSyncService service;
  private ConditionsOfJoiningRepository repository;

  @BeforeEach
  void setUp() {
    repository = mock(ConditionsOfJoiningRepository.class);
    ConditionsOfJoiningMapper mapper = new ConditionsOfJoiningMapperImpl();
    service = new ConditionsOfJoiningSyncService(repository, mapper);
  }

  @Test
  void shouldThrowExceptionIfRecordNotConditionsOfJoining() {
    Record record = new Record();
    assertThrows(IllegalArgumentException.class, () -> service.syncRecord(record));
  }

  @ParameterizedTest(name = "Should store records when operation is {0}.")
  @EnumSource(value = Operation.class, names = {"LOAD", "INSERT", "UPDATE"})
  void shouldStoreRecords(Operation operation) {
    Record conditionsOfJoiningRecord = new Record();
    conditionsOfJoiningRecord.setOperation(operation);
    conditionsOfJoiningRecord.setTable(ConditionsOfJoining.ENTITY_NAME);
    conditionsOfJoiningRecord.setData(Map.of(
        "programmeMembershipUuid", ID,
        "signedAt", SIGNED_AT.toString(),
        "version", VERSION
    ));

    when(repository.findById(anyString())).thenReturn(Optional.empty());

    service.syncRecord(conditionsOfJoiningRecord);

    ArgumentCaptor<ConditionsOfJoining> captor = ArgumentCaptor.forClass(ConditionsOfJoining.class);
    verify(repository).save(captor.capture());

    ConditionsOfJoining conditionsOfJoining = captor.getValue();
    assertThat("Unexpected ID.", conditionsOfJoining.getProgrammeMembershipUuid(), is(ID));
    assertThat("Unexpected Signed at.", conditionsOfJoining.getSignedAt(), is(SIGNED_AT));
    assertThat("Unexpected Version.", conditionsOfJoining.getVersion(), is(VERSION));
    assertThat("Unexpected Received from TIS.", conditionsOfJoining.getReceivedFromTis(),
        nullValue());
  }

  @Test
  void shouldSetExistingValueForReceivedFromTisWhenThisExistsBeforeSaving() {
    Record conditionsOfJoiningRecord = new Record();
    conditionsOfJoiningRecord.setOperation(Operation.LOAD);
    conditionsOfJoiningRecord.setTable(ConditionsOfJoining.ENTITY_NAME);
    conditionsOfJoiningRecord.setData(Map.of(
        "programmeMembershipUuid", ID,
        "signedAt", SIGNED_AT.toString(),
        "version", VERSION
    ));

    ConditionsOfJoining existingCoj = new ConditionsOfJoining();
    existingCoj.setReceivedFromTis(RECEIVED_FROM_TIS);

    when(repository.findById(anyString())).thenReturn(Optional.of(existingCoj));

    service.syncRecord(conditionsOfJoiningRecord);

    ArgumentCaptor<ConditionsOfJoining> captor = ArgumentCaptor.forClass(ConditionsOfJoining.class);
    verify(repository).save(captor.capture());

    ConditionsOfJoining conditionsOfJoining = captor.getValue();
    assertThat("Unexpected ID.", conditionsOfJoining.getProgrammeMembershipUuid(), is(ID));
    assertThat("Unexpected Signed at.", conditionsOfJoining.getSignedAt(), is(SIGNED_AT));
    assertThat("Unexpected Version.", conditionsOfJoining.getVersion(), is(VERSION));
    assertThat("Unexpected Received from TIS.", conditionsOfJoining.getReceivedFromTis(),
        is(RECEIVED_FROM_TIS));
  }

  @Test
  void shouldDeleteRecordFromStore() {
    Record conditionsOfJoiningRecord = new Record();
    conditionsOfJoiningRecord.setOperation(DELETE);
    conditionsOfJoiningRecord.setTable(ConditionsOfJoining.ENTITY_NAME);
    conditionsOfJoiningRecord.setData(Map.of(
        "programmeMembershipUuid", ID
    ));

    service.syncRecord(conditionsOfJoiningRecord);

    verify(repository).deleteById(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldFindConditionsOfJoiningByIdWhenExists() {
    ConditionsOfJoining conditionsOfJoining = new ConditionsOfJoining();
    when(repository.findById(ID)).thenReturn(Optional.of(conditionsOfJoining));

    Optional<ConditionsOfJoining> optionalRecord = service.findById(ID);
    assertThat("Unexpected Conditions of joining found.", optionalRecord.isPresent(), is(true));

    ConditionsOfJoining foundRecord = optionalRecord.get();
    assertThat("Unexpected Conditions of joining.", foundRecord,
        sameInstance(conditionsOfJoining));

    verify(repository).findById(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotFindConditionsOfJoiningByIdWhenNotExists() {
    when(repository.findById(ID)).thenReturn(Optional.empty());

    Optional<ConditionsOfJoining> optionalRecord = service.findById(ID);
    assertThat("Unexpected Conditions of joining found.", optionalRecord.isPresent(), is(false));

    verify(repository).findById(ID);
    verifyNoMoreInteractions(repository);
  }
}

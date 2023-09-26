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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.nhs.hee.tis.trainee.sync.model.Operation.DELETE;
import static uk.nhs.hee.tis.trainee.sync.model.Operation.LOAD;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bson.BsonString;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import uk.nhs.hee.tis.trainee.sync.mapper.ConditionsOfJoiningMapper;
import uk.nhs.hee.tis.trainee.sync.mapper.ConditionsOfJoiningMapperImpl;
import uk.nhs.hee.tis.trainee.sync.model.ConditionsOfJoining;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.ProgrammeMembership;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.repository.ConditionsOfJoiningRepository;

class ConditionsOfJoiningSyncServiceTest {

  private static final String ID = UUID.randomUUID().toString();
  private static final Instant SIGNED_AT = Instant.now();
  private static final Instant SYNCED_AT = Instant.MIN;
  private static final String VERSION = "GG9";

  private ConditionsOfJoiningSyncService service;
  private ConditionsOfJoiningRepository repository;
  private ProgrammeMembershipSyncService programmeMembershipSyncService;

  private ConditionsOfJoiningMapper mapper;
  private ApplicationEventPublisher eventPublisher;

  @BeforeEach
  void setUp() {
    repository = mock(ConditionsOfJoiningRepository.class);
    mapper = new ConditionsOfJoiningMapperImpl();
    eventPublisher = mock(ApplicationEventPublisher.class);
    programmeMembershipSyncService = mock(ProgrammeMembershipSyncService.class);
    service = new ConditionsOfJoiningSyncService(repository, programmeMembershipSyncService,
         mapper, eventPublisher);
  }

  @Test
  void shouldThrowExceptionIfRecordNotConditionsOfJoining() {
    Record record = new Record();
    assertThrows(IllegalArgumentException.class, () -> service.syncRecord(record));
  }

  @ParameterizedTest(name = "Should store record and broadcast event when operation is {0}.")
  @EnumSource(value = Operation.class, names = {"LOAD", "INSERT", "UPDATE"})
  void shouldStoreRecordsAndBroadcastEvent(Operation operation) {
    Record conditionsOfJoiningRecord = new Record();
    conditionsOfJoiningRecord.setOperation(operation);
    conditionsOfJoiningRecord.setTable(ConditionsOfJoining.ENTITY_NAME);
    conditionsOfJoiningRecord.setData(Map.of(
        "programmeMembershipUuid", ID,
        "signedAt", SIGNED_AT.toString(),
        "version", VERSION
    ));

    when(repository.findById(anyString())).thenReturn(Optional.empty());
    when(repository.save(any())).thenReturn(new ConditionsOfJoining());

    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    programmeMembership.setProgrammeId(123L);
    when(programmeMembershipSyncService.findById(any())).thenReturn(
        Optional.of(programmeMembership));

    service.syncRecord(conditionsOfJoiningRecord);

    ArgumentCaptor<ConditionsOfJoining> captor = ArgumentCaptor.forClass(ConditionsOfJoining.class);
    verify(repository).save(captor.capture());

    ConditionsOfJoining conditionsOfJoining = captor.getValue();
    assertThat("Unexpected ID.", conditionsOfJoining.getProgrammeMembershipUuid(), is(ID));
    assertThat("Unexpected Signed at.", conditionsOfJoining.getSignedAt(), is(SIGNED_AT));
    assertThat("Unexpected Version.", conditionsOfJoining.getVersion(), is(VERSION));
    assertThat("Unexpected Synced at.", conditionsOfJoining.getSyncedAt(),
        nullValue());

    verify(eventPublisher).publishEvent(any());
  }

  @Test
  void shouldSetExistingValueForSyncedAtWhenThisExistsBeforeSaving() {
    Record conditionsOfJoiningRecord = new Record();
    conditionsOfJoiningRecord.setOperation(Operation.LOAD);
    conditionsOfJoiningRecord.setTable(ConditionsOfJoining.ENTITY_NAME);
    conditionsOfJoiningRecord.setData(Map.of(
        "programmeMembershipUuid", ID,
        "signedAt", SIGNED_AT.toString(),
        "version", VERSION
    ));

    ConditionsOfJoining existingCoj = new ConditionsOfJoining();
    existingCoj.setSyncedAt(SYNCED_AT);

    when(repository.findById(anyString())).thenReturn(Optional.of(existingCoj));
    when(repository.save(any())).thenReturn(new ConditionsOfJoining());

    service.syncRecord(conditionsOfJoiningRecord);

    ArgumentCaptor<ConditionsOfJoining> captor = ArgumentCaptor.forClass(ConditionsOfJoining.class);
    verify(repository).save(captor.capture());

    ConditionsOfJoining conditionsOfJoining = captor.getValue();
    assertThat("Unexpected ID.", conditionsOfJoining.getProgrammeMembershipUuid(), is(ID));
    assertThat("Unexpected Signed at.", conditionsOfJoining.getSignedAt(), is(SIGNED_AT));
    assertThat("Unexpected Version.", conditionsOfJoining.getVersion(), is(VERSION));
    assertThat("Unexpected Synced at.", conditionsOfJoining.getSyncedAt(),
        is(SYNCED_AT));
  }

  @Test
  void shouldTriggerProgrammeMembershipAfterSavedEvent() {
    ConditionsOfJoining conditionsOfJoining = new ConditionsOfJoining();
    conditionsOfJoining.setProgrammeMembershipUuid(ID);
    conditionsOfJoining.setVersion(VERSION);
    conditionsOfJoining.setSignedAt(SIGNED_AT);
    conditionsOfJoining.setSyncedAt(SYNCED_AT);

    Record cojRecord = mapper.toRecord(conditionsOfJoining);
    cojRecord.setOperation(Operation.LOAD);
    cojRecord.setTable(ConditionsOfJoining.ENTITY_NAME);

    when(repository.findById(anyString())).thenReturn(Optional.empty());
    when(repository.save(any())).thenReturn(conditionsOfJoining);

    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    when(programmeMembershipSyncService.findById(any())).thenReturn(
        Optional.of(programmeMembership));

    service.syncRecord(cojRecord);

    ArgumentCaptor<AfterSaveEvent<ProgrammeMembership>> broadcastCaptor
        = ArgumentCaptor.forClass(AfterSaveEvent.class);

    verify(eventPublisher).publishEvent(broadcastCaptor.capture());

    Document routingDoc = new Document();
    routingDoc.append("event_type", new BsonString("COJ_RECEIVED"));

    AfterSaveEvent<ProgrammeMembership> broadcastRecord = broadcastCaptor.getValue();
    assertThat("Unexpected event source.", broadcastRecord.getSource(),
        is(programmeMembership));
    assertThat("Unexpected event document.", broadcastRecord.getDocument().toString(),
        is(routingDoc.toString()));
    assertThat("Unexpected event collection.", broadcastRecord.getCollectionName(),
        is(ProgrammeMembership.ENTITY_NAME));
  }

  @Test
  void shouldNotTriggerAfterSaveEventIfProgrammeMembershipCouldNotBeFound() {
    Record conditionsOfJoiningRecord = new Record();
    conditionsOfJoiningRecord.setOperation(LOAD);
    conditionsOfJoiningRecord.setTable(ConditionsOfJoining.ENTITY_NAME);
    conditionsOfJoiningRecord.setData(Map.of(
        "programmeMembershipUuid", ID,
        "signedAt", SIGNED_AT.toString(),
        "version", VERSION
    ));

    when(repository.findById(anyString())).thenReturn(Optional.empty());
    when(repository.save(any())).thenReturn(new ConditionsOfJoining());
    when(programmeMembershipSyncService.findById(any())).thenReturn(
        Optional.empty());

    service.syncRecord(conditionsOfJoiningRecord);

    verify(eventPublisher, never()).publishEvent(any());
    verifyNoInteractions(eventPublisher);
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

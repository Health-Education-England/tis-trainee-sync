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

package uk.nhs.hee.tis.trainee.sync.event;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.AssertionErrors.assertNotNull;

import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeDeleteEvent;
import uk.nhs.hee.tis.trainee.sync.mapper.ProgrammeMembershipMapper;
import uk.nhs.hee.tis.trainee.sync.mapper.ProgrammeMembershipMapperImpl;
import uk.nhs.hee.tis.trainee.sync.model.ConditionsOfJoining;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.ProgrammeMembership;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.service.ConditionsOfJoiningSyncService;
import uk.nhs.hee.tis.trainee.sync.service.ProgrammeMembershipSyncService;

class ConditionsOfJoiningEventListenerTest {

  private static final String PROGRAMME_MEMBERSHIP_QUEUE_URL = "https://queue.programmemembership";

  private ConditionsOfJoiningEventListener listener;
  private ProgrammeMembershipSyncService programmeMembershipSyncService;
  private ConditionsOfJoiningSyncService conditionsOfJoiningSyncService;
  private ProgrammeMembershipMapper programmeMembershipMapper;
  private QueueMessagingTemplate messagingTemplate;
  private Cache cache;

  @BeforeEach
  void setUp() {
    conditionsOfJoiningSyncService = mock(ConditionsOfJoiningSyncService.class);
    programmeMembershipSyncService = mock(ProgrammeMembershipSyncService.class);
    programmeMembershipMapper = new ProgrammeMembershipMapperImpl();
    messagingTemplate = mock(QueueMessagingTemplate.class);

    CacheManager cacheManager = mock(CacheManager.class);
    cache = mock(Cache.class);
    when(cacheManager.getCache(anyString())).thenReturn(cache);

    listener = new ConditionsOfJoiningEventListener(conditionsOfJoiningSyncService,
        programmeMembershipSyncService, programmeMembershipMapper, cacheManager, messagingTemplate,
        PROGRAMME_MEMBERSHIP_QUEUE_URL);
  }

  @Test
  void shouldRequestProgrammeMembershipWhenMissingAfterSave() {
    UUID pmUuid = UUID.randomUUID();
    ConditionsOfJoining conditionsOfJoining = new ConditionsOfJoining();
    conditionsOfJoining.setProgrammeMembershipUuid(pmUuid.toString());
    AfterSaveEvent<ConditionsOfJoining> event = new AfterSaveEvent<>(conditionsOfJoining,
        null, null);

    when(programmeMembershipSyncService.findById(pmUuid.toString())).thenReturn(Optional.empty());

    listener.onAfterSave(event);

    verify(programmeMembershipSyncService).request(pmUuid);
    verifyNoInteractions(messagingTemplate);
  }

  @Test
  void shouldQueueProgrammeMembershipWhenFoundAfterSave() {
    UUID pmUuid = UUID.randomUUID();

    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    programmeMembership.setUuid(pmUuid);
    Record programmeMembershipRecord = programmeMembershipMapper.toRecord(programmeMembership);
    programmeMembershipRecord.setOperation(Operation.LOOKUP);

    ConditionsOfJoining conditionsOfJoining = new ConditionsOfJoining();
    conditionsOfJoining.setProgrammeMembershipUuid(pmUuid.toString());
    AfterSaveEvent<ConditionsOfJoining> event = new AfterSaveEvent<>(conditionsOfJoining,
        null, null);

    when(programmeMembershipSyncService.findById(pmUuid.toString()))
        .thenReturn(Optional.of(programmeMembership));

    listener.onAfterSave(event);

    verify(programmeMembershipSyncService, never()).request(any());
    verify(messagingTemplate)
        .convertAndSend(PROGRAMME_MEMBERSHIP_QUEUE_URL, programmeMembershipRecord);

    assertThat("Unexpected operation.", programmeMembershipRecord.getOperation(),
        is(Operation.LOOKUP));
  }

  @Test
  void shouldFindAndCacheConditionsOfJoiningIfNotInCacheBeforeDelete() {
    String pmUuidString = UUID.randomUUID().toString();
    ConditionsOfJoining conditionsOfJoining = new ConditionsOfJoining();
    conditionsOfJoining.setProgrammeMembershipUuid(pmUuidString);

    when(cache.get(pmUuidString, ConditionsOfJoining.class)).thenReturn(null);
    when(conditionsOfJoiningSyncService.findById(any()))
        .thenReturn(Optional.of(conditionsOfJoining));

    Document document = new Document();
    document.append("_id", pmUuidString);
    BeforeDeleteEvent<ConditionsOfJoining> event = new BeforeDeleteEvent<>(document, null, null);

    listener.onBeforeDelete(event);

    verify(conditionsOfJoiningSyncService).findById(pmUuidString);
    verify(cache).put(pmUuidString, conditionsOfJoining);
    verifyNoInteractions(messagingTemplate);
  }

  @Test
  void shouldNotFindAndCacheConditionsOfJoiningIfInCacheBeforeDelete() {
    String pmUuidString = UUID.randomUUID().toString();
    Document document = new Document();
    document.append("_id", pmUuidString);
    BeforeDeleteEvent<ConditionsOfJoining> event = new BeforeDeleteEvent<>(document, null, null);

    ConditionsOfJoining conditionsOfJoining = new ConditionsOfJoining();

    when(cache.get(pmUuidString, ConditionsOfJoining.class)).thenReturn(conditionsOfJoining);

    listener.onBeforeDelete(event);

    verifyNoInteractions(conditionsOfJoiningSyncService);
    verifyNoInteractions(messagingTemplate);
  }

  @Test
  void shouldNotQueueRelatedProgrammeMembershipWhenConditionsOfJoiningNotInCacheAfterDelete() {
    String pmUuidString = UUID.randomUUID().toString();
    Document document = new Document();
    document.append("_id", pmUuidString);
    AfterDeleteEvent<ConditionsOfJoining> event = new AfterDeleteEvent<>(document, null, null);

    when(cache.get(pmUuidString, ConditionsOfJoining.class)).thenReturn(null);

    listener.onAfterDelete(event);

    verifyNoInteractions(messagingTemplate);
  }

  @Test
  void shouldNotQueueRelatedProgrammeMembershipWhenProgrammeMembershipNotFoundAfterDelete() {
    String pmUuidString = UUID.randomUUID().toString();
    ConditionsOfJoining conditionsOfJoining = new ConditionsOfJoining();
    conditionsOfJoining.setProgrammeMembershipUuid(pmUuidString);

    when(cache.get(pmUuidString, ConditionsOfJoining.class)).thenReturn(conditionsOfJoining);
    when(programmeMembershipSyncService.findById(pmUuidString)).thenReturn(Optional.empty());

    Document document = new Document();
    document.append("_id", pmUuidString);
    AfterDeleteEvent<ConditionsOfJoining> event = new AfterDeleteEvent<>(document, null, null);

    listener.onAfterDelete(event);

    verifyNoInteractions(messagingTemplate);
  }

  @Test
  void shouldQueueRelatedProgrammeMembershipWhenProgrammeMembershipFoundAfterDelete() {
    UUID pmUuid = UUID.randomUUID();
    ConditionsOfJoining conditionsOfJoining = new ConditionsOfJoining();
    conditionsOfJoining.setProgrammeMembershipUuid(pmUuid.toString());

    when(cache.get(pmUuid.toString(), ConditionsOfJoining.class)).thenReturn(conditionsOfJoining);

    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    programmeMembership.setUuid(pmUuid);
    Record programmeMembershipRecord = programmeMembershipMapper.toRecord(programmeMembership);
    programmeMembershipRecord.setOperation(Operation.LOOKUP);

    when(programmeMembershipSyncService.findById(pmUuid.toString()))
        .thenReturn(Optional.of(programmeMembership));

    Document document = new Document();
    document.append("_id", pmUuid.toString());
    AfterDeleteEvent<ConditionsOfJoining> event = new AfterDeleteEvent<>(document, null, null);

    listener.onAfterDelete(event);

    verify(messagingTemplate)
        .convertAndSend(PROGRAMME_MEMBERSHIP_QUEUE_URL, programmeMembershipRecord);

    assertThat("Unexpected operation.", programmeMembershipRecord.getOperation(),
        is(Operation.LOOKUP));
  }

  @Test
  void shouldSetReceivedFromTisValueIfNullBeforeConvertingAndSaving() {
    String pmUuidString = UUID.randomUUID().toString();

    ConditionsOfJoining conditionsOfJoining = new ConditionsOfJoining();
    conditionsOfJoining.setProgrammeMembershipUuid(pmUuidString);

    BeforeConvertEvent<ConditionsOfJoining> event
        = new BeforeConvertEvent<>(conditionsOfJoining, null);

    listener.onBeforeConvert(event);

    assertNotNull("Missing Received from TIS", event.getSource().getReceivedFromTis());
  }

  @Test
  void shouldNotOverwriteReceivedFromTisValueIfNotNullBeforeConvertingAndSaving() {
    String pmUuidString = UUID.randomUUID().toString();
    Instant cojReceivedFromTis = Instant.now();

    ConditionsOfJoining conditionsOfJoining = new ConditionsOfJoining();
    conditionsOfJoining.setProgrammeMembershipUuid(pmUuidString);
    conditionsOfJoining.setReceivedFromTis(cojReceivedFromTis);

    BeforeConvertEvent<ConditionsOfJoining> event
        = new BeforeConvertEvent<>(conditionsOfJoining, null);

    listener.onBeforeConvert(event);

    assertThat("Unexpected Received from TIS", event.getSource().getReceivedFromTis(),
        is(cojReceivedFromTis));
  }
}

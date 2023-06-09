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

package uk.nhs.hee.tis.trainee.sync.event;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeDeleteEvent;
import uk.nhs.hee.tis.trainee.sync.mapper.ProgrammeMembershipMapperImpl;
import uk.nhs.hee.tis.trainee.sync.model.CurriculumMembership;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.ProgrammeMembership;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.service.CurriculumMembershipSyncService;
import uk.nhs.hee.tis.trainee.sync.service.ProgrammeMembershipSyncService;

class CurriculumMembershipEventListenerTest {

  private static final String PROGRAMME_MEMBERSHIP_QUEUE_URL = "https://queue.programme-membership";

  private CurriculumMembershipEventListener listener;

  private CurriculumMembershipSyncService curriculumMembershipSyncService;

  private ProgrammeMembershipSyncService programmeMembershipService;

  private Cache cache;
  private QueueMessagingTemplate messagingTemplate;

  @BeforeEach
  void setUp() {
    curriculumMembershipSyncService = mock(CurriculumMembershipSyncService.class);
    programmeMembershipService = mock(ProgrammeMembershipSyncService.class);
    CacheManager cacheManager = mock(CacheManager.class);
    cache = mock(Cache.class);
    messagingTemplate = mock(QueueMessagingTemplate.class);

    when(cacheManager.getCache(anyString())).thenReturn(cache);
    listener = new CurriculumMembershipEventListener(curriculumMembershipSyncService,
        programmeMembershipService, new ProgrammeMembershipMapperImpl(), cacheManager,
        messagingTemplate, PROGRAMME_MEMBERSHIP_QUEUE_URL);
  }

  @Test
  void shouldRequestProgrammeMembershipAfterSaveWhenNoRelatedProgrammeMembership() {
    String programmeMembershipUuid = UUID.randomUUID().toString();

    CurriculumMembership curriculumMembership = new CurriculumMembership();
    curriculumMembership.setTisId("cm1");
    curriculumMembership.setData(Map.of("programmeMembershipUuid", programmeMembershipUuid));
    AfterSaveEvent<CurriculumMembership> event = new AfterSaveEvent<>(curriculumMembership, null,
        null);

    when(programmeMembershipService.findById(programmeMembershipUuid)).thenReturn(Optional.empty());

    listener.onAfterSave(event);

    verify(programmeMembershipService).request(UUID.fromString(programmeMembershipUuid));

    verifyNoInteractions(messagingTemplate);
  }

  @Test
  void shouldSendRelatedProgrammeMembershipsToQueueAfterSaveWhenRelatedProgrammeMembership() {
    UUID programmeMembershipUuid = UUID.randomUUID();
    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    programmeMembership.setUuid(programmeMembershipUuid);
    programmeMembership.setProgrammeMembershipType("type1");
    programmeMembership.setProgrammeStartDate(LocalDate.MIN);
    programmeMembership.setProgrammeEndDate(LocalDate.MAX);
    programmeMembership.setProgrammeId(1L);
    programmeMembership.setTrainingNumberId(2L);
    programmeMembership.setPersonId(3L);
    programmeMembership.setRotation("rotation1");
    programmeMembership.setRotationId(4L);
    programmeMembership.setTrainingPathway("trainingPathway1");
    programmeMembership.setLeavingReason("leavingReason1");
    programmeMembership.setLeavingDestination("leavingDestination1");
    programmeMembership.setAmendedDate(Instant.EPOCH);

    CurriculumMembership curriculumMembership = new CurriculumMembership();
    curriculumMembership.setTisId("cm1");
    curriculumMembership.setData(
        Map.of("programmeMembershipUuid", programmeMembershipUuid.toString()));

    when(programmeMembershipService.findById(programmeMembershipUuid.toString())).thenReturn(
        Optional.of(programmeMembership));

    AfterSaveEvent<CurriculumMembership> event = new AfterSaveEvent<>(curriculumMembership, null,
        null);
    listener.onAfterSave(event);

    ArgumentCaptor<Record> recordCaptor = ArgumentCaptor.forClass(Record.class);
    verify(messagingTemplate).convertAndSend(ArgumentMatchers.eq(PROGRAMME_MEMBERSHIP_QUEUE_URL),
        recordCaptor.capture());

    Record record = recordCaptor.getValue();
    assertThat("Unexpected TIS ID.", record.getTisId(), is(programmeMembershipUuid.toString()));
    assertThat("Unexpected table operation.", record.getOperation(), is(Operation.LOAD));

    Map<String, String> data = record.getData();
    assertThat("Unexpected date count.", data.size(), is(13));
    assertThat("Unexpected UUID.", data.get("uuid"), is(programmeMembershipUuid.toString()));
    assertThat("Unexpected PM type.", data.get("programmeMembershipType"), is("type1"));
    assertThat("Unexpected programme start date.", data.get("programmeStartDate"),
        is(LocalDate.MIN.toString()));
    assertThat("Unexpected programme end date.", data.get("programmeEndDate"),
        is(LocalDate.MAX.toString()));
    assertThat("Unexpected programme ID.", data.get("programmeId"), is("1"));
    assertThat("Unexpected training number ID.", data.get("trainingNumberId"), is("2"));
    assertThat("Unexpected person ID.", data.get("personId"), is("3"));
    assertThat("Unexpected rotation.", data.get("rotation"), is("rotation1"));
    assertThat("Unexpected rotation ID.", data.get("rotationId"), is("4"));
    assertThat("Unexpected training pathway.", data.get("trainingPathway"), is("trainingPathway1"));
    assertThat("Unexpected leaving reason.", data.get("leavingReason"), is("leavingReason1"));
    assertThat("Unexpected leaving destination.", data.get("leavingDestination"),
        is("leavingDestination1"));
    assertThat("Unexpected amended date.", data.get("amendedDate"), is(Instant.EPOCH.toString()));

    verify(programmeMembershipService, never()).request(any());
  }

  @Test
  void shouldFindAndCacheProgrammeMembershipIfNotInCacheBeforeDelete() {
    Document document = new Document();
    document.append("_id", "1");
    CurriculumMembership curriculumMembership = new CurriculumMembership();
    BeforeDeleteEvent<CurriculumMembership> event = new BeforeDeleteEvent<>(document, null, null);

    when(cache.get("1", CurriculumMembership.class)).thenReturn(null);
    when(curriculumMembershipSyncService.findById(anyString())).thenReturn(
        Optional.of(curriculumMembership));

    listener.onBeforeDelete(event);

    verify(curriculumMembershipSyncService).findById("1");
    verify(cache).put("1", curriculumMembership);
    verifyNoInteractions(programmeMembershipService);
  }

  @Test
  void shouldNotFindAndCacheProgrammeMembershipIfInCacheBeforeDelete() {
    Document document = new Document();
    document.append("_id", "1");
    CurriculumMembership curriculumMembership = new CurriculumMembership();
    BeforeDeleteEvent<CurriculumMembership> event = new BeforeDeleteEvent<>(document, null, null);

    when(cache.get("1", CurriculumMembership.class)).thenReturn(curriculumMembership);

    listener.onBeforeDelete(event);

    verifyNoInteractions(curriculumMembershipSyncService);
    verifyNoInteractions(programmeMembershipService);
  }

  @Test
  void shouldQueueProgrammeMembershipAfterDelete() {
    Document document = new Document();
    document.append("_id", "1");
    CurriculumMembership curriculumMembership = new CurriculumMembership();
    String uuid = UUID.randomUUID().toString();
    curriculumMembership.getData().put("programmeMembershipUuid", uuid);
    AfterDeleteEvent<CurriculumMembership> eventAfter
        = new AfterDeleteEvent<>(document, null, null);

    when(cache.get("1", CurriculumMembership.class)).thenReturn(curriculumMembership);

    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    programmeMembership.setUuid(UUID.fromString(uuid));
    when(programmeMembershipService.findById(uuid)).thenReturn(Optional.of(programmeMembership));

    listener.onAfterDelete(eventAfter);

    ArgumentCaptor<Record> recordCaptor = ArgumentCaptor.forClass(Record.class);
    verify(messagingTemplate).convertAndSend(eq(PROGRAMME_MEMBERSHIP_QUEUE_URL),
        recordCaptor.capture());

    Record record = recordCaptor.getValue();
    assertThat("Unexpected request UUID", record.getTisId(), is(uuid));
  }

  @Test
  void shouldNotQueueProgrammeMembershipIfNoCurriculumMembership() {
    Document document = new Document();
    document.append("_id", "1");
    AfterDeleteEvent<CurriculumMembership> eventAfter
        = new AfterDeleteEvent<>(document, null, null);

    when(cache.get("1", CurriculumMembership.class)).thenReturn(null);

    listener.onAfterDelete(eventAfter);

    verifyNoInteractions(messagingTemplate);
  }

  @Test
  void shouldNotQueueProgrammeMembershipIfNoProgrammeMembership() {
    Document document = new Document();
    document.append("_id", "1");
    CurriculumMembership curriculumMembership = new CurriculumMembership();
    String uuid = UUID.randomUUID().toString();
    curriculumMembership.getData().put("programmeMembershipUuid", uuid);
    AfterDeleteEvent<CurriculumMembership> eventAfter
        = new AfterDeleteEvent<>(document, null, null);

    when(cache.get("1", CurriculumMembership.class)).thenReturn(curriculumMembership);

    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    programmeMembership.setUuid(UUID.fromString(uuid));
    when(programmeMembershipService.findById(uuid)).thenReturn(Optional.empty());

    listener.onAfterDelete(eventAfter);

    verifyNoInteractions(messagingTemplate);
  }

  @Test
  void shouldNotRequestMissingProgrammeMembershipAfterDelete() {
    Document document = new Document();
    document.append("_id", "1");
    CurriculumMembership curriculumMembership = new CurriculumMembership();
    String uuid = UUID.randomUUID().toString();
    curriculumMembership.getData().put("programmeMembershipUuid", uuid);
    AfterDeleteEvent<CurriculumMembership> eventAfter
        = new AfterDeleteEvent<>(document, null, null);

    when(cache.get("1", CurriculumMembership.class)).thenReturn(curriculumMembership);

    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    programmeMembership.setUuid(UUID.fromString(uuid));
    when(programmeMembershipService.findById(uuid)).thenReturn(Optional.empty());

    listener.onAfterDelete(eventAfter);

    verify(programmeMembershipService, never()).request(any());
  }
}

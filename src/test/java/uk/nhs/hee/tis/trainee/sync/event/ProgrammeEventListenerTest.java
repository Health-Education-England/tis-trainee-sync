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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import uk.nhs.hee.tis.trainee.sync.mapper.ProgrammeMembershipMapperImpl;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.Programme;
import uk.nhs.hee.tis.trainee.sync.model.ProgrammeMembership;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.service.ProgrammeMembershipSyncService;

class ProgrammeEventListenerTest {

  private static final String PROGRAMME_MEMBERSHIP_QUEUE_URL = "https://queue.programme-membership";

  private static final String PROGRAMME_ID = String.valueOf(new Random().nextLong());
  private static final String CURRICULUM_MEMBERSHIP_1_ID = UUID.randomUUID().toString();
  private static final String CURRICULUM_MEMBERSHIP_2_ID = UUID.randomUUID().toString();

  private ProgrammeEventListener listener;
  private ProgrammeMembershipSyncService programmeMembershipService;
  private QueueMessagingTemplate messagingTemplate;

  @BeforeEach
  void setUp() {
    programmeMembershipService = mock(ProgrammeMembershipSyncService.class);
    messagingTemplate = mock(QueueMessagingTemplate.class);
    listener = new ProgrammeEventListener(programmeMembershipService,
        new ProgrammeMembershipMapperImpl(), messagingTemplate, PROGRAMME_MEMBERSHIP_QUEUE_URL);
  }

  @Test
  void shouldNotQueueProgrammeMembershipAfterSaveWhenNoRelatedProgrammeMemberships() {
    Programme programme = new Programme();
    programme.setTisId(PROGRAMME_ID);
    AfterSaveEvent<Programme> event = new AfterSaveEvent<>(programme, null, null);

    when(programmeMembershipService.findByProgrammeId(PROGRAMME_ID)).thenReturn(
        Collections.emptySet());

    listener.onAfterSave(event);

    verifyNoInteractions(messagingTemplate);
  }

  @Test
  void shouldQueueProgrammeMembershipAfterSaveWhenSingleRelatedProgrammeMembership() {
    Programme programme = new Programme();
    programme.setTisId(PROGRAMME_ID);

    UUID programmeMembershipUuid = UUID.randomUUID();
    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    programmeMembership.setUuid(programmeMembershipUuid);
    programmeMembership.setProgrammeMembershipType("type1");
    programmeMembership.setProgrammeStartDate(LocalDate.MIN);
    programmeMembership.setProgrammeEndDate(LocalDate.MAX);
    programmeMembership.setProgrammeId(Long.parseLong(PROGRAMME_ID));
    programmeMembership.setTrainingNumberId(2L);
    programmeMembership.setPersonId(3L);
    programmeMembership.setRotation("rotation1");
    programmeMembership.setRotationId(4L);
    programmeMembership.setTrainingPathway("trainingPathway1");
    programmeMembership.setLeavingReason("leavingReason1");
    programmeMembership.setLeavingDestination("leavingDestination1");
    programmeMembership.setAmendedDate(Instant.EPOCH);

    when(programmeMembershipService.findByProgrammeId(PROGRAMME_ID)).thenReturn(
        Set.of(programmeMembership));

    AfterSaveEvent<Programme> event = new AfterSaveEvent<>(programme, null, null);
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
    assertThat("Unexpected programme ID.", data.get("programmeId"), is(PROGRAMME_ID));
    assertThat("Unexpected training number ID.", data.get("trainingNumberId"), is("2"));
    assertThat("Unexpected person ID.", data.get("personId"), is("3"));
    assertThat("Unexpected rotation.", data.get("rotation"), is("rotation1"));
    assertThat("Unexpected rotation ID.", data.get("rotationId"), is("4"));
    assertThat("Unexpected training pathway.", data.get("trainingPathway"), is("trainingPathway1"));
    assertThat("Unexpected leaving reason.", data.get("leavingReason"), is("leavingReason1"));
    assertThat("Unexpected leaving destination.", data.get("leavingDestination"),
        is("leavingDestination1"));
    assertThat("Unexpected amended date.", data.get("amendedDate"), is(Instant.EPOCH.toString()));
  }

  @Test
  void shouldQueueProgrammeMembershipsAfterSaveWhenMultipleRelatedProgrammeMemberships() {
    Programme programme = new Programme();
    programme.setTisId(PROGRAMME_ID);

    UUID programmeMembershipUuid1 = UUID.randomUUID();
    ProgrammeMembership programmeMembership1 = new ProgrammeMembership();
    programmeMembership1.setUuid(programmeMembershipUuid1);
    programmeMembership1.setProgrammeId(Long.parseLong(PROGRAMME_ID));

    UUID programmeMembershipUuid2 = UUID.randomUUID();
    ProgrammeMembership programmeMembership2 = new ProgrammeMembership();
    programmeMembership2.setUuid(programmeMembershipUuid2);
    programmeMembership2.setProgrammeId(Long.parseLong(PROGRAMME_ID));

    when(programmeMembershipService.findByProgrammeId(PROGRAMME_ID)).thenReturn(
        Set.of(programmeMembership1, programmeMembership2));

    AfterSaveEvent<Programme> event = new AfterSaveEvent<>(programme, null, null);
    listener.onAfterSave(event);

    ArgumentCaptor<Record> recordCaptor = ArgumentCaptor.forClass(Record.class);
    verify(messagingTemplate, times(2)).convertAndSend(
        ArgumentMatchers.eq(PROGRAMME_MEMBERSHIP_QUEUE_URL),
        recordCaptor.capture());

    List<Record> records = recordCaptor.getAllValues();
    assertThat("Unexpected record count.", records.size(), is(2));

    Record record = records.stream()
        .filter(r -> r.getTisId().equals(programmeMembershipUuid1.toString())).findFirst()
        .orElse(null);
    assertThat("Unexpected TIS ID.", record.getTisId(), is(programmeMembershipUuid1.toString()));
    assertThat("Unexpected table operation.", record.getOperation(), is(Operation.LOAD));

    Map<String, String> data = record.getData();
    assertThat("Unexpected UUID.", data.get("uuid"), is(programmeMembershipUuid1.toString()));
    assertThat("Unexpected programme ID.", data.get("programmeId"), is(PROGRAMME_ID));

    record = records.stream().filter(r -> r.getTisId().equals(programmeMembershipUuid2.toString()))
        .findFirst().orElse(null);
    assertThat("Unexpected TIS ID.", record.getTisId(), is(programmeMembershipUuid2.toString()));
    assertThat("Unexpected table operation.", record.getOperation(), is(Operation.LOAD));

    data = record.getData();
    assertThat("Unexpected UUID.", data.get("uuid"), is(programmeMembershipUuid2.toString()));
    assertThat("Unexpected programme ID.", data.get("programmeId"), is(PROGRAMME_ID));
  }
}

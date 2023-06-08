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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import uk.nhs.hee.tis.trainee.sync.model.CurriculumMembership;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.Programme;
import uk.nhs.hee.tis.trainee.sync.service.CurriculumMembershipSyncService;

class ProgrammeEventListenerTest {

  private static final String CURRICULUM_MEMBERSHIP_QUEUE_URL = "https://queue.curriculum-membership";

  private static final String PROGRAMME_ID = UUID.randomUUID().toString();
  private static final String CURRICULUM_MEMBERSHIP_1_ID = UUID.randomUUID().toString();
  private static final String CURRICULUM_MEMBERSHIP_2_ID = UUID.randomUUID().toString();

  private ProgrammeEventListener listener;
  private CurriculumMembershipSyncService curriculumMembershipService;
  private QueueMessagingTemplate messagingTemplate;

  @BeforeEach
  void setUp() {
    curriculumMembershipService = mock(CurriculumMembershipSyncService.class);
    messagingTemplate = mock(QueueMessagingTemplate.class);
    listener = new ProgrammeEventListener(curriculumMembershipService, messagingTemplate,
        CURRICULUM_MEMBERSHIP_QUEUE_URL);
  }

  @Test
  void shouldNotInteractWithCurriculumMembershipQueueAfterSaveWhenNoRelatedCurriculumMemberships() {
    Programme programme = new Programme();
    programme.setTisId(PROGRAMME_ID);
    AfterSaveEvent<Programme> event = new AfterSaveEvent<>(programme, null, null);

    when(curriculumMembershipService.findByProgrammeId(PROGRAMME_ID)).thenReturn(
        Collections.emptySet());

    listener.onAfterSave(event);

    verifyNoInteractions(messagingTemplate);
  }

  @Test
  void shouldSendRelatedCurriculumMembershipsToQueueAfterSaveWhenRelatedCurriculumMemberships() {
    Programme programme = new Programme();
    programme.setTisId(PROGRAMME_ID);

    CurriculumMembership curriculumMembership1 = new CurriculumMembership();
    curriculumMembership1.setTisId(CURRICULUM_MEMBERSHIP_1_ID);

    CurriculumMembership curriculumMembership2 = new CurriculumMembership();
    curriculumMembership2.setTisId(CURRICULUM_MEMBERSHIP_2_ID);
    when(curriculumMembershipService.findByProgrammeId(PROGRAMME_ID)).thenReturn(
        Set.of(curriculumMembership1, curriculumMembership2));

    AfterSaveEvent<Programme> event = new AfterSaveEvent<>(programme, null, null);
    listener.onAfterSave(event);

    verify(messagingTemplate).convertAndSend(CURRICULUM_MEMBERSHIP_QUEUE_URL,
        curriculumMembership1);
    assertThat("Unexpected table operation.", curriculumMembership1.getOperation(),
        is(Operation.LOAD));

    verify(messagingTemplate).convertAndSend(CURRICULUM_MEMBERSHIP_QUEUE_URL,
        curriculumMembership2);
    assertThat("Unexpected table operation.", curriculumMembership2.getOperation(),
        is(Operation.LOAD));
  }
}

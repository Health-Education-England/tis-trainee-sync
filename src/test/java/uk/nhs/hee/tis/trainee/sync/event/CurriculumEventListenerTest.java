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
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import java.util.Collections;
import java.util.Set;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import uk.nhs.hee.tis.trainee.sync.facade.ProgrammeMembershipEnricherFacade;
import uk.nhs.hee.tis.trainee.sync.model.Curriculum;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.ProgrammeMembership;
import uk.nhs.hee.tis.trainee.sync.service.ProgrammeMembershipSyncService;

class CurriculumEventListenerTest {

  private static final String TIS_ID = "5";
  private static final String PROGRAMME_MEMBERSHIP_QUEUE_URL = "https://queue.programme-membership";

  private CurriculumEventListener listener;
  private ProgrammeMembershipEnricherFacade enricher;
  private CacheManager cacheManager;
  private Cache cache;

  private ProgrammeMembershipSyncService programmeMembershipService;
  private QueueMessagingTemplate messagingTemplate;

  @BeforeEach
  void setUp() {
    enricher = mock(ProgrammeMembershipEnricherFacade.class);
    cacheManager = mock(CacheManager.class);
    cache = mock(Cache.class);
    when(cacheManager.getCache(Curriculum.ENTITY_NAME)).thenReturn(cache);
    programmeMembershipService = mock(ProgrammeMembershipSyncService.class);
    messagingTemplate = mock(QueueMessagingTemplate.class);
    listener = new CurriculumEventListener(programmeMembershipService, messagingTemplate,
        enricher, cacheManager, PROGRAMME_MEMBERSHIP_QUEUE_URL);
  }

  @Test
  void shouldCallEnricherAfterSave() {
    Curriculum curriculum = new Curriculum();
    curriculum.setTisId(TIS_ID);
    AfterSaveEvent<Curriculum> event = new AfterSaveEvent<>(curriculum, null, null);

    listener.onAfterSave(event);

    verify(cache).put(TIS_ID, curriculum);
    verify(enricher).enrich(curriculum);
    verifyNoMoreInteractions(enricher);
  }
}

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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;

import java.util.Optional;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeDeleteEvent;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;
import uk.nhs.hee.tis.trainee.sync.event.ProgrammeMembershipEventListener;
import uk.nhs.hee.tis.trainee.sync.facade.ProgrammeMembershipEnricherFacade;
import uk.nhs.hee.tis.trainee.sync.model.ProgrammeMembership;
import uk.nhs.hee.tis.trainee.sync.repository.ProgrammeMembershipRepository;
import uk.nhs.hee.tis.trainee.sync.service.ProgrammeMembershipSyncService;

@SpringBootTest
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
class ProgrammeMembershipEventListenerTest {

  private ProgrammeMembershipEventListener listener;

  private ProgrammeMembershipEnricherFacade enricher;
  
  private static ProgrammeMembershipRepository mockProgrammeMembershipRepository;

  @Autowired
  private ProgrammeMembershipSyncService programmeMembershipSyncService;

  @Autowired
  CacheManager cacheManager;

  @BeforeEach
  void setUp() {
    enricher = mock(ProgrammeMembershipEnricherFacade.class);
    mockProgrammeMembershipRepository = mock(ProgrammeMembershipRepository.class);
    ReflectionTestUtils.setField(programmeMembershipSyncService, "repository",
        mockProgrammeMembershipRepository);
    listener = new ProgrammeMembershipEventListener(enricher, programmeMembershipSyncService,
        cacheManager);
  }

  @Test
  void shouldCallEnricherAfterSave() {
    ProgrammeMembership record = new ProgrammeMembership();
    AfterSaveEvent<ProgrammeMembership> event = new AfterSaveEvent<>(record, null, null);

    listener.onAfterSave(event);

    verify(enricher).enrich(record);
    verifyNoMoreInteractions(enricher);
  }

  @Test
  void shouldFindProgrammeMembershipIfNotInCacheBeforeDelete() {
    Document document = new Document();
    document.append("_id", "1");
    BeforeDeleteEvent<ProgrammeMembership> event = new BeforeDeleteEvent<>(document, null, null);

    when(mockProgrammeMembershipRepository.findById(anyString())).thenReturn(Optional.empty());

    listener.onBeforeDelete(event);

    verify(mockProgrammeMembershipRepository).findById("1");
    verifyNoMoreInteractions(enricher);
  }

  @Test
  void shouldCallFacadeDeleteAfterDelete() {
    Document document = new Document();
    document.append("_id", "1");
    ProgrammeMembership record = new ProgrammeMembership();

    when(mockProgrammeMembershipRepository.findById(anyString())).thenReturn(Optional.of(record));

    BeforeDeleteEvent<ProgrammeMembership> eventBefore
        = new BeforeDeleteEvent<>(document, null, null);
    AfterDeleteEvent<ProgrammeMembership> eventAfter
        = new AfterDeleteEvent<>(document, null, null);

    listener.onBeforeDelete(eventBefore);
    listener.onAfterDelete(eventAfter);

    verify(mockProgrammeMembershipRepository).findById("1");
    verify(enricher).delete(record);
    verifyNoMoreInteractions(enricher);
  }
}

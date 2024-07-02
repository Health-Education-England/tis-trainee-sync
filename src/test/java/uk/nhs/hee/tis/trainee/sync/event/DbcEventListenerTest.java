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

package uk.nhs.hee.tis.trainee.sync.event;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import uk.nhs.hee.tis.trainee.sync.model.Dbc;
import uk.nhs.hee.tis.trainee.sync.model.Programme;
import uk.nhs.hee.tis.trainee.sync.service.FifoMessagingService;
import uk.nhs.hee.tis.trainee.sync.service.ProgrammeSyncService;

@ExtendWith(OutputCaptureExtension.class)
class DbcEventListenerTest {

  private static final String DBC_ID = "99";
  private static final String OWNER = "heeOwner";
  private static final String PROGRAMME_1_ID = "1";
  private static final String PROGRAMME_2_ID = "2";

  private static final String PROGRAMME_QUEUE_URL = "queue";

  private DbcEventListener listener;
  private ProgrammeSyncService programmeService;
  private FifoMessagingService fifoMessagingService;
  private Cache cache;

  @BeforeEach
  void setUp() {
    programmeService = mock(ProgrammeSyncService.class);
    fifoMessagingService = mock(FifoMessagingService.class);
    CacheManager cacheManager = mock(CacheManager.class);
    cache = mock(Cache.class);
    when(cacheManager.getCache(Dbc.ENTITY_NAME)).thenReturn(cache);
    listener = new DbcEventListener(programmeService, fifoMessagingService,
        PROGRAMME_QUEUE_URL, cacheManager);
  }

  @Test
  void shouldCacheAfterSave() {
    Dbc dbc = new Dbc();
    dbc.setTisId(DBC_ID);
    AfterSaveEvent<Dbc> event = new AfterSaveEvent<>(dbc, null, null);

    listener.onAfterSave(event);

    verify(cache).put(DBC_ID, dbc);
  }

  @Test
  void shouldNotInteractWithProgrammeQueueAfterSaveWhenNoRelatedProgrammes() {
    Dbc dbc = new Dbc();
    dbc.setTisId(DBC_ID);
    AfterSaveEvent<Dbc> event = new AfterSaveEvent<>(dbc, null, null);

    when(programmeService.findByOwner(OWNER)).thenReturn(Collections.emptySet());

    listener.onAfterSave(event);

    verifyNoInteractions(fifoMessagingService);
  }

  @Test
  void shouldLogRelatedProgrammesAfterSaveWhenRelatedProgrammes(CapturedOutput output) {
    Dbc dbc = new Dbc();
    dbc.setTisId(DBC_ID);
    dbc.setData(Map.of("name", OWNER));

    Programme programme1 = new Programme();
    programme1.setTisId(PROGRAMME_1_ID);
    Programme programme2 = new Programme();
    programme2.setTisId(PROGRAMME_2_ID);

    when(programmeService.findByOwner(OWNER)).thenReturn(Set.of(programme1, programme2));

    AfterSaveEvent<Dbc> event = new AfterSaveEvent<>(dbc, null, null);
    listener.onAfterSave(event);

    verifyNoInteractions(fifoMessagingService);

    String log1 = String.format("Dbc %s affects programme %s", OWNER, PROGRAMME_1_ID);
    String log2 = String.format("Dbc %s affects programme %s", OWNER, PROGRAMME_2_ID);
    assertThat("Expected log not found.", output.getOut(), containsString(log1));
    assertThat("Expected log not found.", output.getOut(), containsString(log2));
  }
}

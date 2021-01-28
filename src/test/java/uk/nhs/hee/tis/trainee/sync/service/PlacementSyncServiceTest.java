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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.nhs.hee.tis.trainee.sync.model.Operation.DELETE;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.nhs.hee.tis.trainee.sync.facade.PlacementEnricherFacade;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.Placement;
import uk.nhs.hee.tis.trainee.sync.model.Post;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.repository.PlacementRepository;

class PlacementSyncServiceTest {

  private static final String ID = "40";

  private PlacementSyncService service;

  private PlacementRepository repository;

  private Placement record;

  private MessageSendingService messageSendingService;

  private PostSyncService postSyncService;

  private PlacementEnricherFacade placementEnricherFacade;

  @BeforeEach
  void setUp() {

    messageSendingService = mock(MessageSendingService.class);
    postSyncService = mock(PostSyncService.class);
    repository = mock(PlacementRepository.class);
    placementEnricherFacade = mock(PlacementEnricherFacade.class);
    service = new PlacementSyncService(repository, messageSendingService, postSyncService,
        placementEnricherFacade);

    record = new Placement();
    record.setTisId(ID);
  }

  @Test
  void shouldThrowExceptionIfRecordNotPlacement() {
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
  void shouldFindRecordByPostIdWhenExists() {
    when(repository.findByPostId(ID)).thenReturn(Collections.singleton(record));

    Set<Placement> foundRecords = service.findByPostId(ID);
    assertThat("Unexpected record count.", foundRecords.size(), is(1));

    Placement foundRecord = foundRecords.iterator().next();
    assertThat("Unexpected record.", foundRecord, sameInstance(record));

    verify(repository).findByPostId(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotFindRecordByIdPostWhenNotExists() {
    when(repository.findByPostId(ID)).thenReturn(Collections.emptySet());

    Set<Placement> foundRecords = service.findByPostId(ID);
    assertThat("Unexpected record count.", foundRecords.size(), is(0));

    verify(repository).findByPostId(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldEnrichARequestPost() throws JsonProcessingException {

    when(postSyncService.findById(ID)).thenReturn(Optional.<Post>empty());
    Map<String,String> mockData = mock(Map.class);

    when(record.getData()).thenReturn(mockData);
    when(mockData.get("postId")).thenReturn(ID);
    Map<String,String> data = record.getData();

    service.enrichOrRequestPost(record);

    verify(postSyncService).request(ID);
  }
}

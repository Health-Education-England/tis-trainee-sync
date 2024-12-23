/*
 * The MIT License (MIT)
 *
 *  Copyright 2023 Crown Copyright (Health Education England)
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 *  and associated documentation files (the "Software"), to deal in the Software without
 *  restriction, including without limitation the rights to use, copy, modify, merge, publish,
 *  distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all copies or
 *  substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 *  BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 *  DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package uk.nhs.hee.tis.trainee.sync.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.nhs.hee.tis.trainee.sync.model.Operation.DELETE;
import static uk.nhs.hee.tis.trainee.sync.model.Operation.LOOKUP;
import static uk.nhs.hee.tis.trainee.sync.model.PostSpecialty.ENTITY_NAME;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.PostSpecialty;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.repository.PostSpecialtyRepository;

class PostSpecialtySyncServiceTest {

  private static final String ID = "40";
  private static final String POST_ID = "postId1";
  private static final String SPECIALTY_ID = "specialtyId1";

  private PostSpecialtySyncService service;

  private PostSpecialtyRepository repository;

  private FifoMessagingService fifoMessagingService;

  private ApplicationEventPublisher eventPublisher;

  private PostSpecialty postSpecialty;

  @BeforeEach
  void setUp() {
    repository = mock(PostSpecialtyRepository.class);
    fifoMessagingService = mock(FifoMessagingService.class);
    eventPublisher = mock(ApplicationEventPublisher.class);

    service = new PostSpecialtySyncService(repository, fifoMessagingService,
        "http://queue.postspecialty", eventPublisher);
    postSpecialty = new PostSpecialty();
    postSpecialty.setTisId(ID);
    postSpecialty.setData(Map.of(
        "postId", POST_ID,
        "specialtyId", SPECIALTY_ID,
        "postSpecialtyType", "SUB_SPECIALTY"));
  }

  @Test
  void shouldThrowExceptionIfRecordNotPostSpecialty() {
    Record theRecord = new Record();
    assertThrows(IllegalArgumentException.class, () -> service.syncRecord(theRecord));
  }

  @ParameterizedTest(name = "Should send post specialty records to queue when operation is {0}.")
  @EnumSource(value = Operation.class, names = {"LOAD", "INSERT", "UPDATE", "DELETE"})
  void shouldSendPostSpecialtyRecordsToQueue(Operation operation) {
    postSpecialty.setOperation(operation);

    service.syncRecord(postSpecialty);

    verify(fifoMessagingService).sendMessageToFifoQueue(
        "http://queue.postspecialty", postSpecialty);
    verifyNoInteractions(repository);
  }

  @ParameterizedTest(name = "Should store post sub-specialties when operation is {0}.")
  @EnumSource(value = Operation.class, names = {"LOAD", "INSERT", "UPDATE"})
  void shouldStorePostSubSpecialties(Operation operation) {
    postSpecialty.setOperation(operation);

    service.syncPostSpecialty(postSpecialty);

    verify(repository).save(postSpecialty);
    verifyNoMoreInteractions(repository);
  }

  @ParameterizedTest(name = "Should not store post not-sub specialties when operation is {0}.")
  @EnumSource(value = Operation.class, names = {"LOAD", "INSERT", "UPDATE"})
  void shouldNotStorePostNotSubSpecialties(Operation operation) {
    postSpecialty.setOperation(operation);
    postSpecialty.setData(Map.of("postSpecialtyType", "OTHER"));

    service.syncPostSpecialty(postSpecialty);

    verifyNoInteractions(repository);
  }

  @Test
  void shouldRequestMissingPostSpecialtyWhenOperationLookupAndPostSpecialtyNotFound() {
    postSpecialty.setOperation(LOOKUP);
    when(repository.findById(ID)).thenReturn(Optional.empty());

    service.syncPostSpecialty(postSpecialty);

    verify(repository).findById(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldPublishSavePostSpecialtyEventWhenOperationLookupAndPostSpecialtyFound() {
    postSpecialty.setOperation(LOOKUP);

    PostSpecialty lookupPostSpecialty = new PostSpecialty();
    lookupPostSpecialty.setTisId(ID);
    lookupPostSpecialty.setData(Map.of("dummy", "data"));
    when(repository.findById(ID)).thenReturn(Optional.of(lookupPostSpecialty));

    service.syncPostSpecialty(postSpecialty);

    verify(repository).findById(ID);
    verifyNoMoreInteractions(repository);

    ArgumentCaptor<AfterSaveEvent<PostSpecialty>> eventCaptor = ArgumentCaptor.captor();
    verify(eventPublisher).publishEvent(eventCaptor.capture());

    AfterSaveEvent<PostSpecialty> event = eventCaptor.getValue();
    assertThat("Unexpected event source.", event.getSource(), sameInstance(lookupPostSpecialty));
    assertThat("Unexpected event collection.", event.getCollectionName(), is(ENTITY_NAME));
    assertThat("Unexpected event document.", event.getDocument(), nullValue());
  }

  @Test
  void shouldDeletePostSpecialtyFromStore() {
    postSpecialty.setOperation(DELETE);

    service.syncPostSpecialty(postSpecialty);

    verify(repository).deleteById(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldFindRecordByIdWhenExists() {
    when(repository.findById(ID)).thenReturn(Optional.of(postSpecialty));

    Optional<PostSpecialty> found = service.findById(ID);
    assertThat("Record not found.", found.isPresent(), is(true));
    assertThat("Unexpected record.", found.orElse(null), sameInstance(postSpecialty));

    verify(repository).findById(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotFindRecordByIdWhenNotExists() {
    when(repository.findById(ID)).thenReturn(Optional.empty());

    Optional<PostSpecialty> found = service.findById(ID);
    assertThat("Record not found.", found.isEmpty(), is(true));

    verify(repository).findById(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldFindRecordByPostIdWhenExists() {
    when(repository.findSubSpecialtiesByPostId(POST_ID)).thenReturn(Set.of(postSpecialty));

    Set<PostSpecialty> foundRecords = service.findByPostId(POST_ID);
    assertThat("Unexpected record count.", foundRecords.size(), is(1));

    PostSpecialty foundRecord = foundRecords.iterator().next();
    assertThat("Unexpected record.", foundRecord, sameInstance(postSpecialty));

    verify(repository).findSubSpecialtiesByPostId(POST_ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotFindRecordByPostIdWhenNotExists() {
    when(repository.findSubSpecialtiesByPostId(POST_ID)).thenReturn(Set.of());

    Set<PostSpecialty> foundRecords = service.findByPostId(POST_ID);
    assertThat("Unexpected record count.", foundRecords.size(), is(0));

    verify(repository).findSubSpecialtiesByPostId(POST_ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldFindRecordBySpecialtyIdWhenExists() {
    when(repository.findSubSpecialtiesBySpecialtyId(SPECIALTY_ID))
        .thenReturn(Set.of(postSpecialty));

    Set<PostSpecialty> foundRecords = service.findBySpecialtyId(SPECIALTY_ID);
    assertThat("Unexpected record count.", foundRecords.size(), is(1));

    PostSpecialty foundRecord = foundRecords.iterator().next();
    assertThat("Unexpected record.", foundRecord, sameInstance(postSpecialty));

    verify(repository).findSubSpecialtiesBySpecialtyId(SPECIALTY_ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotFindRecordBySpecialtyIdWhenNotExists() {
    when(repository.findSubSpecialtiesBySpecialtyId(SPECIALTY_ID)).thenReturn(Set.of());

    Set<PostSpecialty> foundRecords = service.findBySpecialtyId(SPECIALTY_ID);
    assertThat("Unexpected record count.", foundRecords.size(), is(0));

    verify(repository).findSubSpecialtiesBySpecialtyId(SPECIALTY_ID);
    verifyNoMoreInteractions(repository);
  }
}

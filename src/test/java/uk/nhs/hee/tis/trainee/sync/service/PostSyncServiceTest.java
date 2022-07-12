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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.nhs.hee.tis.trainee.sync.model.Operation.DELETE;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import io.lettuce.core.RedisClient;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.Post;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.repository.PostRepository;

@SpringBootTest
@ActiveProfiles("int")
class PostSyncServiceTest {

  private static final String ID = "40";
  private static final String ID_2 = "140";

  private PostSyncService service;

  private PostRepository repository;

  private QueueMessagingTemplate queueMessagingTemplate;

  private Post post;

  private DataRequestService dataRequestService;

  @Autowired
  private RedisClient redisClient;

  @Value("${spring.redis.requests-cache.database}")
  Integer redisDb;
  @Value("${spring.redis.requests-cache.ttl}")
  Long redisTtl;

  private Map<String, String> whereMap;

  private Map<String, String> whereMap2;

  @BeforeEach
  void setUp() {
    dataRequestService = mock(DataRequestService.class);
    repository = mock(PostRepository.class);
    queueMessagingTemplate = mock(QueueMessagingTemplate.class);
    service = new PostSyncService(repository, dataRequestService, queueMessagingTemplate,
        "http://queue.post", redisClient);
    service.redisDb = redisDb;
    service.redisTtl = redisTtl;
    post = new Post();
    post.setTisId(ID);

    whereMap = Map.of("id", ID);
    whereMap2 = Map.of("id", ID_2);
  }

  @Test
  void shouldThrowExceptionIfRecordNotPost() {
    Record recrd = new Record();
    assertThrows(IllegalArgumentException.class, () -> service.syncRecord(recrd));
  }

  @ParameterizedTest(name = "Should send post records to queue when operation is {0}.")
  @EnumSource(value = Operation.class, names = {"LOAD", "INSERT", "UPDATE", "DELETE"})
  void shouldSendPostRecordsToQueue(Operation operation) {
    post.setOperation(operation);

    service.syncRecord(post);

    verify(queueMessagingTemplate).convertAndSend("http://queue.post", post);
    verifyNoInteractions(repository);
  }

  @ParameterizedTest(name = "Should store posts when operation is {0}.")
  @EnumSource(value = Operation.class, names = {"LOAD", "INSERT", "UPDATE"})
  void shouldStorePosts(Operation operation) {
    post.setOperation(operation);

    service.syncPost(post);

    verify(repository).save(post);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldDeletePostFromStore() {
    post.setOperation(DELETE);

    service.syncPost(post);

    verify(repository).deleteById(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldFindRecordByIdWhenExists() {
    when(repository.findById(ID)).thenReturn(Optional.of(post));

    Optional<Post> found = service.findById(ID);
    assertThat("Record not found.", found.isPresent(), is(true));
    assertThat("Unexpected record.", found.orElse(null), sameInstance(post));

    verify(repository).findById(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotFindRecordByIdWhenNotExists() {
    when(repository.findById(ID)).thenReturn(Optional.empty());

    Optional<Post> found = service.findById(ID);
    assertThat("Record not found.", found.isEmpty(), is(true));

    verify(repository).findById(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldFindRecordByEmployingBodyIdWhenExists() {
    when(repository.findByEmployingBodyId(ID)).thenReturn(Collections.singleton(post));

    Set<Post> foundRecords = service.findByEmployingBodyId(ID);
    assertThat("Unexpected record count.", foundRecords.size(), is(1));

    Post foundRecord = foundRecords.iterator().next();
    assertThat("Unexpected record.", foundRecord, sameInstance(post));

    verify(repository).findByEmployingBodyId(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotFindRecordByIdEmployingBodyWhenNotExists() {
    when(repository.findByEmployingBodyId(ID)).thenReturn(Collections.emptySet());

    Set<Post> foundRecords = service.findByEmployingBodyId(ID);
    assertThat("Unexpected record count.", foundRecords.size(), is(0));

    verify(repository).findByEmployingBodyId(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldFindRecordByTrainingBodyIdWhenExists() {
    when(repository.findByTrainingBodyId(ID)).thenReturn(Collections.singleton(post));

    Set<Post> foundRecords = service.findByTrainingBodyId(ID);
    assertThat("Unexpected record count.", foundRecords.size(), is(1));

    Post foundRecord = foundRecords.iterator().next();
    assertThat("Unexpected record.", foundRecord, sameInstance(post));

    verify(repository).findByTrainingBodyId(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotFindRecordByIdTrainingBodyWhenNotExists() {
    when(repository.findByTrainingBodyId(ID)).thenReturn(Collections.emptySet());

    Set<Post> foundRecords = service.findByTrainingBodyId(ID);
    assertThat("Unexpected record count.", foundRecords.size(), is(0));

    verify(repository).findByTrainingBodyId(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  @DirtiesContext
  void shouldSendRequestWhenNotAlreadyRequested() throws JsonProcessingException {
    service.request(ID);
    verify(dataRequestService).sendRequest("Post", whereMap);
  }

  @Test
  @DirtiesContext
  void shouldNotSendRequestWhenAlreadyRequested() throws JsonProcessingException {
    service.request(ID);
    service.request(ID);
    verify(dataRequestService, atMostOnce()).sendRequest("Post", whereMap);
    verifyNoMoreInteractions(dataRequestService);
  }

  @Test
  @DirtiesContext
  void shouldSendRequestWhenSyncedBetweenRequests() throws JsonProcessingException {
    service.request(ID);

    post.setOperation(DELETE);
    service.syncPost(post);

    service.request(ID);
    verify(dataRequestService, times(2)).sendRequest("Post", whereMap);
  }

  @Test
  @DirtiesContext
  void shouldSendRequestWhenRequestedDifferentIds() throws JsonProcessingException {
    service.request(ID);
    service.request("140");
    verify(dataRequestService, atMostOnce()).sendRequest("Post", whereMap);
    verify(dataRequestService, atMostOnce()).sendRequest("Post", whereMap2);
  }

  @Test
  @DirtiesContext
  void shouldSendRequestWhenFirstRequestFails() throws JsonProcessingException {
    doThrow(JsonProcessingException.class).when(dataRequestService)
        .sendRequest(anyString(), anyMap());

    service.request(ID);
    service.request(ID);

    verify(dataRequestService, times(2)).sendRequest("Post", whereMap);
  }

  @Test
  void shouldCatchJsonProcessingExceptionIfThrown() throws JsonProcessingException {
    doThrow(JsonProcessingException.class).when(dataRequestService)
        .sendRequest(anyString(), anyMap());
    assertDoesNotThrow(() -> service.request(ID));
  }

  @Test
  void shouldThrowAnExceptionIfNotJsonProcessingException() throws JsonProcessingException {
    IllegalStateException illegalStateException = new IllegalStateException("error");
    doThrow(illegalStateException).when(dataRequestService).sendRequest(anyString(),
        anyMap());
    assertThrows(IllegalStateException.class, () -> service.request(ID));
    assertEquals("error", illegalStateException.getMessage());
  }
}

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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.nhs.hee.tis.trainee.sync.model.Operation.LOOKUP;

import java.util.Collections;
import java.util.Set;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.Post;
import uk.nhs.hee.tis.trainee.sync.model.Trust;
import uk.nhs.hee.tis.trainee.sync.service.FifoMessagingService;
import uk.nhs.hee.tis.trainee.sync.service.PostSyncService;

class TrustEventListenerTest {

  private static final String POST_QUEUE_URL = "https://queue.post";

  private TrustEventListener listener;
  private PostSyncService postService;
  private FifoMessagingService fifoMessagingService;

  @BeforeEach
  void setUp() {
    postService = mock(PostSyncService.class);
    fifoMessagingService = mock(FifoMessagingService.class);
    listener = new TrustEventListener(postService, fifoMessagingService, POST_QUEUE_URL);
  }

  @Test
  void shouldNotInteractWithPostQueueAfterSaveWhenNoRelatedPosts() {
    Trust trust = new Trust();
    trust.setTisId("trust1");
    AfterSaveEvent<Trust> event = new AfterSaveEvent<>(trust, null, null);

    when(postService.findByEmployingBodyId("trust1")).thenReturn(Collections.emptySet());
    when(postService.findByTrainingBodyId("trust1")).thenReturn(Collections.emptySet());

    listener.onAfterSave(event);

    verifyNoInteractions(fifoMessagingService);
  }

  @Test
  void shouldSendRelatedPostsToQueueAfterSaveWhenRelatedPosts() {
    Trust trust = new Trust();
    trust.setTisId("trust1");

    Post post1 = new Post();
    post1.setTisId("post1");

    Post post2 = new Post();
    post2.setTisId("post2");

    Post post3 = new Post();
    post3.setTisId("post3");
    when(postService.findByTrainingBodyId("trust1")).thenReturn(Set.of(post1, post2));
    when(postService.findByEmployingBodyId("trust1")).thenReturn(Set.of(post2, post3));

    AfterSaveEvent<Trust> event = new AfterSaveEvent<>(trust, null, null);
    listener.onAfterSave(event);

    verify(fifoMessagingService).sendMessageToFifoQueue(
        eq(POST_QUEUE_URL), eq(post1), any());
    assertThat("Unexpected table operation.", post1.getOperation(), is(LOOKUP));

    verify(fifoMessagingService).sendMessageToFifoQueue(
        eq(POST_QUEUE_URL), eq(post2), any());
    assertThat("Unexpected table operation.", post2.getOperation(), is(LOOKUP));

    verify(fifoMessagingService).sendMessageToFifoQueue(
        eq(POST_QUEUE_URL), eq(post3), any());
    assertThat("Unexpected table operation.", post3.getOperation(), is(LOOKUP));
  }

  @Test
  void shouldNotInteractWithPostQueueAfterDeleteWhenNoRelatedPosts() {
    Document document = new Document();
    document.append("_id", "trust1");
    AfterDeleteEvent<Trust> event = new AfterDeleteEvent<>(document, Trust.class, "trust");

    when(postService.findByEmployingBodyId("trust1")).thenReturn(Collections.emptySet());
    when(postService.findByTrainingBodyId("trust1")).thenReturn(Collections.emptySet());

    listener.onAfterDelete(event);

    verifyNoInteractions(fifoMessagingService);
  }

  @Test
  void shouldSendRelatedPostsToQueueAfterDeleteWhenRelatedPosts() {
    Document document = new Document();
    document.append("_id", "trust1");

    Post post1 = new Post();
    post1.setTisId("post1");

    Post post2 = new Post();
    post2.setTisId("post2");

    Post post3 = new Post();
    post3.setTisId("post3");
    when(postService.findByTrainingBodyId("trust1")).thenReturn(Set.of(post1, post2));
    when(postService.findByEmployingBodyId("trust1")).thenReturn(Set.of(post2, post3));

    AfterDeleteEvent<Trust> event = new AfterDeleteEvent<>(document, Trust.class, "trust");
    listener.onAfterDelete(event);

    verify(fifoMessagingService).sendMessageToFifoQueue(
        eq(POST_QUEUE_URL), eq(post1), any());
    assertThat("Unexpected table operation.", post1.getOperation(), is(Operation.DELETE));

    verify(fifoMessagingService).sendMessageToFifoQueue(
        eq(POST_QUEUE_URL), eq(post2), any());
    assertThat("Unexpected table operation.", post2.getOperation(), is(Operation.DELETE));

    verify(fifoMessagingService).sendMessageToFifoQueue(
        eq(POST_QUEUE_URL), eq(post3), any());
    assertThat("Unexpected table operation.", post3.getOperation(), is(Operation.DELETE));
  }
}

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

package uk.nhs.hee.tis.trainee.sync.event;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import java.util.Map;
import java.util.Optional;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeDeleteEvent;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.Post;
import uk.nhs.hee.tis.trainee.sync.model.PostSpecialty;
import uk.nhs.hee.tis.trainee.sync.model.Specialty;
import uk.nhs.hee.tis.trainee.sync.service.PostSpecialtySyncService;
import uk.nhs.hee.tis.trainee.sync.service.PostSyncService;
import uk.nhs.hee.tis.trainee.sync.service.SpecialtySyncService;

class PostSpecialtyEventListenerTest {

  private static final String POST_QUEUE_URL = "https://queue.post";
  private static final String ID = "postSpecialtyId1";
  private static final String POST_ID = "postId1";
  private static final String SPECIALTY_ID = "specialtyId1";

  private PostSpecialtyEventListener listener;
  private PostSyncService postService;
  private SpecialtySyncService specialtyService;
  private PostSpecialtySyncService postSpecialtyService;
  private QueueMessagingTemplate messagingTemplate;
  private PostSpecialty postSpecialty;
  private Cache cache;

  @BeforeEach
  void setUp() {
    postService = mock(PostSyncService.class);
    specialtyService = mock(SpecialtySyncService.class);
    postSpecialtyService = mock(PostSpecialtySyncService.class);
    messagingTemplate = mock(QueueMessagingTemplate.class);

    CacheManager cacheManager = mock(CacheManager.class);
    cache = mock(Cache.class);
    when(cacheManager.getCache(anyString())).thenReturn(cache);

    listener = new PostSpecialtyEventListener(postService, specialtyService, postSpecialtyService,
        messagingTemplate, cacheManager, POST_QUEUE_URL);

    postSpecialty = new PostSpecialty();
    postSpecialty.setTisId(ID);
    postSpecialty.setData(Map.of("postId", POST_ID, "specialtyId", SPECIALTY_ID));
  }

  @Test
  void shouldRequestSpecialtyAfterSaveWhenNoRelatedSpecialty() {
    when(specialtyService.findById(SPECIALTY_ID)).thenReturn(Optional.empty());

    AfterSaveEvent<PostSpecialty> event = new AfterSaveEvent<>(postSpecialty, null, null);
    listener.onAfterSave(event);

    verify(specialtyService).request(SPECIALTY_ID);

    verifyNoInteractions(messagingTemplate);
  }

  @Test
  void shouldNotRequestSpecialtyAfterSaveWhenRelatedSpecialtyExists() {
    Specialty specialty = new Specialty();
    specialty.setTisId(SPECIALTY_ID);

    when(specialtyService.findById(SPECIALTY_ID)).thenReturn(Optional.of(specialty));

    AfterSaveEvent<PostSpecialty> event = new AfterSaveEvent<>(postSpecialty, null, null);
    listener.onAfterSave(event);

    verify(specialtyService, never()).request(any());

    verifyNoInteractions(messagingTemplate);
  }

  @Test
  void shouldRequestPostAfterSaveWhenNoRelatedPost() {
    when(postService.findById(POST_ID)).thenReturn(Optional.empty());

    AfterSaveEvent<PostSpecialty> event = new AfterSaveEvent<>(postSpecialty, null, null);
    listener.onAfterSave(event);

    verify(postService).request(POST_ID);

    verifyNoInteractions(messagingTemplate);
  }

  @Test
  void shouldNotRequestPostAfterSaveWhenRelatedPostExists() {
    Post post = new Post();
    post.setTisId(POST_ID);

    when(postService.findById(POST_ID)).thenReturn(Optional.of(post));

    AfterSaveEvent<PostSpecialty> event = new AfterSaveEvent<>(postSpecialty, null, null);
    listener.onAfterSave(event);

    verify(postService, never()).request(any());
  }

  @Test
  void shouldQueueRelatedPostWhenFoundAfterSave() {
    Post post = new Post();
    post.setTisId(POST_ID);

    when(postService.findById(POST_ID)).thenReturn(Optional.of(post));

    AfterSaveEvent<PostSpecialty> event = new AfterSaveEvent<>(postSpecialty, null, null);
    listener.onAfterSave(event);

    verify(postService, never()).request(any());
    verify(messagingTemplate).convertAndSend(POST_QUEUE_URL, post);
    assertThat("Unexpected table operation.", post.getOperation(), is(Operation.LOAD));
  }

  @Test
  void shouldFindAndCachePostSpecialtyIfNotInCacheBeforeDelete() {
    Document document = new Document();
    document.append("_id", ID);
    PostSpecialty postSpecialty = new PostSpecialty();
    BeforeDeleteEvent<PostSpecialty> event = new BeforeDeleteEvent<>(document, null, null);

    when(cache.get(ID, PostSpecialty.class)).thenReturn(null);
    when(postSpecialtyService.findById(ID)).thenReturn(Optional.of(postSpecialty));

    listener.onBeforeDelete(event);

    verify(postSpecialtyService).findById(ID);
    verify(cache).put(ID, postSpecialty);
    verifyNoInteractions(messagingTemplate);
  }

  @Test
  void shouldNotFindAndCachePostSpecialtyIfInCacheBeforeDelete() {
    Document document = new Document();
    document.append("_id", ID);
    PostSpecialty postSpecialty = new PostSpecialty();
    BeforeDeleteEvent<PostSpecialty> event = new BeforeDeleteEvent<>(document, null, null);

    when(cache.get(ID, PostSpecialty.class)).thenReturn(postSpecialty);

    listener.onBeforeDelete(event);

    verifyNoInteractions(postSpecialtyService);
    verifyNoInteractions(messagingTemplate);
  }

  @Test
  void shouldNotQueueRelatedPostWhenPostSpecialtyNotInCacheAfterDelete() {
    Document document = new Document();
    document.append("_id", ID);
    AfterDeleteEvent<PostSpecialty> event = new AfterDeleteEvent<>(document, null, null);

    when(cache.get(ID, PostSpecialty.class)).thenReturn(null);

    listener.onAfterDelete(event);

    verifyNoInteractions(messagingTemplate);
  }

  @Test
  void shouldQueueRelatedPostAfterDelete() {
    Document document = new Document();
    document.append("_id", ID);

    Post post = new Post();
    post.setTisId(POST_ID);

    when(postService.findById(POST_ID)).thenReturn(Optional.of(post));
    when(cache.get(ID, PostSpecialty.class)).thenReturn(postSpecialty);

    AfterDeleteEvent<PostSpecialty> event = new AfterDeleteEvent<>(document, null, null);
    listener.onAfterDelete(event);

    verify(postService, never()).request(any());
    verify(messagingTemplate).convertAndSend(POST_QUEUE_URL, post);
    assertThat("Unexpected table operation.", post.getOperation(), is(Operation.LOAD));
  }

  @Test
  void shouldNotSendMissingPostAfterDelete() {
    when(cache.get(ID, PostSpecialty.class)).thenReturn(postSpecialty);
    when(postService.findById(POST_ID)).thenReturn(Optional.empty());

    Document document = new Document();
    document.append("_id", ID);

    AfterDeleteEvent<PostSpecialty> eventAfter
        = new AfterDeleteEvent<>(document, null, null);

    listener.onAfterDelete(eventAfter);

    verifyNoInteractions(messagingTemplate);
  }
}

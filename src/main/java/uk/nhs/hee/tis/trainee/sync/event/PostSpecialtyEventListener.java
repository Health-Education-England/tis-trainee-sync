/*
 * The MIT License (MIT)
 *
 *  Copyright 2023 Crown Copyright (Health Education England)
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 *  associated documentation files (the "Software"), to deal in the Software without restriction,
 *  including without limitation the rights to use, copy, modify, merge, publish, distribute,
 *  sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all copies or
 *  substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 *  NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 *  DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package uk.nhs.hee.tis.trainee.sync.event;

import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeDeleteEvent;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.Post;
import uk.nhs.hee.tis.trainee.sync.model.PostSpecialty;
import uk.nhs.hee.tis.trainee.sync.model.Specialty;
import uk.nhs.hee.tis.trainee.sync.service.PostSpecialtySyncService;
import uk.nhs.hee.tis.trainee.sync.service.PostSyncService;
import uk.nhs.hee.tis.trainee.sync.service.SpecialtySyncService;

@Component
public class PostSpecialtyEventListener extends AbstractMongoEventListener<PostSpecialty> {

  private final PostSyncService postService;
  private final SpecialtySyncService specialtyService;
  private final PostSpecialtySyncService postSpecialtyService;

  private final Cache postSpecialtyCache;

  private final QueueMessagingTemplate messagingTemplate;

  private final String postQueueUrl;

  PostSpecialtyEventListener(PostSyncService postService, SpecialtySyncService specialtyService,
      PostSpecialtySyncService postSpecialtyService,
      QueueMessagingTemplate messagingTemplate, CacheManager cacheManager,
      @Value("${application.aws.sqs.post}") String postQueueUrl) {
    this.postService = postService;
    this.specialtyService = specialtyService;
    this.postSpecialtyService = postSpecialtyService;
    this.messagingTemplate = messagingTemplate;
    this.postQueueUrl = postQueueUrl;
    postSpecialtyCache = cacheManager.getCache(PostSpecialty.ENTITY_NAME);
  }

  @Override
  public void onAfterSave(AfterSaveEvent<PostSpecialty> event) {
    super.onAfterSave(event);

    PostSpecialty postSpecialty = event.getSource();
    String postId = postSpecialty.getData().get("postId");
    String specialtyId = postSpecialty.getData().get("specialtyId");

    Optional<Post> postOptional = postService.findById(postId);

    if (postOptional.isPresent()) {
      Post post = postOptional.get();
      post.setOperation(Operation.LOAD);
      messagingTemplate.convertAndSend(postQueueUrl, post);
    } else {
      // Request the missing Post record.
      postService.request(postId);
    }

    Optional<Specialty> specialtyOptional = specialtyService.findById(specialtyId);

    if (specialtyOptional.isEmpty()) {
      // Request the missing Specialty record.
      specialtyService.request(specialtyId);
    }
  }

  /**
   * Before deleting a post specialty, ensure it is cached.
   *
   * @param event The before-delete event for the post specialty.
   */
  @Override
  public void onBeforeDelete(BeforeDeleteEvent<PostSpecialty> event) {
    String id = event.getSource().getString("_id");
    PostSpecialty postSpecialty = postSpecialtyCache.get(id, PostSpecialty.class);
    if (postSpecialty == null) {
      Optional<PostSpecialty> newPostSpecialty = postSpecialtyService.findById(id);
      newPostSpecialty.ifPresent(membership -> postSpecialtyCache.put(id, membership));
    }
  }

  @Override
  public void onAfterDelete(AfterDeleteEvent<PostSpecialty> event) {
    super.onAfterDelete(event);

    PostSpecialty postSpecialty =
        postSpecialtyCache.get(event.getSource().getString("_id"), PostSpecialty.class);
    if (postSpecialty != null) {
      Optional<Post> postOptional = postService.findById(postSpecialty.getData().get("postId"));

      if (postOptional.isPresent()) {
        Post post = postOptional.get();
        post.setOperation(Operation.LOAD);
        messagingTemplate.convertAndSend(postQueueUrl, post);
      }
    }
  }
}

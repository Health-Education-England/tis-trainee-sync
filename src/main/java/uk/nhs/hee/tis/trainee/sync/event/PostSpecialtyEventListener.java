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

import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import java.time.Instant;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
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
import uk.nhs.hee.tis.trainee.sync.service.FifoMessagingService;
import uk.nhs.hee.tis.trainee.sync.service.PostSpecialtySyncService;
import uk.nhs.hee.tis.trainee.sync.service.PostSyncService;
import uk.nhs.hee.tis.trainee.sync.service.SpecialtySyncService;

/**
 * A listener for Mongo events associated with PostSpecialty data.
 */
@Slf4j
@Component
public class PostSpecialtyEventListener extends AbstractMongoEventListener<PostSpecialty> {

  private final PostSyncService postService;
  private final SpecialtySyncService specialtyService;
  private final PostSpecialtySyncService postSpecialtyService;

  private final Cache postSpecialtyCache;

  private final FifoMessagingService fifoMessagingService;

  private final String postQueueUrl;

  /**
   * Construct a listener for PostSpecialty Mongo events.
   *
   * @param postService          The post service.
   * @param specialtyService     The specialty service.
   * @param postSpecialtyService The post specialty service.
   * @param fifoMessagingService    FIFO queue service for placement expansion.
   * @param cacheManager         The cache for deleted records.
   * @param postQueueUrl         The queue to expand posts into.
   */
  PostSpecialtyEventListener(PostSyncService postService, SpecialtySyncService specialtyService,
      PostSpecialtySyncService postSpecialtyService,
      FifoMessagingService fifoMessagingService, CacheManager cacheManager,
      @Value("${application.aws.sqs.post}") String postQueueUrl) {
    this.postService = postService;
    this.specialtyService = specialtyService;
    this.postSpecialtyService = postSpecialtyService;
    this.fifoMessagingService = fifoMessagingService;
    this.postQueueUrl = postQueueUrl;
    postSpecialtyCache = cacheManager.getCache(PostSpecialty.ENTITY_NAME);
  }

  /**
   * After saving a post specialty the related post should be handled.
   *
   * @param event the after-save event for the post specialty.
   */
  @Override
  public void onAfterSave(AfterSaveEvent<PostSpecialty> event) {
    super.onAfterSave(event);

    PostSpecialty postSpecialty = event.getSource();
    String postId = postSpecialty.getData().get("postId");
    String specialtyId = postSpecialty.getData().get("specialtyId");

    Optional<Post> postOptional = postService.findById(postId);

    if (postOptional.isPresent()) {
      log.debug("Post {} found, queuing for re-sync.", postId);

      Post post = postOptional.get();
      post.setOperation(Operation.LOAD);
      String deduplicationId = String.format("%s_%s_%s", "Post", post.getTisId(),
          Instant.now());
      fifoMessagingService.sendMessageToFifoQueue(postQueueUrl, post, deduplicationId);
    } else {
      // Request the missing Post record.
      log.info("Post {} not found, requesting data.", postId);
      postService.request(postId);
    }

    Optional<Specialty> specialtyOptional = specialtyService.findById(specialtyId);

    if (specialtyOptional.isEmpty()) {
      // Request the missing Specialty record.
      log.info("Specialty {} not found, requesting data.", specialtyId);
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
    super.onBeforeDelete(event);

    String id = event.getSource().getString("_id");
    PostSpecialty postSpecialty = postSpecialtyCache.get(id, PostSpecialty.class);

    if (postSpecialty == null) {
      Optional<PostSpecialty> newPostSpecialty = postSpecialtyService.findById(id);
      newPostSpecialty.ifPresent(
          postSpecialtyToCache -> postSpecialtyCache.put(id, postSpecialtyToCache));
    }
  }

  /**
   * Retrieve the deleted PostSpecialty from the cache and sync the updated post.
   *
   * @param event The after-delete event for the post specialty.
   */
  @Override
  public void onAfterDelete(AfterDeleteEvent<PostSpecialty> event) {
    super.onAfterDelete(event);

    String id = event.getSource().getString("_id");
    PostSpecialty postSpecialty = postSpecialtyCache.get(id, PostSpecialty.class);

    if (postSpecialty != null) {
      String postId = postSpecialty.getData().get("postId");
      Optional<Post> postOptional = postService.findById(postId);

      if (postOptional.isPresent()) {
        log.debug("Post {} found, queuing for re-sync.", postId);

        Post post = postOptional.get();
        post.setOperation(Operation.LOAD);
        String deduplicationId = String.format("%s_%s_%s", "Post", post.getTisId(),
            Instant.now());
        fifoMessagingService.sendMessageToFifoQueue(postQueueUrl, post, deduplicationId);
      }
    }
  }
}

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

import static uk.nhs.hee.tis.trainee.sync.model.Operation.DELETE;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.trainee.sync.model.Post;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.repository.PostRepository;

@Slf4j
@Service("tcs-Post")
public class PostSyncService implements SyncService {

  private final PostRepository repository;

  private final DataRequestService dataRequestService;

  private final RequestCacheService requestCacheService;

  private final QueueMessagingTemplate messagingTemplate;

  private final String queueUrl;

  PostSyncService(PostRepository repository, DataRequestService dataRequestService,
      QueueMessagingTemplate messagingTemplate,
      @Value("${application.aws.sqs.post}") String queueUrl, RequestCacheService requestCacheService) {
    this.repository = repository;
    this.dataRequestService = dataRequestService;
    this.messagingTemplate = messagingTemplate;
    this.queueUrl = queueUrl;
    this.requestCacheService = requestCacheService;
    this.requestCacheService.setKeyPrefix(Post.ENTITY_NAME);
  }

  @Override
  public void syncRecord(Record post) {
    if (!(post instanceof Post)) {
      String message = String.format("Invalid record type '%s'.", post.getClass());
      throw new IllegalArgumentException(message);
    }

    // Send incoming post records to the post queue to be processed.
    messagingTemplate.convertAndSend(queueUrl, post);
  }

  /**
   * Synchronize the given post.
   *
   * @param post The post to synchronize.
   */
  public void syncPost(Post post) {
    if (post.getOperation().equals(DELETE)) {
      repository.deleteById(post.getTisId());
    } else {
      repository.save(post);
    }

    requestCacheService.deleteItemFromCache(post.getTisId());
  }

  public Optional<Post> findById(String id) {
    return repository.findById(id);
  }

  public Set<Post> findByEmployingBodyId(String trustId) {
    return repository.findByEmployingBodyId(trustId);
  }

  public Set<Post> findByTrainingBodyId(String trustId) {
    return repository.findByTrainingBodyId(trustId);
  }

  /**
   * Make a request to retrieve a specific post.
   *
   * @param id The id of the post to be retrieved.
   */
  public void request(String id) {
    if (!requestCacheService.isItemInCache(id)) {
      log.info("Sending request for Post [{}]", id);

      try {
        dataRequestService.sendRequest(Post.ENTITY_NAME, Map.of("id", id));
        requestCacheService.addItemToCache(id);
      } catch (JsonProcessingException e) {
        log.error("Error while trying to request a Post", e);
      }
    } else {
      log.debug("Already requested Post [{}].", id);
    }
  }
}

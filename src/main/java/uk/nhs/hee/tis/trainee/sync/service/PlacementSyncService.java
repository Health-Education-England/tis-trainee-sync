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
import java.util.Optional;
import java.util.Set;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.trainee.sync.model.Placement;
import uk.nhs.hee.tis.trainee.sync.model.Post;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.repository.PlacementRepository;

@Slf4j
@Service("tcs-Placement")
public class PlacementSyncService implements SyncService {

  private static final Logger LOG = LoggerFactory.getLogger(PlacementSyncService.class);

  private final PlacementRepository repository;

  private MessageSendingService messageSendingService;

  private PostSyncService postSyncService;

  private static final String TABLE = "Placement";

  PlacementSyncService(PlacementRepository repository,
                       MessageSendingService messageSendingService,
                       PostSyncService postSyncService) {
    this.repository = repository;
    this.messageSendingService = messageSendingService;
    this.postSyncService = postSyncService;
  }

  @Override
  public void syncRecord(Record record) {
    if (!(record instanceof Placement)) {
      String message = String.format("Invalid record type '%s'.", record.getClass());
      throw new IllegalArgumentException(message);
    }

    if (record.getOperation().equals(DELETE)) {
      repository.deleteById(record.getTisId());
    } else {
      repository.save((Placement) record);
    }

    switch (record.getOperation()) {
      case LOAD:
      case INSERT:
      case UPDATE:
        enrichOrRequestPost(record);
        break;
      case DELETE:
    }
  }

  private void enrichOrRequestPost(Record record) {
    String postId = record.getData().get("postId");
    Optional<Post> fetchedPost = postSyncService.findById(postId);

    if (fetchedPost.isPresent()) {
      // TODO: Implement.
    } else {
      postSyncService.request(postId);
    }
  }

  public Set<Placement> findByPostId(String postId) {
    return repository.findByPostId(postId);
  }

  public void request(String id) {
    try {
      messageSendingService.sendMessage(TABLE, id);
    } catch (JsonProcessingException e) {
      LOG.error("Error while trying to retrieve a Post", e);
    }
  }
}

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

package uk.nhs.hee.tis.trainee.sync.service;

import static uk.nhs.hee.tis.trainee.sync.model.Operation.DELETE;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.trainee.sync.model.PlacementSpecialty;
import uk.nhs.hee.tis.trainee.sync.model.PostSpecialty;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.repository.PostSpecialtyRepository;

@Slf4j
@Service("tcs-PostSpecialty")
public class PostSpecialtySyncService implements SyncService {

  private static final String POST_ID = "postId";
  private final PostSpecialtyRepository repository;
  private final DataRequestService dataRequestService;
  private final RequestCacheService requestCacheService;

  private final QueueMessagingTemplate messagingTemplate;
  private final String queueUrl;

  PostSpecialtySyncService(PostSpecialtyRepository repository,
      DataRequestService dataRequestService,
      QueueMessagingTemplate messagingTemplate,
      @Value("${application.aws.sqs.post-specialty}") String queueUrl,
      RequestCacheService requestCacheService) {
    this.repository = repository;
    this.dataRequestService = dataRequestService;
    this.messagingTemplate = messagingTemplate;
    this.queueUrl = queueUrl;
    this.requestCacheService = requestCacheService;
  }

  @Override
  public void syncRecord(Record postSpecialty) {
    if (!(postSpecialty instanceof PostSpecialty)) {
      String message = String.format("Invalid record type '%s'.", postSpecialty.getClass());
      throw new IllegalArgumentException(message);
    }

    // Send incoming post specialty records to the post specialty queue to be processed.
    messagingTemplate.convertAndSend(queueUrl, postSpecialty);
  }

  /**
   * Synchronize the given post specialty.
   *
   * @param postSpecialty The post specialty to synchronize.
   */
  public void syncPostSpecialty(PostSpecialty postSpecialty) {
    if (postSpecialty.getOperation().equals(DELETE)) {
      String postId = postSpecialty.getData().get(POST_ID);
      Optional<PostSpecialty> storedPostSpecialty = repository.findById();
      if (storedPostSpecialty.isEmpty() || haveSameSpecialtyIds(postSpecialty,
          storedPostSpecialty.get())) {
        repository.deleteById();
      }
    } else {
      if (Objects.equals(postSpecialty.getData().get("postSpecialtyType"), "SUB_SPECIALTY")) {
        Map<String, String> postSpecialtyData = postSpecialty.getData();
        String postId = postSpecialtyData.get(POST_ID);
        postSpecialty.setTisId(postId);
        repository.save(postSpecialty);
      }
    }

    requestCacheService.deleteItemFromCache(PostSpecialty.ENTITY_NAME,
        postSpecialty.getTisId());
  }

  public Optional<PostSpecialty> findById(String id) {
    return repository.findById(id);
  }

  /**
   * Make a request to retrieve a specific placementPostSpecialty. Note: since many
   * post specialty per placement can be SUB_SPECIALTY, placementId is used as the primary key for
   * this repository.
   * FIXME
   * @param id The id of the placementPostSpecialty to be retrieved.
   */
  public void request(String id) {
    if (!requestCacheService.isItemInCache(PostSpecialty.ENTITY_NAME, id)) {
      log.info("Sending request for PostSpecialty [{}]", id);

      try {
        requestCacheService.addItemToCache(PlacementSpecialty.ENTITY_NAME, id,
            dataRequestService.sendRequest(PlacementSpecialty.ENTITY_NAME,
                Map.of(POST_ID, id, "placementSpecialtyType", "PRIMARY")));
      } catch (JsonProcessingException e) {
        log.error("Error while trying to request a PlacementSpecialty", e);
      }
    } else {
      log.debug("Already requested PlacementSpecialty [{}].", id);
    }
  }

  private boolean haveSameSpecialtyIds(Record postSpecialty,
      PostSpecialty storedPostSpecialty) {
    return Objects.equals(postSpecialty.getData().get("specialtyId"),
        storedPostSpecialty.getData().get("specialtyId"));
  }
}

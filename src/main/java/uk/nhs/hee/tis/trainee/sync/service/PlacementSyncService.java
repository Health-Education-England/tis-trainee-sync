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
import uk.nhs.hee.tis.trainee.sync.model.Placement;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.repository.PlacementRepository;

@Slf4j
@Service("tcs-Placement")
public class PlacementSyncService implements SyncService {

  private final PlacementRepository repository;

  private final DataRequestService dataRequestService;

  private final RequestCacheService requestCacheService;

  private final FifoMessagingService fifoMessagingService;

  private final String queueUrl;

  PlacementSyncService(PlacementRepository repository, DataRequestService dataRequestService,
                       FifoMessagingService fifoMessagingService,
                       @Value("${application.aws.sqs.placement}") String queueUrl,
                       RequestCacheService requestCacheService) {
    this.repository = repository;
    this.dataRequestService = dataRequestService;
    this.fifoMessagingService = fifoMessagingService;
    this.queueUrl = queueUrl;
    this.requestCacheService = requestCacheService;
  }

  @Override
  public void syncRecord(Record placement) {
    if (!(placement instanceof Placement)) {
      String message = String.format("Invalid record type '%s'.", placement.getClass());
      throw new IllegalArgumentException(message);
    }

    // Send incoming placement records to the placement queue to be processed.
    fifoMessagingService.sendMessageToFifoQueue(queueUrl, placement);
  }

  /**
   * Synchronize the given placement.
   *
   * @param placement The placement to synchronize.
   */
  public void syncPlacement(Placement placement) {
    if (placement.getOperation().equals(DELETE)) {
      repository.deleteById(placement.getTisId());
    } else {
      repository.save(placement);
    }

    requestCacheService.deleteItemFromCache(Placement.ENTITY_NAME, placement.getTisId());
  }

  public Optional<Placement> findById(String id) {
    return repository.findById(id);
  }

  public Set<Placement> findByPostId(String postId) {
    return repository.findByPostId(postId);
  }

  public Set<Placement> findBySiteId(String siteId) {
    return repository.findBySiteId(siteId);
  }

  public Set<Placement> findByGradeId(String gradeId) {
    return repository.findByGradeId(gradeId);
  }

  /**
   * Make a request to retrieve a specific placement.
   *
   * @param id The id of the placement to be retrieved.
   */
  public void request(String id) {
    if (!requestCacheService.isItemInCache(Placement.ENTITY_NAME, id)) {
      log.info("Sending request for Placement [{}]", id);

      try {
        requestCacheService.addItemToCache(Placement.ENTITY_NAME, id,
            dataRequestService.sendRequest(Placement.ENTITY_NAME, Map.of("id", id)));
      } catch (JsonProcessingException e) {
        log.error("Error while trying to retrieve a Placement", e);
      }
    } else {
      log.debug("Already requested Placement [{}].", id);
    }
  }
}

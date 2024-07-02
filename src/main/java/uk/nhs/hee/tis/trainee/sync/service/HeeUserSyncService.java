/*
 * The MIT License (MIT)
 *
 * Copyright 2024 Crown Copyright (Health Education England)
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
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.trainee.sync.model.HeeUser;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.repository.HeeUserRepository;

@Slf4j
@Service("auth-HeeUser")
public class HeeUserSyncService implements SyncService {

  private static final String USER_NAME = "userName";

  private final HeeUserRepository repository;

  private final DataRequestService dataRequestService;

  private final RequestCacheService requestCacheService;

  private final FifoMessagingService fifoMessagingService;

  private final String queueUrl;

  HeeUserSyncService(HeeUserRepository repository, DataRequestService dataRequestService,
      FifoMessagingService fifoMessagingService,
      @Value("${application.aws.sqs.hee-user}") String queueUrl,
      RequestCacheService requestCacheService) {
    this.repository = repository;
    this.dataRequestService = dataRequestService;
    this.fifoMessagingService = fifoMessagingService;
    this.queueUrl = queueUrl;
    this.requestCacheService = requestCacheService;
  }

  @Override
  public void syncRecord(Record heeUser) {
    if (!(heeUser instanceof HeeUser)) {
      String message = String.format("Invalid record type '%s'.", heeUser.getClass());
      throw new IllegalArgumentException(message);
    }

    // Send incoming HEE user records to the HEE user queue to be processed.
    fifoMessagingService.sendMessageToFifoQueue(queueUrl, heeUser);
  }

  /**
   * Synchronize the given HEE user.
   *
   * @param heeUser The HEE user to synchronize.
   */
  public void syncHeeUser(HeeUser heeUser) {

    if (heeUser.getOperation().equals(DELETE)) {
      repository.deleteById(heeUser.getTisId());
    } else {
      repository.save(heeUser);
    }

    requestCacheService.deleteItemFromCache(HeeUser.ENTITY_NAME, heeUser.getTisId());
  }

  public Optional<HeeUser> findById(String id) {
    return repository.findById(id);
  }

  public Optional<HeeUser> findByUserName(String userName) {
    return repository.findByUserName(userName);
  }

  /**
   * Make a request to retrieve a specific HEE user.
   *
   * @param id The id of the HEE user to be retrieved.
   */
  public void request(String id) {
    if (!requestCacheService.isItemInCache(HeeUser.ENTITY_NAME, id)) {
      log.info("Sending request for HEE user [{}]", id);

      try {
        requestCacheService.addItemToCache(HeeUser.ENTITY_NAME, id,
            dataRequestService.sendRequest(HeeUser.ENTITY_NAME, Map.of("id", id)));
      } catch (JsonProcessingException e) {
        log.error("Error while trying to request a HEE user", e);
      }
    } else {
      log.debug("Already requested HEE user [{}].", id);
    }
  }
}

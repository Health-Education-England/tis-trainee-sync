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

import static uk.nhs.hee.tis.trainee.sync.event.HeeUserEventListener.HEE_USER_NAME;
import static uk.nhs.hee.tis.trainee.sync.model.Operation.DELETE;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.trainee.sync.model.HeeUser;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.repository.HeeUserRepository;

/**
 * A service for managing HEE user synchronisation.
 */
@Slf4j
@Service("auth-HeeUser")
public class HeeUserSyncService implements SyncService {

  private final HeeUserRepository repository;

  private final DataRequestService dataRequestService;

  private final RequestCacheService requestCacheService;

  HeeUserSyncService(HeeUserRepository repository, DataRequestService dataRequestService,
      RequestCacheService requestCacheService) {
    this.repository = repository;
    this.dataRequestService = dataRequestService;
    this.requestCacheService = requestCacheService;
  }

  @Override
  public void syncRecord(Record heeUser) {
    if (!(heeUser instanceof HeeUser)) {
      String message = String.format("Invalid record type '%s'.", heeUser.getClass());
      throw new IllegalArgumentException(message);
    }

    if (heeUser.getOperation().equals(DELETE)) {
      String name = heeUser.getData().get("name");
      Optional<HeeUser> heeUserOptional = findByName(name);
      heeUserOptional.ifPresent(user -> repository.deleteById(user.getTisId()));
    } else {
      repository.save((HeeUser) heeUser);
    }

    requestCacheService.deleteItemFromCache(HeeUser.ENTITY_NAME, heeUser.getTisId());
  }

  public Optional<HeeUser> findById(String id) {
    return repository.findById(id);
  }

  public Optional<HeeUser> findByName(String name) {
    return repository.findByName(name);
  }

  /**
   * Make a request to retrieve a specific HEE user.
   *
   * @param name The name of the HEE user to be retrieved.
   */
  public void request(String name) {
    if (!requestCacheService.isItemInCache(HeeUser.ENTITY_NAME, name)) {
      log.info("Sending request for HEE user [{}]", name);

      try {
        requestCacheService.addItemToCache(HeeUser.ENTITY_NAME, name,
            dataRequestService.sendRequest(HeeUser.ENTITY_NAME, Map.of(HEE_USER_NAME, name)));
      } catch (JsonProcessingException e) {
        log.error("Error while trying to request a HEE user", e);
      }
    } else {
      log.debug("Already requested HEE user [{}].", name);
    }
  }
}

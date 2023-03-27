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
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.model.Specialty;
import uk.nhs.hee.tis.trainee.sync.repository.SpecialtyRepository;

@Slf4j
@Service("tcs-Specialty")
public class SpecialtySyncService implements SyncService {

  private final SpecialtyRepository repository;

  private final DataRequestService dataRequestService;

  private final RequestCacheService requestCacheService;

  SpecialtySyncService(SpecialtyRepository repository, DataRequestService dataRequestService,
                       RequestCacheService requestCacheService) {
    this.repository = repository;
    this.dataRequestService = dataRequestService;
    this.requestCacheService = requestCacheService;
  }

  @Override
  public void syncRecord(Record specialty) {
    if (!(specialty instanceof Specialty)) {
      String message = String.format("Invalid record type '%s'.", specialty.getClass());
      throw new IllegalArgumentException(message);
    }

    if (specialty.getOperation().equals(DELETE)) {
      repository.deleteById(specialty.getTisId());
    } else {
      repository.save((Specialty) specialty);
    }

    requestCacheService.deleteItemFromCache(Specialty.ENTITY_NAME, specialty.getTisId());
  }

  public Optional<Specialty> findById(String id) {
    return repository.findById(id);
  }

  /**
   * Make a request to retrieve a specific specialty.
   *
   * @param id The id of the specialty to be retrieved.
   */
  public void request(String id) {
    if (!requestCacheService.isItemInCache(Specialty.ENTITY_NAME, id)) {
      log.info("Sending request for Specialty [{}]", id);

      try {
        requestCacheService.addItemToCache(Specialty.ENTITY_NAME, id,
            dataRequestService.sendRequest(Specialty.ENTITY_NAME, Map.of("id", id)));
      } catch (JsonProcessingException e) {
        log.error("Error while trying to request a Specialty", e);
      }
    } else {
      log.debug("Already requested Specialty [{}].", id);
    }
  }

}

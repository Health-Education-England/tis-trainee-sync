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
import static uk.nhs.hee.tis.trainee.sync.model.Operation.LOOKUP;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.Programme;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.repository.ProgrammeRepository;

@Slf4j
@Service("tcs-Programme")
public class ProgrammeSyncService implements SyncService {

  private final ProgrammeRepository repository;

  private final DataRequestService dataRequestService;

  private final RequestCacheService requestCacheService;

  private final ApplicationEventPublisher eventPublisher;

  ProgrammeSyncService(ProgrammeRepository repository, DataRequestService dataRequestService,
      RequestCacheService requestCacheService, ApplicationEventPublisher eventPublisher) {
    this.repository = repository;
    this.dataRequestService = dataRequestService;
    this.requestCacheService = requestCacheService;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public void syncRecord(Record programme) {
    if (!(programme instanceof Programme)) {
      String message = String.format("Invalid record type '%s'.", programme.getClass());
      throw new IllegalArgumentException(message);
    }

    String id = programme.getTisId();
    Operation operation = programme.getOperation();

    boolean requested = false;

    if (operation.equals(DELETE)) {
      repository.deleteById(programme.getTisId());
    } else if (operation.equals(LOOKUP)) {
      Optional<Programme> optionalProgramme = repository.findById(id);

      if (optionalProgramme.isPresent()) {
        AfterSaveEvent<Programme> event = new AfterSaveEvent<>(
            optionalProgramme.get(), null, Programme.ENTITY_NAME);
        eventPublisher.publishEvent(event);
      } else {
        request(id);
        requested = true;
      }
    } else {
      repository.save((Programme) programme);
    }

    if (!requested) {
      requestCacheService.deleteItemFromCache(Programme.ENTITY_NAME, id);
    }
  }

  public Optional<Programme> findById(String id) {
    return repository.findById(id);
  }

  public Set<Programme> findByOwner(String owner) {
    return repository.findByOwner(owner);
  }

  /**
   * Make a request to retrieve a specific programme.
   *
   * @param id The id of the programme to be retrieved.
   */
  public void request(String id) {
    if (!requestCacheService.isItemInCache(Programme.ENTITY_NAME, id)) {
      log.info("Sending request for Programme [{}]", id);

      try {
        requestCacheService.addItemToCache(Programme.ENTITY_NAME, id,
            dataRequestService.sendRequest(Programme.ENTITY_NAME, Map.of("id", id)));
      } catch (JsonProcessingException e) {
        log.error("Error while trying to request a Programme", e);
      }
    } else {
      log.debug("Already requested Programme [{}].", id);
    }
  }
}

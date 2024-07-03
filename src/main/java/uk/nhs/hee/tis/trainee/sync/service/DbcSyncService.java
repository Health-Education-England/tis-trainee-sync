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
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.trainee.sync.model.Dbc;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.repository.DbcRepository;

/**
 * A service for managing DBC synchronisation.
 */
@Slf4j
@Service("reference-Dbc")
public class DbcSyncService implements SyncService {

  private final DbcRepository repository;

  private final DataRequestService dataRequestService;

  private final ReferenceSyncService referenceSyncService;

  private final RequestCacheService requestCacheService;

  DbcSyncService(DbcRepository repository, DataRequestService dataRequestService,
      ReferenceSyncService referenceSyncService, RequestCacheService requestCacheService) {
    this.repository = repository;
    this.dataRequestService = dataRequestService;
    this.referenceSyncService = referenceSyncService;
    this.requestCacheService = requestCacheService;
  }

  @Override
  public void syncRecord(Record dbc) {
    if (!(dbc instanceof Dbc)) {
      String message = String.format("Invalid record type '%s'.", dbc.getClass());
      throw new IllegalArgumentException(message);
    }

    if (dbc.getOperation().equals(DELETE)) {
      repository.deleteById(dbc.getTisId());
    } else {
      repository.save((Dbc) dbc);
    }

    requestCacheService.deleteItemFromCache(Dbc.ENTITY_NAME, dbc.getTisId());

    // Send the record to the reference sync service to also be handled as a reference data type.
    referenceSyncService.syncRecord(dbc);
  }

  public Optional<Dbc> findById(String id) {
    return repository.findById(id);
  }

  /**
   * Make a request to retrieve a specific Dbc.
   *
   * @param id The id of the Dbc to be retrieved.
   */
  public void request(String id) {
    if (!requestCacheService.isItemInCache(Dbc.ENTITY_NAME, id)) {
      log.info("Sending request for Dbc [{}]", id);

      try {
        requestCacheService.addItemToCache(Dbc.ENTITY_NAME, id,
            dataRequestService.sendRequest("reference", Dbc.ENTITY_NAME, Map.of("id", id)));
      } catch (JsonProcessingException e) {
        log.error("Error while trying to retrieve a Dbc", e);
      }
    } else {
      log.debug("Already requested Dbc [{}].", id);
    }
  }
}

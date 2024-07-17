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
import uk.nhs.hee.tis.trainee.sync.model.LocalOffice;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.repository.LocalOfficeRepository;

/**
 * A service for managing Local Office synchronisation.
 */
@Slf4j
@Service("reference-LocalOffice")
public class LocalOfficeSyncService implements SyncService {

  private final LocalOfficeRepository repository;

  private final DataRequestService dataRequestService;

  private final ReferenceSyncService referenceSyncService;

  private final RequestCacheService requestCacheService;

  LocalOfficeSyncService(LocalOfficeRepository repository, DataRequestService dataRequestService,
      ReferenceSyncService referenceSyncService, RequestCacheService requestCacheService) {
    this.repository = repository;
    this.dataRequestService = dataRequestService;
    this.referenceSyncService = referenceSyncService;
    this.requestCacheService = requestCacheService;
  }

  @Override
  public void syncRecord(Record localOffice) {
    if (!(localOffice instanceof LocalOffice)) {
      String message = String.format("Invalid record type '%s'.", localOffice.getClass());
      throw new IllegalArgumentException(message);
    }

    if (localOffice.getOperation().equals(DELETE)) {
      repository.deleteById(localOffice.getTisId());
    } else {
      repository.save((LocalOffice) localOffice);
    }

    requestCacheService.deleteItemFromCache(LocalOffice.ENTITY_NAME, localOffice.getTisId());

    // Send the record to the reference sync service to also be handled as a reference data type.
    referenceSyncService.syncRecord(localOffice);
  }

  public Optional<LocalOffice> findById(String id) {
    return repository.findById(id);
  }

  public Optional<LocalOffice> findByAbbreviation(String abbr) {
    return repository.findByAbbreviation(abbr);
  }

  /**
   * Make a request to retrieve a specific LocalOffice.
   *
   * @param abbreviation The abbreviation of the LocalOffice to be retrieved.
   */
  public void request(String abbreviation) {
    if (!requestCacheService.isItemInCache(LocalOffice.ENTITY_NAME, abbreviation)) {
      log.info("Sending request for LocalOffice [{}]", abbreviation);

      try {
        requestCacheService.addItemToCache(LocalOffice.ENTITY_NAME, abbreviation,
            dataRequestService.sendRequest("reference", LocalOffice.ENTITY_NAME,
                Map.of("abbreviation", abbreviation)));
      } catch (JsonProcessingException e) {
        log.error("Error while trying to retrieve a LocalOffice", e);
      }
    } else {
      log.debug("Already requested LocalOffice [{}].", abbreviation);
    }
  }
}

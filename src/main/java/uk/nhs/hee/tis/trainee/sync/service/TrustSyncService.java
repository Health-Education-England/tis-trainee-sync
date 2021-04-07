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
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.model.Trust;
import uk.nhs.hee.tis.trainee.sync.repository.TrustRepository;

@Slf4j
@Service("reference-Trust")
public class TrustSyncService implements SyncService {

  private final TrustRepository repository;

  private final DataRequestService dataRequestService;

  private final Set<String> requestedIds = new HashSet<>();

  TrustSyncService(TrustRepository repository,
      DataRequestService dataRequestService) {
    this.repository = repository;
    this.dataRequestService = dataRequestService;
  }

  @Override
  public void syncRecord(Record record) {
    if (!(record instanceof Trust)) {
      String message = String.format("Invalid record type '%s'.", record.getClass());
      throw new IllegalArgumentException(message);
    }

    if (record.getOperation().equals(DELETE)) {
      repository.deleteById(record.getTisId());
    } else {
      repository.save((Trust) record);
    }

    String id = record.getTisId();
    requestedIds.remove(id);
  }

  public Optional<Trust> findById(String id) {
    return repository.findById(id);
  }

  /**
   * Make a request to retrieve a specific trust.
   *
   * @param id The id of the trust to be retrieved.
   */
  public void request(String id) {
    if (!requestedIds.contains(id)) {
      log.info("Sending request for Trust [{}]", id);

      try {
        dataRequestService.sendRequest(Trust.ENTITY_NAME, Map.of("id", id));
        requestedIds.add(id);
      } catch (JsonProcessingException e) {
        log.error("Error while trying to retrieve a Trust", e);
      }
    } else {
      log.debug("Already requested Trust [{}].", id);
    }
  }
}

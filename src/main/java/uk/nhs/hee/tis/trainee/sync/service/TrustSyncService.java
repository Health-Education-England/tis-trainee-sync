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
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.model.Trust;
import uk.nhs.hee.tis.trainee.sync.repository.TrustRepository;

@Service("reference-Trust")
public class TrustSyncService implements SyncService {

  private static final Logger LOG = LoggerFactory.getLogger(PlacementSyncService.class);
  private final TrustRepository repository;
  private MessageSendingService messageSendingService;
  TrustSyncService(TrustRepository repository,
      MessageSendingService messageSendingService) {
    this.repository = repository;
    this.messageSendingService = messageSendingService;
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
  }

  public Optional<Trust> findById(String id) {
    return repository.findById(id);
  }

  public void request(String id) {
    String table = "Trust";
    try {
      messageSendingService.sendMessage(table, id);
    } catch (JsonProcessingException e) {
      LOG.error("Error while trying to retrieve a Trust", e);
    }
    throw new UnsupportedOperationException();
  }
}

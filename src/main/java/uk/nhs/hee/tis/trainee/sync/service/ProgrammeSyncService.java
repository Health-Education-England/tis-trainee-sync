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
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.trainee.sync.model.Programme;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.repository.ProgrammeRepository;

@Slf4j
@Service("tcs-Programme")
public class ProgrammeSyncService implements SyncService {
  private static final String CACHE_KEY_PREFIX = Programme.ENTITY_NAME;
  private static final Integer REQUEST_CACHE_DB = 1;

  private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm:ss");

  private final ProgrammeRepository repository;

  private final DataRequestService dataRequestService;

  StatefulRedisConnection<String, String> connection;
  RedisCommands<String, String> syncCommands;

  ProgrammeSyncService(ProgrammeRepository repository, DataRequestService dataRequestService,
                       RedisClient redisClient) {
    this.repository = repository;
    this.dataRequestService = dataRequestService;
    connection = redisClient.connect();
    syncCommands = connection.sync();
    syncCommands.select(REQUEST_CACHE_DB);
  }

  @Override
  public void syncRecord(Record programme) {
    if (!(programme instanceof Programme)) {
      String message = String.format("Invalid record type '%s'.", programme.getClass());
      throw new IllegalArgumentException(message);
    }

    if (programme.getOperation().equals(DELETE)) {
      repository.deleteById(programme.getTisId());
    } else {
      repository.save((Programme) programme);
    }

    String id = CACHE_KEY_PREFIX + programme.getTisId();
    syncCommands.del(id);
  }

  public Optional<Programme> findById(String id) {
    return repository.findById(id);
  }


  /**
   * Make a request to retrieve a specific programme.
   *
   * @param id The id of the programme to be retrieved.
   */
  public void request(String id) {
    //if it's not in the cache, add it, and send the request; otherwise ignore
    String cachedId = CACHE_KEY_PREFIX + id;
    if (syncCommands.exists(cachedId) == 0) {
      log.info("Sending request for Programme [{}]", id);

      try {
        dataRequestService.sendRequest(Programme.ENTITY_NAME, Map.of("id", id));
        syncCommands.set(cachedId, dtf.format(LocalDateTime.now()));
      } catch (JsonProcessingException e) {
        log.error("Error while trying to request a Programme", e);
      }
    } else {
      log.debug("Already requested Programme [{}].", id);
    }
  }
}

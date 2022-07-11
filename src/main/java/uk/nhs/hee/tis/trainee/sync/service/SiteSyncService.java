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
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.trainee.sync.model.Programme;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.model.Site;
import uk.nhs.hee.tis.trainee.sync.repository.SiteRepository;

@Slf4j
@Service("reference-Site")
public class SiteSyncService extends CacheableService implements SyncService {

  private final SiteRepository repository;

  private final DataRequestService dataRequestService;

  SiteSyncService(SiteRepository repository, DataRequestService dataRequestService,
                  RedisClient redisClient) {
    super(redisClient, Site.ENTITY_NAME);
    this.repository = repository;
    this.dataRequestService = dataRequestService;
  }

  @Override
  public void syncRecord(Record site) {
    if (!(site instanceof Site)) {
      String message = String.format("Invalid record type '%s'.", site.getClass());
      throw new IllegalArgumentException(message);
    }

    if (site.getOperation().equals(DELETE)) {
      repository.deleteById(site.getTisId());
    } else {
      repository.save((Site) site);
    }

    deleteItemFromCache(site.getTisId());
  }

  public Optional<Site> findById(String id) {
    return repository.findById(id);
  }

  /**
   * Make request for the Site from the data request service.
   *
   * @param id the Site it
   */
  public void request(String id) {
    if (!isItemInCache(id)) {
      log.info("Sending request for Site [{}]", id);

      try {
        dataRequestService.sendRequest(Site.ENTITY_NAME, Map.of("id", id));
        addItemToCache(id);
      } catch (JsonProcessingException e) {
        log.error("Error while trying to request a Site", e);
      }
    } else {
      log.debug("Already requested Site [{}].", id);
    }
  }
}

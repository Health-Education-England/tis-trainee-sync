/*
 * The MIT License (MIT)
 *
 * Copyright 2023 Crown Copyright (Health Education England)
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

import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.trainee.sync.mapper.PlacementSiteMapper;
import uk.nhs.hee.tis.trainee.sync.model.PlacementSite;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.repository.PlacementSiteRepository;

@Slf4j
@Service("tcs-PlacementSite")
public class PlacementSiteSyncService implements SyncService {

  private final PlacementSiteRepository repository;
  private final PlacementSiteMapper mapper;

  PlacementSiteSyncService(PlacementSiteRepository repository, PlacementSiteMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public void syncRecord(Record placementSiteRecord) {
    if (!Objects.equals(placementSiteRecord.getTable(), PlacementSite.ENTITY_NAME)) {
      String message = String.format("Invalid record type '%s'.", placementSiteRecord.getClass());
      throw new IllegalArgumentException(message);
    }

    PlacementSite placementSite = mapper.toEntity(placementSiteRecord.getData());

    if (placementSiteRecord.getOperation().equals(DELETE)) {
      repository.deleteById(placementSite.getId());
    } else {
      repository.save(placementSite);
    }
  }

  /**
   * Find PlacementSites with the OTHER type for the given placement.
   *
   * @param placementId The placement ID to filter by.
   * @return The found PlacementSites, empty if no results.
   */
  public Set<PlacementSite> findOtherSitesByPlacementId(long placementId) {
    return repository.findOtherSitesByPlacementId(placementId);
  }
}

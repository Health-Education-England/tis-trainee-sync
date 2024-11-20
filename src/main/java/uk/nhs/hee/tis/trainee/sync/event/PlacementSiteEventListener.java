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

package uk.nhs.hee.tis.trainee.sync.event;

import static uk.nhs.hee.tis.trainee.sync.model.Operation.LOOKUP;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeDeleteEvent;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.trainee.sync.model.Placement;
import uk.nhs.hee.tis.trainee.sync.model.PlacementSite;
import uk.nhs.hee.tis.trainee.sync.service.FifoMessagingService;
import uk.nhs.hee.tis.trainee.sync.service.PlacementSiteSyncService;
import uk.nhs.hee.tis.trainee.sync.service.PlacementSyncService;

/**
 * A listener for Mongo events associated with PlacementSite data.
 */
@Slf4j
@Component
public class PlacementSiteEventListener extends AbstractMongoEventListener<PlacementSite> {

  private final PlacementSiteSyncService placementSiteService;
  private final PlacementSyncService placementService;
  private final FifoMessagingService fifoMessagingService;
  private final String placementQueueUrl;

  private final Cache cache;

  /**
   * Construct a listener for PlacementSite Mongo events.
   *
   * @param placementSiteService The placement site service.
   * @param placementService     The placement service.
   * @param fifoMessagingService The FIFO queue service for placement expansion.
   * @param placementQueueUrl    The queue to expand placements in to.
   * @param cacheManager         The cache for deleted records.
   */
  public PlacementSiteEventListener(PlacementSiteSyncService placementSiteService,
      PlacementSyncService placementService, FifoMessagingService fifoMessagingService,
      @Value("${application.aws.sqs.placement}") String placementQueueUrl,
      CacheManager cacheManager) {
    this.placementSiteService = placementSiteService;
    this.placementService = placementService;
    this.fifoMessagingService = fifoMessagingService;
    this.placementQueueUrl = placementQueueUrl;
    this.cache = cacheManager.getCache(PlacementSite.ENTITY_NAME);
  }

  /**
   * After saving a placement site the related placements should be handled.
   *
   * @param event the after-save event for the placement site.
   */
  @Override
  public void onAfterSave(AfterSaveEvent<PlacementSite> event) {
    super.onAfterSave(event);

    PlacementSite placementSite = event.getSource();
    String placementId = placementSite.getPlacementId().toString();
    Optional<Placement> optionalPlacement = placementService.findById(placementId);

    if (optionalPlacement.isPresent()) {
      log.debug("Placement {} found, queuing for re-sync.", placementId);

      // Default the placement to LOOKUP.
      Placement placement = optionalPlacement.get();
      placement.setOperation(LOOKUP);
      String deduplicationId = fifoMessagingService
          .getUniqueDeduplicationId("Placement", placement.getTisId());
      fifoMessagingService.sendMessageToFifoQueue(placementQueueUrl, placement, deduplicationId);
    } else {
      log.info("Placement {} not found, requesting data.", placementId);
      placementService.request(placementId);
    }
  }

  /**
   * Before deleting a PlacementSite, ensure it is cached.
   *
   * @param event The before-delete event for the placement site.
   */
  @Override
  public void onBeforeDelete(BeforeDeleteEvent<PlacementSite> event) {
    super.onBeforeDelete(event);
    Long id = event.getSource().getLong("_id");
    PlacementSite placementSite = cache.get(id, PlacementSite.class);

    if (placementSite == null) {
      Optional<PlacementSite> newPlacement = placementSiteService.findById(id);
      newPlacement.ifPresent(placementToCache -> cache.put(id, placementToCache));
    }
  }

  /**
   * Retrieve the deleted PlacementSite from the cache and sync the updated placement.
   *
   * @param event The after-delete event for the placement site.
   */
  @Override
  public void onAfterDelete(AfterDeleteEvent<PlacementSite> event) {
    super.onAfterDelete(event);
    Long id = event.getSource().getLong("_id");
    PlacementSite placementSite = cache.get(id, PlacementSite.class);

    if (placementSite != null) {
      String placementId = placementSite.getPlacementId().toString();
      Optional<Placement> optionalPlacement = placementService.findById(placementId);

      if (optionalPlacement.isPresent()) {
        log.debug("Placement {} found, queuing for re-sync.", placementId);

        // Default the placement to LOOKUP.
        Placement placement = optionalPlacement.get();
        placement.setOperation(LOOKUP);
        String deduplicationId = fifoMessagingService
            .getUniqueDeduplicationId("Placement", placement.getTisId());
        fifoMessagingService.sendMessageToFifoQueue(placementQueueUrl, placement, deduplicationId);
      }
    }
  }
}

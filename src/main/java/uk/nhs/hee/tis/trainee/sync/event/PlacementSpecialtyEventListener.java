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

package uk.nhs.hee.tis.trainee.sync.event;

import java.time.Instant;
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
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.Placement;
import uk.nhs.hee.tis.trainee.sync.model.PlacementSpecialty;
import uk.nhs.hee.tis.trainee.sync.service.FifoMessagingService;
import uk.nhs.hee.tis.trainee.sync.service.PlacementSpecialtySyncService;
import uk.nhs.hee.tis.trainee.sync.service.PlacementSyncService;

@Slf4j
@Component
public class PlacementSpecialtyEventListener extends
    AbstractMongoEventListener<PlacementSpecialty> {

  private static final String PLACEMENT_ID = "placementId";

  private final PlacementSyncService placementService;

  private final PlacementSpecialtySyncService placementSpecialtyService;

  private final FifoMessagingService fifoMessagingService;

  private final String placementQueueUrl;

  private final Cache cache;

  PlacementSpecialtyEventListener(PlacementSpecialtySyncService placementSpecialtyService,
      PlacementSyncService placementService,
      FifoMessagingService fifoMessagingService,
      @Value("${application.aws.sqs.placement}") String placementQueueUrl,
      CacheManager cacheManager) {
    this.placementService = placementService;
    this.placementSpecialtyService = placementSpecialtyService;
    this.fifoMessagingService = fifoMessagingService;
    this.placementQueueUrl = placementQueueUrl;
    this.cache = cacheManager.getCache(PlacementSpecialty.ENTITY_NAME);
  }

  @Override
  public void onAfterSave(AfterSaveEvent<PlacementSpecialty> event) {
    super.onAfterSave(event);

    PlacementSpecialty placementSpecialty = event.getSource();
    String placementId = placementSpecialty.getData().get(PLACEMENT_ID);

    if (placementId != null) {
      Optional<Placement> optionalPlacement = placementService.findById(placementId);
      log.debug("After placement specialty save, search for placement {} to re-sync", placementId);
      if (optionalPlacement.isPresent()) {
        // Default the placement to LOAD.
        Placement placement = optionalPlacement.get();
        log.debug("Placement {} found, queuing for re-sync.", placement);
        placement.setOperation(Operation.LOAD);
        String deduplicationId = fifoMessagingService
            .getUniqueDeduplicationId("Placement", placement.getTisId());
        fifoMessagingService.sendMessageToFifoQueue(placementQueueUrl, placement, deduplicationId);
      }
    }
  }

  /**
   * Before deleting a PlacementSpecialty, ensure it is cached.
   *
   * @param event The before-delete event for the placement specialty.
   */
  @Override
  public void onBeforeDelete(BeforeDeleteEvent<PlacementSpecialty> event) {
    super.onBeforeDelete(event);
    String id = event.getSource().get("_id").toString();
    PlacementSpecialty placementSpecialty = cache.get(id, PlacementSpecialty.class);

    if (placementSpecialty == null) {
      Optional<PlacementSpecialty> newPlacementSpecialty = placementSpecialtyService.findById(id);
      newPlacementSpecialty.ifPresent(psToCache -> cache.put(id, psToCache));
    }
  }

  /**
   * Retrieve the deleted PlacementSpecialty from the cache and sync the updated placement.
   *
   * @param event The after-delete event for the placement specialty.
   */
  @Override
  public void onAfterDelete(AfterDeleteEvent<PlacementSpecialty> event) {
    super.onAfterDelete(event);
    String id = event.getSource().get("_id").toString();
    PlacementSpecialty placementSpecialty = cache.get(id, PlacementSpecialty.class);

    if (placementSpecialty != null) {
      String placementId = placementSpecialty.getData().get(PLACEMENT_ID);
      Optional<Placement> optionalPlacement = placementService.findById(placementId);
      log.debug("After placement specialty delete, search for placement {} to re-sync.",
          placementId);
      if (optionalPlacement.isPresent()) {
        // Default the placement to LOAD.
        Placement placement = optionalPlacement.get();
        log.debug("Placement {} found, queuing for re-sync.", placement);
        placement.setOperation(Operation.LOAD);
        String deduplicationId = fifoMessagingService
            .getUniqueDeduplicationId("Placement", placement.getTisId());
        fifoMessagingService.sendMessageToFifoQueue(placementQueueUrl, placement, deduplicationId);
      }
    }
  }
}


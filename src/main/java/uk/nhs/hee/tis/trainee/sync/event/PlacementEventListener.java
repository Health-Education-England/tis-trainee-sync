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

import java.util.Optional;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeDeleteEvent;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.trainee.sync.facade.PlacementEnricherFacade;
import uk.nhs.hee.tis.trainee.sync.model.Placement;
import uk.nhs.hee.tis.trainee.sync.service.PlacementSyncService;

@Component
public class PlacementEventListener extends AbstractMongoEventListener<Placement> {

  private final PlacementEnricherFacade placementEnricher;

  private PlacementSyncService placementSyncService;

  private Cache placementCache;

  PlacementEventListener(PlacementEnricherFacade placementEnricher,
      PlacementSyncService placementSyncService,
      CacheManager cacheManager) {
    this.placementEnricher = placementEnricher;
    this.placementSyncService = placementSyncService;
    this.placementCache = cacheManager.getCache(Placement.ENTITY_NAME);
  }

  @Override
  public void onAfterSave(AfterSaveEvent<Placement> event) {
    super.onAfterSave(event);

    Placement placement = event.getSource();
    placementEnricher.enrich(placement);
  }

  /**
   * Before deleting a placement, ensure it is cached.
   *
   * @param event The before-delete event for the placement.
   */
  @Override
  public void onBeforeDelete(BeforeDeleteEvent<Placement> event) {
    String id = event.getSource().getString("_id");
    Placement placement =
        placementCache.get(id, Placement.class);
    if (placement == null) {
      Optional<Placement> newPlacement =
          placementSyncService.findById(id);
      newPlacement.ifPresent(placementToCache ->
          placementCache.put(id, placementToCache));
    }
  }

  @Override
  public void onAfterDelete(AfterDeleteEvent<Placement> event) {
    super.onAfterDelete(event);
    Placement placement =
        placementCache.get(event.getSource().getString("_id"), Placement.class);
    if (placement != null) {
      placementEnricher.delete(placement);
    }
  }
}

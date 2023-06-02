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
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeDeleteEvent;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.trainee.sync.facade.ProgrammeMembershipEnricherFacade;
import uk.nhs.hee.tis.trainee.sync.model.ProgrammeMembership;
import uk.nhs.hee.tis.trainee.sync.service.ProgrammeMembershipSyncService;

@Slf4j
@Component
public class ProgrammeMembershipEventListener
    extends AbstractMongoEventListener<ProgrammeMembership> {

  private final ProgrammeMembershipEnricherFacade programmeMembershipEnricher;

  private ProgrammeMembershipSyncService programmeMembershipSyncService;

  private Cache programmeMembershipCache;

  ProgrammeMembershipEventListener(ProgrammeMembershipEnricherFacade programmeMembershipEnricher,
      ProgrammeMembershipSyncService programmeMembershipSyncService,
      CacheManager cacheManager) {
    this.programmeMembershipEnricher = programmeMembershipEnricher;
    this.programmeMembershipSyncService = programmeMembershipSyncService;
    programmeMembershipCache = cacheManager.getCache(ProgrammeMembership.ENTITY_NAME);
  }

  @Override
  public void onAfterSave(AfterSaveEvent<ProgrammeMembership> event) {
    super.onAfterSave(event);

    ProgrammeMembership programmeMembership = event.getSource();
    programmeMembershipEnricher.enrich(programmeMembership);
  }

  /**
   * Before deleting a programme membership, ensure it is cached.
   *
   * @param event The before-delete event for the programme membership.
   *
   *              Note: if a programme membership is part of an aggregate (i.e. multiple-curricula)
   *              programme membership, then the saved (and hence cached) programme membership is
   *              the aggregate version. This will have a key like '310640,310641'. Here we cache
   *              the individual programme membership, which would have a key like '310640', so
   *              that it can be successfully retrieved in the onAfterDelete event.
   */
  @Override
  public void onBeforeDelete(BeforeDeleteEvent<ProgrammeMembership> event) {
    String id = event.getSource().get("_id", UUID.class).toString();
    ProgrammeMembership programmeMembership =
        programmeMembershipCache.get(id, ProgrammeMembership.class);
    if (programmeMembership == null) {
      Optional<ProgrammeMembership> newProgrammeMembership =
          programmeMembershipSyncService.findById(id);
      newProgrammeMembership.ifPresent(membership ->
          programmeMembershipCache.put(id, membership));
    }
  }

  @Override
  public void onAfterDelete(AfterDeleteEvent<ProgrammeMembership> event) {
    super.onAfterDelete(event);
    ProgrammeMembership programmeMembership =
        programmeMembershipCache.get(event.getSource().get("_id", UUID.class),
            ProgrammeMembership.class);
    if (programmeMembership != null) {
      programmeMembershipEnricher.delete(programmeMembership);
    }
  }
}

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
import uk.nhs.hee.tis.trainee.sync.facade.CurriculumMembershipEnricherFacade;
import uk.nhs.hee.tis.trainee.sync.model.CurriculumMembership;
import uk.nhs.hee.tis.trainee.sync.model.ProgrammeMembership;
import uk.nhs.hee.tis.trainee.sync.service.CurriculumMembershipSyncService;

@Component
public class CurriculumMembershipEventListener
    extends AbstractMongoEventListener<CurriculumMembership> {

  private final CurriculumMembershipEnricherFacade curriculumMembershipEnricher;

  private CurriculumMembershipSyncService curriculumMembershipSyncService;

  private Cache curriculumMembershipCache;

  CurriculumMembershipEventListener(CurriculumMembershipEnricherFacade curriculumMembershipEnricher,
                                    CurriculumMembershipSyncService curriculumMembershipSyncService,
                                   CacheManager cacheManager) {
    this.curriculumMembershipEnricher = curriculumMembershipEnricher;
    this.curriculumMembershipSyncService = curriculumMembershipSyncService;
    curriculumMembershipCache = cacheManager.getCache(ProgrammeMembership.ENTITY_NAME);
  }

  @Override
  public void onAfterSave(AfterSaveEvent<CurriculumMembership> event) {
    super.onAfterSave(event);

    CurriculumMembership curriculumMembership = event.getSource();
    curriculumMembershipEnricher.enrich(curriculumMembership);
  }

  /**
   * Before deleting a curriculum membership, ensure it is cached.
   *
   * @param event The before-delete event for the curriculum membership.
   *
   *              Note: if a curriculum membership is part of an aggregate (i.e. multiple-curricula)
   *              curriculum membership, then the saved (and hence cached) curriculum membership is
   *              the aggregate version. This will have a key like '310640,310641'. Here we cache
   *              the individual curriculum membership, which would have a key like '310640', so
   *              that it can be successfully retrieved in the onAfterDelete event.
   */
  @Override
  public void onBeforeDelete(BeforeDeleteEvent<CurriculumMembership> event) {
    String id = event.getSource().getString("_id");
    CurriculumMembership curriculumMembership =
        curriculumMembershipCache.get(id, CurriculumMembership.class);
    if (curriculumMembership == null) {
      Optional<CurriculumMembership> newCurriculumMembership =
          curriculumMembershipSyncService.findById(id);
      newCurriculumMembership.ifPresent(membership ->
          curriculumMembershipCache.put(id, membership));
    }
  }

  @Override
  public void onAfterDelete(AfterDeleteEvent<CurriculumMembership> event) {
    super.onAfterDelete(event);
    CurriculumMembership curriculumMembership =
        curriculumMembershipCache.get(event.getSource().getString("_id"),
            CurriculumMembership.class);
    if (curriculumMembership != null) {
      curriculumMembershipEnricher.delete(curriculumMembership);
    }
  }
}

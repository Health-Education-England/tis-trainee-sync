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

import static uk.nhs.hee.tis.trainee.sync.model.Operation.DELETE;

import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.bson.BsonString;
import org.bson.Document;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeDeleteEvent;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.trainee.sync.facade.ProgrammeMembershipEnricherFacade;
import uk.nhs.hee.tis.trainee.sync.mapper.ProgrammeMembershipMapper;
import uk.nhs.hee.tis.trainee.sync.model.ProgrammeMembership;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.service.ProgrammeMembershipSyncService;
import uk.nhs.hee.tis.trainee.sync.service.TcsSyncService;

@Slf4j
@Component
public class ProgrammeMembershipEventListener
    extends AbstractMongoEventListener<ProgrammeMembership> {

  private final ProgrammeMembershipEnricherFacade programmeMembershipEnricher;
  private final ProgrammeMembershipSyncService programmeMembershipService;
  private final TcsSyncService tcsService;
  private final ProgrammeMembershipMapper mapper;
  private final Cache cache;

  ProgrammeMembershipEventListener(ProgrammeMembershipEnricherFacade programmeMembershipEnricher,
      ProgrammeMembershipSyncService programmeMembershipService, TcsSyncService tcsService,
      ProgrammeMembershipMapper mapper, CacheManager cacheManager) {
    this.programmeMembershipEnricher = programmeMembershipEnricher;
    this.programmeMembershipService = programmeMembershipService;
    this.tcsService = tcsService;
    this.mapper = mapper;
    cache = cacheManager.getCache(ProgrammeMembership.ENTITY_NAME);
  }

  @Override
  public void onAfterSave(AfterSaveEvent<ProgrammeMembership> event) {
    super.onAfterSave(event);

    ProgrammeMembership programmeMembership = event.getSource();
    //HACK: I'm pretty sure this not how the document is supposed to be used :>
    //TODO: probably rethink this.
    Document routingDoc = event.getDocument();
    BsonString cojEvent = new BsonString(ProgrammeMembershipSyncService.COJ_EVENT_ROUTING);
    if (routingDoc == null
        || routingDoc.get("event_type") == null
        || !routingDoc.get("event_type").equals(cojEvent)) {
      programmeMembershipEnricher.enrich(programmeMembership);
    } else {
      programmeMembershipEnricher.broadcastCoj(programmeMembership);
    }
  }

  @Override
  public void onBeforeDelete(BeforeDeleteEvent<ProgrammeMembership> event) {
    super.onBeforeDelete(event);

    // Cache the existing PM if not already cached.
    UUID id = event.getSource().get("_id", UUID.class);
    ProgrammeMembership programmeMembership = cache.get(id, ProgrammeMembership.class);
    if (programmeMembership == null) {
      Optional<ProgrammeMembership> existingProgrammeMembership =
          programmeMembershipService.findById(id.toString());
      existingProgrammeMembership.ifPresent(pm -> cache.put(id, pm));
    }
  }

  @Override
  public void onAfterDelete(AfterDeleteEvent<ProgrammeMembership> event) {
    super.onAfterDelete(event);

    UUID id = event.getSource().get("_id", UUID.class);
    ProgrammeMembership programmeMembership = cache.get(id, ProgrammeMembership.class);

    if (programmeMembership != null) {
      Record programmeMembershipRecord = mapper.toRecord(programmeMembership);
      programmeMembershipRecord.setOperation(DELETE);
      programmeMembershipRecord.setSchema("tcs");
      programmeMembershipRecord.setTable("ProgrammeMembership");
      tcsService.syncRecord(programmeMembershipRecord);
    }
  }
}

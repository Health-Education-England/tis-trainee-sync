/*
 * The MIT License (MIT)
 *
 * Copyright 2024 Crown Copyright (Health Education England)
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

import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.trainee.sync.model.Dbc;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.Programme;
import uk.nhs.hee.tis.trainee.sync.service.FifoMessagingService;
import uk.nhs.hee.tis.trainee.sync.service.ProgrammeSyncService;

/**
 * A listener for Mongo events associated with DBC data.
 */
@Component
@Slf4j
public class DbcEventListener extends AbstractMongoEventListener<Dbc> {

  private static final String DBC_NAME = "name";

  private final ProgrammeSyncService programmeSyncService;

  private final FifoMessagingService fifoMessagingService;

  private final String programmeQueueUrl;

  private final Cache cache;

  DbcEventListener(ProgrammeSyncService programmeService,
      FifoMessagingService fifoMessagingService,
      @Value("${application.aws.sqs.programme}") String programmeQueueUrl,
      CacheManager cacheManager) {
    this.programmeSyncService = programmeService;
    this.fifoMessagingService = fifoMessagingService;
    this.programmeQueueUrl = programmeQueueUrl;
    cache = cacheManager.getCache(Dbc.ENTITY_NAME);
  }

  @Override
  public void onAfterSave(AfterSaveEvent<Dbc> event) {
    super.onAfterSave(event);

    Dbc dbc = event.getSource();
    cache.put(dbc.getTisId(), dbc);

    Set<Programme> programmes =
        programmeSyncService.findByOwner(dbc.getData().get(DBC_NAME));

    for (Programme programme : programmes) {
      log.debug("Dbc {} affects programme {}, "
          + "and will require related programme memberships to have RO data amended.",
          dbc.getData().get(DBC_NAME), programme.getTisId());
      // Default each message to LOAD.
      programme.setOperation(Operation.LOAD);
      String deduplicationId = fifoMessagingService
          .getUniqueDeduplicationId(Programme.ENTITY_NAME, programme.getTisId());
      fifoMessagingService.sendMessageToFifoQueue(programmeQueueUrl,
          programme, deduplicationId);
    }
  }
}

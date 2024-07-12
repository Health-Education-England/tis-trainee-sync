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

import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeDeleteEvent;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.trainee.sync.model.LocalOffice;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.Programme;
import uk.nhs.hee.tis.trainee.sync.service.FifoMessagingService;
import uk.nhs.hee.tis.trainee.sync.service.LocalOfficeSyncService;
import uk.nhs.hee.tis.trainee.sync.service.ProgrammeSyncService;

/**
 * A listener for Mongo events associated with Local office data.
 */
@Component
@Slf4j
public class LocalOfficeEventListener extends AbstractMongoEventListener<LocalOffice> {

  static final String LOCAL_OFFICE_NAME = "name";

  private final LocalOfficeSyncService localOfficeSyncService;

  private final ProgrammeSyncService programmeSyncService;

  private final FifoMessagingService fifoMessagingService;

  private final String programmeQueueUrl;

  private final Cache cache;

  LocalOfficeEventListener(LocalOfficeSyncService localOfficeSyncService,
      ProgrammeSyncService programmeService,
      FifoMessagingService fifoMessagingService,
      @Value("${application.aws.sqs.programme}") String programmeQueueUrl,
      CacheManager cacheManager) {
    this.localOfficeSyncService = localOfficeSyncService;
    this.programmeSyncService = programmeService;
    this.fifoMessagingService = fifoMessagingService;
    this.programmeQueueUrl = programmeQueueUrl;
    cache = cacheManager.getCache(LocalOffice.ENTITY_NAME);
  }

  @Override
  public void onAfterSave(AfterSaveEvent<LocalOffice> event) {
    super.onAfterSave(event);

    LocalOffice localOffice = event.getSource();
    cache.put(localOffice.getTisId(), localOffice);

    queueRelatedProgrammes(localOffice);
  }

  /**
   * Before deleting a LocalOffice, ensure it is cached.
   *
   * @param event The before-delete event for the LocalOffice.
   */
  @Override
  public void onBeforeDelete(BeforeDeleteEvent<LocalOffice> event) {
    String id = event.getSource().getString("_id");
    LocalOffice localOffice = cache.get(id, LocalOffice.class);
    if (localOffice == null) {
      Optional<LocalOffice> newLocalOffice = localOfficeSyncService.findById(id);
      newLocalOffice.ifPresent(loPresent ->
          cache.put(id, loPresent));
    }
  }

  /**
   * After delete retrieve cached values and re-sync related Programmes.
   *
   * @param event The after-delete event for the DBC.
   */
  @Override
  public void onAfterDelete(AfterDeleteEvent<LocalOffice> event) {
    super.onAfterDelete(event);
    LocalOffice localOffice = cache.get(event.getSource().getString("_id"), LocalOffice.class);
    if (localOffice != null) {
      queueRelatedProgrammes(localOffice);
    }
  }

  /**
   * Queue the programmes related to the given LocalOffice.
   *
   * @param localOffice The LocalOffice to get related programmes for.
   */
  private void queueRelatedProgrammes(LocalOffice localOffice) {
    //If the LO abbreviation changes then that could mean it links to a different DBC
    //so then the RO could change. This seems quite unlikely but needs to be handled.
    Set<Programme> programmes =
        programmeSyncService.findByOwner(localOffice.getData().get(LOCAL_OFFICE_NAME));

    for (Programme programme : programmes) {
      log.debug("LocalOffice {} affects programme {}, "
              + "and may require related programme memberships to have RO data amended.",
          localOffice.getData().get(LOCAL_OFFICE_NAME), programme.getTisId());
      // Default each message to LOAD.
      programme.setOperation(Operation.LOAD);
      String deduplicationId = fifoMessagingService
          .getUniqueDeduplicationId(Programme.ENTITY_NAME, programme.getTisId());
      fifoMessagingService.sendMessageToFifoQueue(programmeQueueUrl, programme, deduplicationId);
    }
  }
}

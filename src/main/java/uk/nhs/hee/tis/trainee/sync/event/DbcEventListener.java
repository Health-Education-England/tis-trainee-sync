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

import static uk.nhs.hee.tis.trainee.sync.event.LocalOfficeEventListener.LOCAL_OFFICE_NAME;

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
import uk.nhs.hee.tis.trainee.sync.model.Dbc;
import uk.nhs.hee.tis.trainee.sync.model.LocalOffice;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.Programme;
import uk.nhs.hee.tis.trainee.sync.service.DbcSyncService;
import uk.nhs.hee.tis.trainee.sync.service.FifoMessagingService;
import uk.nhs.hee.tis.trainee.sync.service.LocalOfficeSyncService;
import uk.nhs.hee.tis.trainee.sync.service.ProgrammeSyncService;

/**
 * A listener for Mongo events associated with DBC data.
 */
@Component
@Slf4j
public class DbcEventListener extends AbstractMongoEventListener<Dbc> {

  public static final String DBC_NAME = "name";
  public static final String DBC_ABBR = "abbr";
  public static final String DBC_TYPE = "type";
  public static final String DBC_TYPE_RELEVANT = "LETB/Deanery";

  private final DbcSyncService dbcSyncService;

  private final ProgrammeSyncService programmeSyncService;
  private final LocalOfficeSyncService localOfficeSyncService;

  private final FifoMessagingService fifoMessagingService;

  private final String programmeQueueUrl;

  private final Cache cache;

  DbcEventListener(DbcSyncService dbcSyncService, ProgrammeSyncService programmeService,
      LocalOfficeSyncService localOfficeSyncService,
      FifoMessagingService fifoMessagingService,
      @Value("${application.aws.sqs.programme}") String programmeQueueUrl,
      CacheManager cacheManager) {
    this.dbcSyncService = dbcSyncService;
    this.programmeSyncService = programmeService;
    this.localOfficeSyncService = localOfficeSyncService;
    this.fifoMessagingService = fifoMessagingService;
    this.programmeQueueUrl = programmeQueueUrl;
    cache = cacheManager.getCache(Dbc.ENTITY_NAME);
  }

  @Override
  public void onAfterSave(AfterSaveEvent<Dbc> event) {
    super.onAfterSave(event);

    Dbc dbc = event.getSource();
    cache.put(dbc.getTisId(), dbc);

    queueRelatedProgrammes(dbc);
  }

  /**
   * Before deleting a DBC, ensure it is cached.
   *
   * @param event The before-delete event for the DBC.
   */
  @Override
  public void onBeforeDelete(BeforeDeleteEvent<Dbc> event) {
    String id = event.getSource().getString("_id");
    Dbc dbc = cache.get(id, Dbc.class);
    if (dbc == null) {
      Optional<Dbc> newDbc = dbcSyncService.findById(id);
      newDbc.ifPresent(dbcPresent ->
          cache.put(id, dbcPresent));
    }
  }

  /**
   * After delete retrieve cached values and re-sync related Programmes.
   *
   * @param event The after-delete event for the DBC.
   */
  @Override
  public void onAfterDelete(AfterDeleteEvent<Dbc> event) {
    super.onAfterDelete(event);
    Dbc dbc = cache.get(event.getSource().getString("_id"), Dbc.class);
    if (dbc != null) {
      queueRelatedProgrammes(dbc);
    }
  }

  /**
   * Queue the programmes related to the given DBC.
   *
   * @param dbc The DBC to get related programmes for.
   */
  private void queueRelatedProgrammes(Dbc dbc) {
    String dbcType = dbc.getData().get(DBC_TYPE);
    if (dbcType.equalsIgnoreCase(DBC_TYPE_RELEVANT)) {
      String abbr = dbc.getData().get(DBC_ABBR);
      Optional<LocalOffice> localOfficeOptional = localOfficeSyncService.findByAbbreviation(abbr);

      if (localOfficeOptional.isEmpty()) {
        log.info("Local office {} not found, requesting data.", abbr);
        localOfficeSyncService.requestByAbbr(abbr);

      } else {

        Set<Programme> programmes =
            programmeSyncService.findByOwner(
                localOfficeOptional.get().getData().get(LOCAL_OFFICE_NAME));

        for (Programme programme : programmes) {
          log.debug("DBC / LocalOffice {} affects programme {}, "
                  + "and will require related programme memberships to have RO data amended.",
              dbc.getData().get(DBC_ABBR), programme.getTisId());
          // Default each message to LOAD.
          programme.setOperation(Operation.LOAD);
          String deduplicationId = fifoMessagingService
              .getUniqueDeduplicationId(Programme.ENTITY_NAME, programme.getTisId());
          fifoMessagingService.sendMessageToFifoQueue(programmeQueueUrl, programme,
              deduplicationId);
        }
      }
    } else {
      log.info("Ignoring DBC of irrelevant type {}.", dbcType);
    }
  }
}

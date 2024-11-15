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

package uk.nhs.hee.tis.trainee.sync.service;

import static uk.nhs.hee.tis.trainee.sync.event.DbcEventListener.DBC_NAME;
import static uk.nhs.hee.tis.trainee.sync.event.UserDesignatedBodyEventListener.DESIGNATED_BODY_CODE;
import static uk.nhs.hee.tis.trainee.sync.model.Dbc.ENTITY_NAME;
import static uk.nhs.hee.tis.trainee.sync.model.Operation.DELETE;
import static uk.nhs.hee.tis.trainee.sync.model.Operation.LOOKUP;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.trainee.sync.model.Dbc;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.model.UserDesignatedBody;
import uk.nhs.hee.tis.trainee.sync.model.UserRole;
import uk.nhs.hee.tis.trainee.sync.repository.DbcRepository;

/**
 * A service for managing DBC synchronisation.
 */
@Slf4j
@Service("reference-DBC")
public class DbcSyncService implements SyncService {

  private final DbcRepository repository;

  private final DataRequestService dataRequestService;

  private final ReferenceSyncService referenceSyncService;

  private final RequestCacheService requestCacheService;

  private final UserRoleSyncService userRoleSyncService;
  private final UserDesignatedBodySyncService userDbSyncService;
  private final ApplicationEventPublisher eventPublisher;

  DbcSyncService(DbcRepository repository, DataRequestService dataRequestService,
      ReferenceSyncService referenceSyncService, UserRoleSyncService userRoleSyncService,
      UserDesignatedBodySyncService userDbSyncService, RequestCacheService requestCacheService,
      ApplicationEventPublisher eventPublisher) {
    this.repository = repository;
    this.dataRequestService = dataRequestService;
    this.referenceSyncService = referenceSyncService;
    this.requestCacheService = requestCacheService;
    this.userRoleSyncService = userRoleSyncService;
    this.userDbSyncService = userDbSyncService;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public void syncRecord(Record dbc) {
    if (!(dbc instanceof Dbc)) {
      String message = String.format("Invalid record type '%s'.", dbc.getClass());
      throw new IllegalArgumentException(message);
    }

    Operation operation = dbc.getOperation();
    boolean requested = false;

    if (operation.equals(DELETE)) {
      repository.deleteById(dbc.getTisId());
    } else if (operation.equals(LOOKUP)) {
      Optional<Dbc> optionalDbc = repository.findById(dbc.getTisId());

      if (optionalDbc.isPresent()) {
        AfterSaveEvent<Dbc> event = new AfterSaveEvent<>(optionalDbc.get(), null, ENTITY_NAME);
        eventPublisher.publishEvent(event);
      } else {
        requestByDbc(dbc.getData().get(DESIGNATED_BODY_CODE));
        requested = true;
      }
    } else {
      repository.save((Dbc) dbc);
    }

    if (!requested) {
      requestCacheService.deleteItemFromCache(ENTITY_NAME, dbc.getTisId());
    }

    // Send the record to the reference sync service to also be handled as a reference data type.
    referenceSyncService.syncRecord(dbc);
  }

  public Optional<Dbc> findById(String id) {
    return repository.findById(id);
  }

  public Optional<Dbc> findByDbc(String dbc) {
    return repository.findByDbc(dbc);
  }

  public Optional<Dbc> findByAbbr(String abbr) {
    return repository.findByAbbr(abbr);
  }

  /**
   * Make a request to retrieve a specific Dbc.
   *
   * @param dbc The designated body code of the Dbc to be retrieved.
   */
  public void requestByDbc(String dbc) {
    request("dbc", dbc);
  }

  /**
   * Make a request to retrieve a specific Dbc.
   *
   * @param abbr The designated body abbreviation of the Dbc to be retrieved.
   */
  public void requestByAbbr(String abbr) {
    request("abbr", abbr);
  }

  /**
   * Make a request to retrieve a specific Dbc.
   *
   * @param key   The field to filter the Dbc records by.
   * @param value The field value of the Dbc to be retrieved.
   */
  private void request(String key, String value) {
    if (!requestCacheService.isItemInCache(ENTITY_NAME, value)) {
      log.info("Sending request for DBC [{}]", value);

      try {
        requestCacheService.addItemToCache(ENTITY_NAME, value,
            dataRequestService.sendRequest("reference", ENTITY_NAME, Map.of(key, value)));
      } catch (JsonProcessingException e) {
        log.error("Error while trying to retrieve a DBC", e);
      }
    } else {
      log.debug("Already requested DBC [{}].", value);
    }
  }

  /**
   * Resync the programmes related to the user's designated bodies, if the user has a Responsible
   * Officer role.
   *
   * @param userName The username to filter user designated bodies by.
   */
  public void resyncProgrammesIfUserIsResponsibleOfficer(String userName) {
    if (userName != null) {
      Optional<UserRole> userRoRole = userRoleSyncService.findRvOfficerRoleByUserName(userName);
      if (userRoRole.isPresent()) {
        log.debug("HEE user {} is a Responsible Officer, searching for related designated bodies.",
            userName);
        Set<UserDesignatedBody> userDesignatedBodies = userDbSyncService.findByUserName(userName);
        userDesignatedBodies.forEach(this::syncDesignatedBodyRelatedProgrammes);
      } else {
        log.info("User {} is not a Responsible Officer, ignoring changes.", userName);
      }
    }
  }

  /**
   * Resync the programmes related to a single user designated body, if the user has a Responsible
   * Officer role.
   *
   * @param userName           The username to filter user designated bodies by.
   * @param designatedBodyCode The designated body code to filter user deisngated bodies by.
   */
  public void resyncProgrammesForSingleDbcIfUserIsResponsibleOfficer(String userName,
      String designatedBodyCode) {
    if (userName != null && designatedBodyCode != null) {
      Optional<UserRole> userRoRole = userRoleSyncService.findRvOfficerRoleByUserName(userName);
      if (userRoRole.isPresent()) {
        log.debug("HEE user {} is a Responsible Officer, searching for related designated body {}.",
            userName, designatedBodyCode);
        Optional<UserDesignatedBody> optionalUserDesignatedBody
            = userDbSyncService.findByUserNameAndDesignatedBodyCode(userName, designatedBodyCode);
        if (optionalUserDesignatedBody.isPresent()) {
          syncDesignatedBodyRelatedProgrammes(optionalUserDesignatedBody.get());
        } else {
          log.warn("No matching user designated body found for user {} and code {}.",
              userName, designatedBodyCode);
        }
      } else {
        log.info("User {} is not a Responsible Officer, ignoring changes.", userName);
      }
    }
  }

  /**
   * Resync the programmes related to a designated body by triggering its save event.
   *
   * @param udb The user designated body to filter programmes by.
   */
  private void syncDesignatedBodyRelatedProgrammes(UserDesignatedBody udb) {
    String dbCode = udb.getData().get(DESIGNATED_BODY_CODE);
    log.debug("User designated body {} found, searching for DBC record.", dbCode);
    Optional<Dbc> optionalDbc = findByDbc(dbCode);
    if (optionalDbc.isPresent()) {
      Dbc dbc = optionalDbc.get();
      log.debug("DBC {} found, queueing related programmes for resync.",
          dbc.getData().get(DBC_NAME));
      dbc.setOperation(LOOKUP);

      //trigger the reprocessing of programmes related to the DBC.
      AfterSaveEvent<Dbc> dbcEvent = new AfterSaveEvent<>(dbc, null, ENTITY_NAME);
      eventPublisher.publishEvent(dbcEvent);
    } else {
      log.warn("No matching DBC record for {}, no further processing is possible.", dbCode);
    }
  }
}

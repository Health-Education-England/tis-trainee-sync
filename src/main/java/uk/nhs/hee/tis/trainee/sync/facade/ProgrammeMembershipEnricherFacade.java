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

package uk.nhs.hee.tis.trainee.sync.facade;

import static uk.nhs.hee.tis.trainee.sync.model.Operation.LOAD;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.apache.logging.log4j.util.Strings;
import org.mapstruct.Mapping;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.trainee.sync.mapper.util.TraineeDetailsUtil;
import uk.nhs.hee.tis.trainee.sync.model.ProgrammeMembership;
import uk.nhs.hee.tis.trainee.sync.model.Programme;
import uk.nhs.hee.tis.trainee.sync.service.ProgrammeMembershipSyncService;
import uk.nhs.hee.tis.trainee.sync.service.ProgrammeSyncService;
import uk.nhs.hee.tis.trainee.sync.service.TcsSyncService;

@Component
public class ProgrammeMembershipEnricherFacade {

  private static final String PROGRAMME_MEMBERSHIP_PROGRAMME_ID = "programmeId";

  private static final String PROGRAMME_MEMBERSHIP_DATA_PROGRAMME_NAME = "programmeName";
  private static final String PROGRAMME_MEMBERSHIP_DATA_PROGRAMME_NUMBER = "programmeNumber";
  private static final String PROGRAMME_MEMBERSHIP_DATA_MANAGING_DEANERY = "managingDeanery";

  private static final String PROGRAMME_NAME = "programmeName";
  private static final String PROGRAMME_NUMBER = "programmeNumber";
  private static final String MANAGING_DEANERY = "owner";

  private final ProgrammeMembershipSyncService programmeMembershipService;
  private final ProgrammeSyncService programmeSyncService;

  private final TcsSyncService tcsSyncService;

  ProgrammeMembershipEnricherFacade(ProgrammeMembershipSyncService programmeMembershipService, ProgrammeSyncService programmeSyncService,
                          TcsSyncService tcsSyncService) {
    this.programmeMembershipService = programmeMembershipService;
    this.programmeSyncService = programmeSyncService;
    this.tcsSyncService = tcsSyncService;
  }

  /**
   * Sync an enriched programmeMembership with the programmeMembership as the starting object.
   *
   * @param programmeMembership The programmeMembership to enrich.
   */
  public void enrich(ProgrammeMembership programmeMembership) {
    boolean doSync = true;

    String programmeId = getProgrammeId(programmeMembership);

    if (programmeId != null) {
      Optional<Programme> optionalProgramme = programmeSyncService.findById(programmeId);

      if (optionalProgramme.isPresent()) {
        doSync = enrich(programmeMembership, optionalProgramme.get());
      } else {
        programmeSyncService.request(programmeId);
        doSync = false;
      }
    }

    if (doSync) {
      syncProgrammeMembership(programmeMembership);
    }
  }

  /**
   * Enrich the programmeMembership with details from the Programme.
   *
   * @param programmeMembership The programmeMembership to enrich.
   * @param programme           The programme to enrich the programmeMembership with.
   * @return Whether enrichment was successful.
   */
  private boolean enrich(ProgrammeMembership programmeMembership, Programme programme) {

    String programmeName = getProgrammeName(programme);
    String programmeNumber = getProgrammeNumber(programme);
    String managingDeanery = getManagingDeanery(programme);

    if (programmeName != null || programmeNumber != null || managingDeanery != null) {
      populateProgrammeDetails(programmeMembership, programmeName, programmeNumber, managingDeanery);
      return true;
    }

    return false;
  }

  /**
   * Sync an enriched programmeMembership with the associated programme as the starting point.
   *
   * @param programme The programme triggering programme membership enrichment.
   */
  public void enrich(Programme programme) {
    enrich(programme, null, null, null);
  }

  /**
   * Enrich the programmeMembership with the given name, number and owner and then sync it.
   *
   * @param programmeMembership The programmeMembership to sync.
   * @param programmeName       The programme name to enrich with.
   */
  private void populateProgrammeDetails(ProgrammeMembership programmeMembership, String programmeName,
                                        String programmeNumber, String managingDeanery) {
    // Add extra data to programmeMembership data.
    if (Strings.isNotBlank(programmeName)) {
      programmeMembership.getData().put(PROGRAMME_MEMBERSHIP_DATA_PROGRAMME_NAME, programmeName);
    }
    if (Strings.isNotBlank(programmeNumber)) {
      programmeMembership.getData().put(PROGRAMME_MEMBERSHIP_DATA_PROGRAMME_NUMBER, programmeNumber);
    }
    if (Strings.isNotBlank(managingDeanery)) {
      programmeMembership.getData().put(PROGRAMME_MEMBERSHIP_DATA_MANAGING_DEANERY, managingDeanery);
    }
  }

  /**
   * Sync the (completely enriched) programmeMembership.
   *
   * @param programmeMembership The programmeMembership to sync.
   */

  private void syncProgrammeMembership(ProgrammeMembership programmeMembership) {
    // Set the required metadata so the record can be synced using common logic.
    programmeMembership.setOperation(LOAD);
    programmeMembership.setSchema("tcs");
    programmeMembership.setTable("ProgrammeMembership");

    tcsSyncService.syncRecord(programmeMembership);
  }

  /**
   * Enrich programmeMemberships associated with the Programme with the given programme name, number and owner.
   * If any of these are null they will be queried for.
   *
   * @param programme       The programme to get associated programmeMemberships from.
   * @param programmeName   The programme name to enrich with.
   * @param programmeNumber The programme number to enrich with.
   * @param managingDeanery The managing deanery to enrich with.
   */
  private void enrich(Programme programme, @Nullable String programmeName, @Nullable String programmeNumber,
                      @Nullable String managingDeanery) {

    if (programmeName == null) {
      programmeName = getProgrammeName(programme);
    }
    if (programmeNumber == null) {
      programmeNumber = getProgrammeNumber(programme);
    }
    if (managingDeanery == null) {
      managingDeanery = getManagingDeanery(programme);
    }

    if (programmeName != null || programmeNumber != null || managingDeanery != null) {
      String id = programme.getTisId();
      Set<ProgrammeMembership> programmeMemberships = programmeMembershipService.findByProgrammeId(id);

      final String finalProgrammeName = programmeName;
      final String finalProgrammeNumber = programmeNumber;
      final String finalManagingDeanery = managingDeanery;

      programmeMemberships.forEach(
          programmeMembership -> {
            populateProgrammeDetails(programmeMembership, finalProgrammeName, finalProgrammeNumber,
                finalManagingDeanery);
            syncProgrammeMembership(programmeMembership);
          }
      );
    }
  }

  /**
   * Get the Programme Name for the programme.
   *
   * @param programme The programme to get the name from.
   * @return The programme name.
   */
  private String getProgrammeName(Programme programme) {
    return programme.getData().get(PROGRAMME_NAME);
  }

  /**
   * Get the Programme Number for the programme.
   *
   * @param programme The programme to get the number from.
   * @return The programme number.
   */
  private String getProgrammeNumber(Programme programme) {
    return programme.getData().get(PROGRAMME_NUMBER);
  }

  /**
   * Get the Managing Deanery for the programme.
   *
   * @param programme The programme to get the owner from.
   * @return The managingDeanery.
   */
  private String getManagingDeanery(Programme programme) {
    return programme.getData().get(MANAGING_DEANERY);
  }

  /**
   * Get the Programme ID from the programmeMembership.
   *
   * @param programmeMembership The programmeMembership to get the programme id from.
   * @return The programme id.
   */
  private String getProgrammeId(ProgrammeMembership programmeMembership) {
    return programmeMembership.getData().get(PROGRAMME_MEMBERSHIP_PROGRAMME_ID);
  }

}

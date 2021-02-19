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

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.logging.log4j.util.Strings;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.trainee.sync.model.Post;
import uk.nhs.hee.tis.trainee.sync.model.ProgrammeMembership;
import uk.nhs.hee.tis.trainee.sync.model.Programme;
import uk.nhs.hee.tis.trainee.sync.service.ProgrammeMembershipSyncService;
import uk.nhs.hee.tis.trainee.sync.service.ProgrammeSyncService;
import uk.nhs.hee.tis.trainee.sync.service.TcsSyncService;

@Component
public class ProgrammeMembershipEnricherFacade {

  private static final String PROGRAMME_MEMBERSHIP_PROGRAMME_ID = "programmeId";
  private static final String PROGRAMME_MEMBERSHIP_DATA_TRAINING_BODY_NAME = "trainingBodyName";
  private static final String PROGRAMME_MEMBERSHIP_DATA_PROGRAMME_NAME = "programmeName"; // TODO should be 'name'? (need to fix dto etc.)
  private static final String PROGRAMME_NAME = "programmeName";

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


    // TODO this flow seems weird - sync'd twice??
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
   * @param programme      The programme to enrich the programmeMembership with.
   * @return Whether enrichment was successful.
   */
  private boolean enrich(ProgrammeMembership programmeMembership, Programme programme) {

    String programmeName = getProgrammeName(programme);
    // String siteLocation = getSiteLocation(site); TODO more attributes

    if (programmeName != null) {
      populateProgrammeDetails(programmeMembership, programmeName);
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
    enrich(programme, null);
  }

  /**
   * Enrich the programmeMembership with the given TODO details and then sync it.
   *
   * @param programmeMembership         The programmeMembership to sync.
   * @param programmeName The programme name to enrich with.
   */
  private void populateProgrammeDetails(ProgrammeMembership programmeMembership, String programmeName) {
    // Add extra data to programmeMembership data.
    if (Strings.isNotBlank(programmeName)) {
      programmeMembership.getData().put(PROGRAMME_MEMBERSHIP_DATA_PROGRAMME_NAME, programmeName);
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
   * Enrich programmeMemberships associated with the Programme with the given programme name TODO etc, if these are
   * null they will be queried for.
   *
   * @param programme         The programme to get associated programmeMemberships from.
   * @param programmeName     The programme name to enrich with.
   */
  private void enrich(Programme programme, @Nullable String programmeName) {

    if (programmeName == null) {
      programmeName = getProgrammeName(programme);
    }

    if (programmeName != null) {
      String id = programme.getTisId();
      Set<ProgrammeMembership> programmeMemberships = programmeMembershipService.findByProgrammeId(id);

      final String finalProgrammeName = programmeName;

      programmeMemberships.forEach(
          programmeMembership -> {
            populateProgrammeDetails(programmeMembership, finalProgrammeName);
            enrich(programmeMembership);
          }
      );
    }
  }



  /**
   * Get the name for the programme.
   *
   * @param programme The programme to get the name from.
   * @return The name.
   */
  private String getProgrammeName(Programme programme) {
    return programme.getData().get(PROGRAMME_NAME);
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

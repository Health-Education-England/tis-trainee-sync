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

import java.util.*;

import org.apache.logging.log4j.util.Strings;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.trainee.sync.model.*;
import uk.nhs.hee.tis.trainee.sync.service.CurriculumSyncService;
import uk.nhs.hee.tis.trainee.sync.service.ProgrammeMembershipSyncService;
import uk.nhs.hee.tis.trainee.sync.service.ProgrammeSyncService;
import uk.nhs.hee.tis.trainee.sync.service.TcsSyncService;

@Component
public class ProgrammeMembershipEnricherFacade {

  private static final String PROGRAMME_MEMBERSHIP_PROGRAMME_ID = "programmeId";
  private static final String PROGRAMME_MEMBERSHIP_CURRICULUM_ID = "curriculumId";

  private static final String PROGRAMME_MEMBERSHIP_DATA_PROGRAMME_NAME = "programmeName";
  private static final String PROGRAMME_MEMBERSHIP_DATA_PROGRAMME_TIS_ID = "programmeTisId";
  private static final String PROGRAMME_MEMBERSHIP_DATA_PROGRAMME_NUMBER = "programmeNumber";
  private static final String PROGRAMME_MEMBERSHIP_DATA_MANAGING_DEANERY = "managingDeanery";
  private static final String PROGRAMME_MEMBERSHIP_DATA_CURRICULA = "curricula"; // TODO temporary

  private static final String PROGRAMME_NAME = "programmeName";
  private static final String PROGRAMME_TIS_ID = "programmeId";
  private static final String PROGRAMME_NUMBER = "programmeNumber";
  private static final String MANAGING_DEANERY = "owner";

  private static final String CURRICULUM_NAME = "name";
  private static final String CURRICULUM_SUB_TYPE = "curriculumSubType";
  private static final String CURRICULUM_START_DATE = "curriculumStartDate"; // TODO this is from programme membership

  private final ProgrammeMembershipSyncService programmeMembershipService;
  private final ProgrammeSyncService programmeSyncService;
  private final CurriculumSyncService curriculumSyncService;

  private final TcsSyncService tcsSyncService;

  ProgrammeMembershipEnricherFacade(ProgrammeMembershipSyncService programmeMembershipService,
                                    ProgrammeSyncService programmeSyncService,
                                    CurriculumSyncService curriculumSyncService,
                                    TcsSyncService tcsSyncService) {
    this.programmeMembershipService = programmeMembershipService;
    this.programmeSyncService = programmeSyncService;
    this.curriculumSyncService = curriculumSyncService;
    this.tcsSyncService = tcsSyncService;
  }

  /**
   * Sync an enriched programmeMembership with the programmeMembership as the starting object.
   *
   * @param programmeMembership The programmeMembership to enrich.
   */
  public void enrich(ProgrammeMembership programmeMembership) {
    enrich(programmeMembership, true, true);
  }

  /**
   * Sync an enriched programmeMembership with the associated programme as the starting point.
   *
   * @param programme The programme triggering programme membership enrichment.
   */
  public void enrich(Programme programme) {
    enrich(programme, null, null,null, null);
  }

  /**
   * Sync an enriched programmeMembership with the associated curriculum as the starting point.
   *
   * @param curriculum The curriculum triggering programme membership enrichment.
   */
  public void enrich(Curriculum curriculum) {
    enrich(curriculum, null, null, null);
  }

  private void enrich(ProgrammeMembership programmeMembership, boolean doProgrammeEnrich, boolean doCurriculumEnrich) {
    boolean doSync = true;

    if (doProgrammeEnrich) {
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
    }

    if (doCurriculumEnrich) {
      String curriculumId = getCurriculumId(programmeMembership);

      if (curriculumId != null) {
        Optional<Curriculum> optionalCurriculum = curriculumSyncService.findById(curriculumId);

        if (optionalCurriculum.isPresent()) {
          doSync &= enrich(programmeMembership, optionalCurriculum.get());
        } else {
          curriculumSyncService.request(curriculumId);
          doSync = false;
        }
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
    String programmeTisId = getProgrammeTisId(programme);
    String programmeNumber = getProgrammeNumber(programme);
    String managingDeanery = getManagingDeanery(programme);

    if (programmeName != null || programmeTisId != null || programmeNumber != null || managingDeanery != null) {
      populateProgrammeDetails(programmeMembership, programmeName, programmeTisId, programmeNumber, managingDeanery);
      return true;
    }

    return false;
  }

  /**
   * Enrich the programmeMembership with details from the Curriculum.
   *
   * @param programmeMembership The programmeMembership to enrich.
   * @param curriculum           The curriculum to enrich the programmeMembership with.
   * @return Whether enrichment was successful.
   */
  private boolean enrich(ProgrammeMembership programmeMembership, Curriculum curriculum) {

    String curriculumName = getCurriculumName(curriculum);
    // TODO

    if (curriculumName != null) {
      populateCurriculumDetails(programmeMembership, null, curriculumName, null, null);
      return true;
    }

    return false;
  }



  /**
   * Enrich the programmeMembership with the given programme name, TIS ID, number and managing deanery and then sync it.
   *
   * @param programmeMembership The programmeMembership to sync.
   * @param programmeName       The programme name to enrich with.
   * @param programmeTisId      The programme TIS ID to enrich with.
   * @param programmeName       The programme name to enrich with.
   * @param programmeNumber     The programme number to enrich with.
   * @param managingDeanery     The managing deanery to enrich with.
   */
  private void populateProgrammeDetails(ProgrammeMembership programmeMembership, String programmeName,
                                        String programmeTisId, String programmeNumber, String managingDeanery) {
    // Add extra data to programmeMembership data.
    if (Strings.isNotBlank(programmeName)) {
      programmeMembership.getData().put(PROGRAMME_MEMBERSHIP_DATA_PROGRAMME_NAME, programmeName);
    }
    if (Strings.isNotBlank(programmeTisId)) {
      programmeMembership.getData().put(PROGRAMME_MEMBERSHIP_DATA_PROGRAMME_TIS_ID, programmeTisId);
    }
    if (Strings.isNotBlank(programmeNumber)) {
      programmeMembership.getData().put(PROGRAMME_MEMBERSHIP_DATA_PROGRAMME_NUMBER, programmeNumber);
    }
    if (Strings.isNotBlank(managingDeanery)) {
      programmeMembership.getData().put(PROGRAMME_MEMBERSHIP_DATA_MANAGING_DEANERY, managingDeanery);
    }
  }

  /**
   * Enrich the programmeMembership with the given programme name, TIS ID, number and managing deanery and then sync it.
   *
   * @param programmeMembership The programmeMembership to sync.
   * @param curriculumName       The curriculum name to enrich with.

   */
  private void populateCurriculumDetails(ProgrammeMembership programmeMembership, String curriculumTisId,
                                         String curriculumName, String CurriculumSubType, String CurriculumStartDate) {
    // Add extra data to programmeMembership data.
    if (Strings.isNotBlank(curriculumName)) {
      Map<String,String> c = new HashMap<String, String>();
      c.put("CurriculumName", curriculumName);
      c.put("dummy", "data");
      // etc.
      ArrayList<Map<String,String>> curricula = new ArrayList<>();
      curricula.add(c);
      programmeMembership.getData().put(PROGRAMME_MEMBERSHIP_DATA_CURRICULA, String.valueOf(curricula));
    }
    // TODO - *** - maybe rework this - should build array of curricula, but the record object expects a string....
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

    // TODO build aggregate programmeMembershipX entry from all PMs where personId, programmeTisId, startDate,
    // endDate and programmeMembershipType are equal to programmeMembership details:
    // programmeMembershipX is the same as programmeMembership, except
    // tisId = null
    // programmeCompletionDate =  max(programmeCompletionDate)
    // curricula = set of all curricula attached
    // programmeMembership.setTisId(null);


    tcsSyncService.syncRecord(programmeMembership);
  }

  /**
   * Enrich programmeMemberships associated with the Programme with the given programme name, TIS ID, number and owner.
   * If any of these are null they will be queried for.
   *
   * @param programme       The programme to get associated programmeMemberships from.
   * @param programmeName   The programme name to enrich with.
   * @param programmeTisId  The programme TIS ID to enrich with.
   * @param programmeNumber The programme number to enrich with.
   * @param managingDeanery The managing deanery to enrich with.
   */
  private void enrich(Programme programme, @Nullable String programmeName, @Nullable String programmeTisId,
                      @Nullable String programmeNumber, @Nullable String managingDeanery) {

    if (programmeName == null) {
      programmeName = getProgrammeName(programme);
    }
    if (programmeTisId == null) {
      programmeTisId = getProgrammeTisId(programme);
    }
    if (programmeNumber == null) {
      programmeNumber = getProgrammeNumber(programme);
    }
    if (managingDeanery == null) {
      managingDeanery = getManagingDeanery(programme);
    }

    if (programmeName != null || programmeTisId != null || programmeNumber != null || managingDeanery != null) {
      String id = programme.getTisId();
      Set<ProgrammeMembership> programmeMemberships = programmeMembershipService.findByProgrammeId(id);

      final String finalProgrammeName = programmeName;
      final String finalProgrammeTisId = programmeTisId;
      final String finalProgrammeNumber = programmeNumber;
      final String finalManagingDeanery = managingDeanery;

      programmeMemberships.forEach(
          programmeMembership -> {
            populateProgrammeDetails(programmeMembership, finalProgrammeName, finalProgrammeTisId, finalProgrammeNumber,
                finalManagingDeanery);
            enrich(programmeMembership, true, true);
          }
      );
    }
  }

  /**
   * Enrich programmeMemberships associated with the Curriculum with the given curriculum details.
   * If any of these are null they will be queried for.
   *
   * @param curriculum          The curriculum to get associated programmeMemberships from.
   * @param curriculumName      The curriculum name to enrich with.
   * @param curriculumSubType   The curriculum subtype to enrich with.
   * @param curriculumStartDate The curriculum start date to enrich with.
   */
  private void enrich(Curriculum curriculum, @Nullable String curriculumName, @Nullable String curriculumSubType,
                      @Nullable String curriculumStartDate) {

    if (curriculumName == null) {
      curriculumName = getCurriculumName(curriculum);
    }
    if (curriculumSubType == null) {
      curriculumSubType = getCurriculumSubType(curriculum);
    }
    if (curriculumStartDate == null) {
      curriculumStartDate = getCurriculumStartDate(curriculum);
    }
    // TODO: tisId?

    if (curriculumName != null || curriculumSubType != null || curriculumStartDate != null) {
      String id = curriculum.getTisId();
      Set<ProgrammeMembership> programmeMemberships = programmeMembershipService.findByCurriculumId(id);

      final String finalCurriculumTisId = id;
      final String finalCurriculumName = curriculumName;
      final String finalCurriculumSubType = curriculumSubType;
      final String finalCurriculumStartDate = curriculumStartDate;

      programmeMemberships.forEach(
          programmeMembership -> {
            // TODO: this will be a bit different because push into collection, right?
            populateCurriculumDetails(programmeMembership, finalCurriculumTisId, finalCurriculumName, finalCurriculumSubType,
                finalCurriculumStartDate);
            enrich(programmeMembership, true, true); // TODO doProgrammeEnrich should be false...?
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
   * Get the Programme TIS ID for the programme.
   *
   * @param programme The programme to get the TIS ID from.
   * @return The programme TIS ID.
   */
  private String getProgrammeTisId(Programme programme) {
    return programme.getData().get(PROGRAMME_TIS_ID);
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
   * @return The managing deanery.
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

  /**
   * Get the Curriculum ID from the programmeMembership.
   *
   * @param programmeMembership The programmeMembership to get the curriculum id from.
   * @return The programme id.
   */
  private String getCurriculumId(ProgrammeMembership programmeMembership) {
    return programmeMembership.getData().get(PROGRAMME_MEMBERSHIP_CURRICULUM_ID);
  }

  /**
   * Get the Name for the curriculum.
   *
   * @param curriculum The curriculum to get the name from.
   * @return The curriculum name.
   */
  private String getCurriculumName(Curriculum curriculum) {
    return curriculum.getData().get(CURRICULUM_NAME);
  }

  /**
   * Get the SubType for the curriculum.
   *
   * @param curriculum The curriculum to get the name from.
   * @return The curriculum subtype.
   */
  private String getCurriculumSubType(Curriculum curriculum) {
    return curriculum.getData().get(CURRICULUM_SUB_TYPE);
  }

  /**
   * Get the StartingDate for the curriculum.
   *
   * @param curriculum The curriculum to get the starting date from.
   * @return The curriculum starting date.
   */
  private String getCurriculumStartDate(Curriculum curriculum) {
    return curriculum.getData().get(CURRICULUM_START_DATE);
  }

}

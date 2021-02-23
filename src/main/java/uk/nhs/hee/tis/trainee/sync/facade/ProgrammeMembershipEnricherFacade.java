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

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.util.Strings;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.trainee.sync.model.Curriculum;
import uk.nhs.hee.tis.trainee.sync.model.Programme;
import uk.nhs.hee.tis.trainee.sync.model.ProgrammeMembership;
import uk.nhs.hee.tis.trainee.sync.service.CurriculumSyncService;
import uk.nhs.hee.tis.trainee.sync.service.ProgrammeMembershipSyncService;
import uk.nhs.hee.tis.trainee.sync.service.ProgrammeSyncService;
import uk.nhs.hee.tis.trainee.sync.service.TcsSyncService;

@Component
public class ProgrammeMembershipEnricherFacade {

  private static final String PROGRAMME_MEMBERSHIP_PROGRAMME_ID = "programmeId";
  private static final String PROGRAMME_MEMBERSHIP_CURRICULUM_ID = "curriculumId";
  private static final String PROGRAMME_MEMBERSHIP_PERSON_ID = "personId";
  private static final String PROGRAMME_MEMBERSHIP_PROGRAMME_MEMBERSHIP_TYPE = "programmeMembershipType";
  private static final String PROGRAMME_MEMBERSHIP_PROGRAMME_COMPLETION_DATE = "programmeCompletionDate";
  private static final String PROGRAMME_MEMBERSHIP_PROGRAMME_START_DATE = "programmeStartDate";
  private static final String PROGRAMME_MEMBERSHIP_PROGRAMME_END_DATE = "programmeEndDate";
  private static final String PROGRAMME_MEMBERSHIP_CURRICULA = "curricula";

  private static final String PROGRAMME_MEMBERSHIP_DATA_PROGRAMME_NAME = "programmeName";
  private static final String PROGRAMME_MEMBERSHIP_DATA_PROGRAMME_TIS_ID = "programmeTisId";
  private static final String PROGRAMME_MEMBERSHIP_DATA_PROGRAMME_NUMBER = "programmeNumber";
  private static final String PROGRAMME_MEMBERSHIP_DATA_MANAGING_DEANERY = "managingDeanery";

  private static final String PROGRAMME_NAME = "programmeName";
  private static final String PROGRAMME_TIS_ID = "programmeId";
  private static final String PROGRAMME_NUMBER = "programmeNumber";
  private static final String MANAGING_DEANERY = "owner";

  private static final String CURRICULUM_DATA_TIS_ID = "id";
  private static final String CURRICULUM_DATA_NAME = "curriculumName";
  private static final String CURRICULUM_DATA_SUB_TYPE = "curriculumSubType";
  private static final String CURRICULUM_DATA_START_DATE = "curriculumStartDate"; // note this is from programme membership

  private static final String CURRICULUM_TIS_ID = "id";
  private static final String CURRICULUM_NAME = "name";
  private static final String CURRICULUM_SUB_TYPE = "curriculumSubType";
  private static final String CURRICULUM_START_DATE = "curriculumStartDate"; // note this is from programme membership

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
    enrich(curriculum, null, null);
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
    String curriculumTisId = getCurriculumTisId(curriculum);
    String curriculumSubType = getCurriculumSubType(curriculum);

    if (curriculumName != null) {
      populateCurriculumDetails(programmeMembership, curriculumTisId, curriculumName, curriculumSubType);
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
                                         String curriculumName, String curriculumSubType) {
    // Add extra data to programmeMembership data. This is unpacked again in
    // syncProgrammeMembership(ProgrammeMembership programmeMembership)
    // to derive the aggregate programmeMembership record.
    if (Strings.isNotBlank(curriculumName)) {
      Map<String,String> c = new HashMap<>();
      c.put(CURRICULUM_DATA_NAME, curriculumName);
      c.put(CURRICULUM_DATA_TIS_ID, curriculumTisId);
      c.put(CURRICULUM_DATA_SUB_TYPE, curriculumSubType);
      c.put(CURRICULUM_DATA_START_DATE, getCurriculumStartDate(programmeMembership));

      Set<Map<String,String>> curricula = new HashSet<>();
      curricula.add(c);

      String curriculaJSON = getCurriculaJSON(curricula);

      programmeMembership.getData().put(PROGRAMME_MEMBERSHIP_CURRICULA, curriculaJSON);
    }
  }

  /**
   * Sync the (completely enriched) programmeMembership, aggregating it with similar programmeMemberships
   *
   * @param programmeMembership The programmeMembership to sync.
   *
   * Note: 'similar' is defined as sharing the same personId, programmeId, programmeStartDate, programmeEndDate and
   *                            programmeMembershipType.
   */

  private void syncProgrammeMembership(ProgrammeMembership programmeMembership) {
    // Set the required metadata so the record can be synced using common logic.
    programmeMembership.setOperation(LOAD);
    programmeMembership.setSchema("tcs");
    programmeMembership.setTable("ProgrammeMembership");

    // build aggregate programmeMembership entry from all similar programmeMemberships

    String personId = getPersonId(programmeMembership);
    String programmeId = getProgrammeId(programmeMembership);
    String programmeMembershipType = getProgrammeMembershipType(programmeMembership);
    String programmeStartDate = getProgrammeStartDate(programmeMembership);
    String programmeEndDate = getProgrammeEndDate(programmeMembership);

    Set<ProgrammeMembership> programmeMemberships = programmeMembershipService.findByPersonIdAndProgrammeIdAndProgrammeMembershipTypeAndProgrammeStartDateAndProgrammeEndDate(personId, programmeId, programmeMembershipType, programmeStartDate, programmeEndDate);

    ProgrammeMembership aggregateProgrammeMembership = new ProgrammeMembership();
    aggregateProgrammeMembership.setData(programmeMembership.getData());
    aggregateProgrammeMembership.setMetadata(programmeMembership.getMetadata());
    aggregateProgrammeMembership.setTable(programmeMembership.getTable());
    aggregateProgrammeMembership.setSchema(programmeMembership.getSchema());
    aggregateProgrammeMembership.setOperation(programmeMembership.getOperation());

    aggregateProgrammeMembership.setTisId(null);

    String programmeCompletionDate = getProgrammeCompletionDate(programmeMembership);
    Set<String> tisIds = new HashSet<>();
    tisIds.add(programmeMembership.getTisId());
    LocalDate maxProgrammeCompletionDate = programmeCompletionDate == null ? null : LocalDate.parse(programmeCompletionDate);

    Set<Map<String, String>> allCurricula = getCurricula(programmeMembership);
    boolean doSync = true;

    for (ProgrammeMembership thisProgrammeMembership : programmeMemberships) {

      tisIds.add(thisProgrammeMembership.getTisId());

      String thisProgrammeCompletionDateString = getProgrammeCompletionDate(thisProgrammeMembership);
      if (thisProgrammeCompletionDateString != null) {
        LocalDate thisProgrammeCompletionDate = LocalDate.parse(thisProgrammeCompletionDateString);
        if (maxProgrammeCompletionDate == null) {
          maxProgrammeCompletionDate = thisProgrammeCompletionDate;
        } else {
          if (thisProgrammeCompletionDate.isAfter(maxProgrammeCompletionDate)) {
            maxProgrammeCompletionDate = thisProgrammeCompletionDate;
          }
        }
      }

      String curriculumId = getCurriculumId(thisProgrammeMembership);
      if ( curriculumId != null) {
        Optional<Curriculum> optionalCurriculum = curriculumSyncService.findById(curriculumId);

        if (optionalCurriculum.isPresent()) {
          enrich(thisProgrammeMembership, optionalCurriculum.get());
        } else {
          curriculumSyncService.request(curriculumId);
          doSync = false;
          // cannot sync this record because all the related curriculum data is not available
        }
      }
      Set<Map<String, String>> thisCurricula = getCurricula(thisProgrammeMembership);
      allCurricula.addAll(thisCurricula);
    }

    List<String> sortedTisIds = new ArrayList<>(tisIds);
    Collections.sort(sortedTisIds);
    String allSortedTisIds = String.join(",", sortedTisIds);
    String hashTisIds = "";
    try {
      byte[] bytesOfMessage = allSortedTisIds.getBytes(StandardCharsets.UTF_8);
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] digest = md.digest(bytesOfMessage);
      BigInteger bigInt = new BigInteger(1,digest);
      hashTisIds = bigInt.toString(16);
    } catch (Exception e) {
      hashTisIds = "hash failed"; // should never happen?
    }
    aggregateProgrammeMembership.setTisId(hashTisIds);

    aggregateProgrammeMembership.getData().put(PROGRAMME_MEMBERSHIP_PROGRAMME_COMPLETION_DATE, String.valueOf(maxProgrammeCompletionDate));

    String allCurriculaJSON = getCurriculaJSON(allCurricula);
    aggregateProgrammeMembership.getData().put(PROGRAMME_MEMBERSHIP_CURRICULA, allCurriculaJSON);

    if (Boolean.TRUE.equals(doSync)) {
      tcsSyncService.syncRecord(aggregateProgrammeMembership);
    }
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
   */
  private void enrich(Curriculum curriculum, @Nullable String curriculumName, @Nullable String curriculumSubType) {

    if (curriculumName == null) {
      curriculumName = getCurriculumName(curriculum);
    }
    if (curriculumSubType == null) {
      curriculumSubType = getCurriculumSubType(curriculum);
    }
    if (curriculumSubType == null) {
      curriculumSubType = getCurriculumSubType(curriculum);
    }

    if (curriculumName != null || curriculumSubType != null) {
      String id = curriculum.getTisId();
      Set<ProgrammeMembership> programmeMemberships = programmeMembershipService.findByCurriculumId(id);

      final String finalCurriculumTisId = id;
      final String finalCurriculumName = curriculumName;
      final String finalCurriculumSubType = curriculumSubType;

      programmeMemberships.forEach(
          programmeMembership -> {
            populateCurriculumDetails(programmeMembership, finalCurriculumTisId, finalCurriculumName, finalCurriculumSubType);
            enrich(programmeMembership, true, true);
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
   * Get the Person ID from the programmeMembership.
   *
   * @param programmeMembership The programmeMembership to get the person id from.
   * @return The person id.
   */
  private String getPersonId(ProgrammeMembership programmeMembership) {
    return programmeMembership.getData().get(PROGRAMME_MEMBERSHIP_PERSON_ID);
  }

  /**
   * Get the Programme Membership Type from the programmeMembership.
   *
   * @param programmeMembership The programmeMembership to get the membership type from.
   * @return The programme membership type.
   */
  private String getProgrammeMembershipType(ProgrammeMembership programmeMembership) {
    return programmeMembership.getData().get(PROGRAMME_MEMBERSHIP_PROGRAMME_MEMBERSHIP_TYPE);
  }

  /**
   * Get the Programme Start Date from the programmeMembership.
   *
   * @param programmeMembership The programmeMembership to get the start date from.
   * @return The programme start date.
   */
  private String getProgrammeStartDate(ProgrammeMembership programmeMembership) {
    return programmeMembership.getData().get(PROGRAMME_MEMBERSHIP_PROGRAMME_START_DATE);
  }

  /**
   * Get the Programme End Date from the programmeMembership.
   *
   * @param programmeMembership The programmeMembership to get the end date from.
   * @return The programme end date.
   */
  private String getProgrammeEndDate(ProgrammeMembership programmeMembership) {
    return programmeMembership.getData().get(PROGRAMME_MEMBERSHIP_PROGRAMME_END_DATE);
  }

  /**
   * Get the Programme Completion Date from the programmeMembership.
   *
   * @param programmeMembership The programmeMembership to get the membership type from.
   * @return The programme membership type.
   */
  private String getProgrammeCompletionDate(ProgrammeMembership programmeMembership) {
    return programmeMembership.getData().get(PROGRAMME_MEMBERSHIP_PROGRAMME_COMPLETION_DATE);
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
   * Get the Curricula from the programmeMembership.
   *
   * @param programmeMembership The programmeMembership to get the curricula from.
   * @return The curricula.
   */
  private Set<Map<String,String>> getCurricula(ProgrammeMembership programmeMembership) {
    ObjectMapper mapper = new ObjectMapper();

    Set<Map<String, String>> curricula = new HashSet<>();
    try {
      curricula = mapper.readValue(programmeMembership.getData().get(PROGRAMME_MEMBERSHIP_CURRICULA), new TypeReference<Set<Map<String, String>>>() {
      });
    } catch (Exception e) {
      e.printStackTrace(); // TODO: anything more to do here?
    }

    return curricula;
  }

  /**
   * Get the Curricula JSON from the curricula string.
   *
   * @param curricula The curricula to convert to JSON.
   * @return The curricula as JSON.
   */
  private String getCurriculaJSON(Set<Map<String,String>> curricula) {
    ObjectMapper mapper = new ObjectMapper();
    String curriculaJSON = "[]";
    try {
      curriculaJSON = mapper.writeValueAsString(curricula);
    } catch (Exception e) {
      return null;
    }
    return curriculaJSON;
  }


  /**
   * Get the TIS ID for the curriculum.
   *
   * @param curriculum The curriculum to get the TIS ID from.
   * @return The curriculum TIS ID.
   */
  private String getCurriculumTisId(Curriculum curriculum) {
    return curriculum.getData().get(CURRICULUM_TIS_ID);
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
   * @param curriculum The curriculum to get the subtype from.
   * @return The curriculum subtype.
   */
  private String getCurriculumSubType(Curriculum curriculum) {
    return curriculum.getData().get(CURRICULUM_SUB_TYPE);
  }

  /**
   * Get the StartingDate for the curriculum.
   *
   * @param programmeMembership The ProgrammeMembership to get the starting date from.
   * @return The curriculum starting date.
   *
   * Note: this is taken from the programmeMembership, NOT the curriculum
   */
  private String getCurriculumStartDate(ProgrammeMembership programmeMembership) {
    return programmeMembership.getData().get(CURRICULUM_START_DATE);
  }

}

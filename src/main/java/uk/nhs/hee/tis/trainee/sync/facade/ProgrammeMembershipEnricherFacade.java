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

import static uk.nhs.hee.tis.trainee.sync.model.Operation.DELETE;
import static uk.nhs.hee.tis.trainee.sync.model.Operation.LOAD;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.trainee.sync.model.Curriculum;
import uk.nhs.hee.tis.trainee.sync.model.Programme;
import uk.nhs.hee.tis.trainee.sync.model.ProgrammeMembership;
import uk.nhs.hee.tis.trainee.sync.service.CurriculumSyncService;
import uk.nhs.hee.tis.trainee.sync.service.ProgrammeMembershipSyncService;
import uk.nhs.hee.tis.trainee.sync.service.ProgrammeSyncService;
import uk.nhs.hee.tis.trainee.sync.service.TcsSyncService;

@Component
@Slf4j
public class ProgrammeMembershipEnricherFacade {

  private static final String PROGRAMME_MEMBERSHIP_PROGRAMME_ID = "programmeId";
  private static final String PROGRAMME_MEMBERSHIP_CURRICULUM_ID = "curriculumId";
  private static final String PROGRAMME_MEMBERSHIP_PERSON_ID = "personId";
  private static final String PROGRAMME_MEMBERSHIP_PROGRAMME_MEMBERSHIP_TYPE =
      "programmeMembershipType";
  private static final String PROGRAMME_MEMBERSHIP_PROGRAMME_COMPLETION_DATE =
      "programmeCompletionDate";
  private static final String PROGRAMME_MEMBERSHIP_PROGRAMME_START_DATE = "programmeStartDate";
  private static final String PROGRAMME_MEMBERSHIP_PROGRAMME_END_DATE = "programmeEndDate";
  private static final String PROGRAMME_MEMBERSHIP_CURRICULA = "curricula";

  private static final String PROGRAMME_MEMBERSHIP_DATA_PROGRAMME_NAME = "programmeName";
  private static final String PROGRAMME_MEMBERSHIP_DATA_PROGRAMME_TIS_ID = "programmeTisId";
  private static final String PROGRAMME_MEMBERSHIP_DATA_PROGRAMME_NUMBER = "programmeNumber";
  private static final String PROGRAMME_MEMBERSHIP_DATA_MANAGING_DEANERY = "managingDeanery";

  private static final String PROGRAMME_NAME = "programmeName";
  private static final String PROGRAMME_NUMBER = "programmeNumber";
  private static final String MANAGING_DEANERY = "owner";

  private static final String CURRICULUM_DATA_TIS_ID = "curriculumTisId";
  private static final String CURRICULUM_DATA_NAME = "curriculumName";
  private static final String CURRICULUM_DATA_SUB_TYPE = "curriculumSubType";
  private static final String CURRICULUM_DATA_START_DATE = "curriculumStartDate";
  private static final String CURRICULUM_DATA_END_DATE = "curriculumEndDate";
  // from programme membership

  private static final String CURRICULUM_NAME = "name";
  private static final String CURRICULUM_SUB_TYPE = "curriculumSubType";
  private static final String CURRICULUM_START_DATE = "curriculumStartDate";
  private static final String CURRICULUM_END_DATE = "curriculumEndDate";
  // from programme membership

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
   * Delete a programmeMembership from tis-trainee-details.
   *
   * @param programmeMembership The programme membership to delete.
   */
  public void delete(ProgrammeMembership programmeMembership) {

    deleteAllPersonsProgrammeMemberships(programmeMembership);

    HashSet<String> programmeMembershipsSynced = new HashSet<>();

    Set<ProgrammeMembership> allTheirOtherProgrammeMemberships =
        programmeMembershipService.findByPersonId(getPersonId(programmeMembership));

    for (ProgrammeMembership theirProgrammeMembership : allTheirOtherProgrammeMemberships) {
      String similarKey = getProgrammeMembershipsSimilarKey(theirProgrammeMembership);
      if (!programmeMembershipsSynced.contains(similarKey)) {
        enrich(theirProgrammeMembership, true, true, false);
        programmeMembershipsSynced.add(similarKey);
      }
    }
  }

  /**
   * Sync an enriched programmeMembership with the associated curriculum as the starting point.
   *
   * @param curriculum The curriculum triggering programme membership enrichment.
   */
  public void enrich(Curriculum curriculum) {
    final String finalCurriculumName = getCurriculumName(curriculum);
    final String finalCurriculumSubType = getCurriculumSubType(curriculum);
    final String finalCurriculumTisId = curriculum.getTisId();

    if (finalCurriculumName != null || finalCurriculumSubType != null) {
      Set<ProgrammeMembership> programmeMemberships =
          programmeMembershipService.findByCurriculumId(finalCurriculumTisId);

      programmeMemberships.forEach(
          programmeMembership -> {
            populateCurriculumDetails(programmeMembership, finalCurriculumTisId,
                finalCurriculumName, finalCurriculumSubType);
            enrich(programmeMembership, true, false, false);
          }
      );
    }
  }

  /**
   * Sync an enriched programmeMembership with the associated programme as the starting point.
   *
   * @param programme The programme triggering programme membership enrichment.
   */
  public void enrich(Programme programme) {
    final String finalProgrammeName = getProgrammeName(programme);
    final String finalProgrammeTisId = programme.getTisId();
    final String finalProgrammeNumber = getProgrammeNumber(programme);
    final String finalManagingDeanery = getManagingDeanery(programme);

    if (finalProgrammeName != null || finalProgrammeTisId != null || finalProgrammeNumber != null
        || finalManagingDeanery != null) {
      Set<ProgrammeMembership> programmeMemberships =
          programmeMembershipService.findByProgrammeId(finalProgrammeTisId);

      programmeMemberships.forEach(
          programmeMembership -> {
            populateProgrammeDetails(programmeMembership, finalProgrammeName, finalProgrammeTisId,
                finalProgrammeNumber, finalManagingDeanery);
            enrich(programmeMembership, false, true, false);
          }
      );
    }
  }

  /**
   * Sync an enriched programmeMembership with the programmeMembership as the starting object.
   *
   * @param programmeMembership The programmeMembership to enrich.
   */
  public void enrich(ProgrammeMembership programmeMembership) {
    enrich(programmeMembership, true, true, true);
  }

  /**
   * Sync an enriched programmeMembership with the programmeMembership. Optionally enrich programme
   * or curriculum details Optionally completely resync all programme memberships for the person
   *
   * @param programmeMembership                  The programmeMembership to enrich.
   * @param doProgrammeEnrich                    Enrich programme details
   * @param doCurriculumEnrich                   Enrich curriculum details
   * @param doRebuildPersonsProgrammeMemberships Rebuild all programme memberships for person
   */
  private void enrich(ProgrammeMembership programmeMembership,
      boolean doProgrammeEnrich,
      boolean doCurriculumEnrich,
      boolean doRebuildPersonsProgrammeMemberships) {
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
      syncAggregateProgrammeMembership(programmeMembership,
          doRebuildPersonsProgrammeMemberships);
    }
  }

  /**
   * Enrich the programmeMembership with details from the Curriculum.
   *
   * @param programmeMembership The programmeMembership to enrich.
   * @param curriculum          The curriculum to enrich the programmeMembership with.
   * @return Whether enrichment was successful.
   */
  private boolean enrich(ProgrammeMembership programmeMembership, Curriculum curriculum) {

    String curriculumName = getCurriculumName(curriculum);
    String curriculumTisId = curriculum.getTisId();
    String curriculumSubType = getCurriculumSubType(curriculum);

    if (curriculumName != null) {
      populateCurriculumDetails(programmeMembership, curriculumTisId, curriculumName,
          curriculumSubType);
      return true;
    }

    return false;
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
    String programmeTisId = programme.getTisId();
    String programmeNumber = getProgrammeNumber(programme);
    String managingDeanery = getManagingDeanery(programme);

    if (programmeName != null || programmeTisId != null || programmeNumber != null
        || managingDeanery != null) {
      populateProgrammeDetails(programmeMembership, programmeName, programmeTisId, programmeNumber,
          managingDeanery);
      return true;
    }

    return false;
  }

  /**
   * Sync the aggregated programmeMembership.
   *
   * @param aggregateProgrammeMembership         The aggregated programmeMembership to sync.
   * @param doRebuildPersonsProgrammeMemberships Re-sync all PMs for the person.
   */
  void syncAggregateProgrammeMembership(ProgrammeMembership aggregateProgrammeMembership,
      boolean doRebuildPersonsProgrammeMemberships) {
    if (doRebuildPersonsProgrammeMemberships) {
      deleteAllPersonsProgrammeMemberships(aggregateProgrammeMembership);

      HashSet<String> programmeMembershipsSynced = new HashSet<>();
      syncProgrammeMembership(aggregateProgrammeMembership);
      programmeMembershipsSynced
          .add(getProgrammeMembershipsSimilarKey(aggregateProgrammeMembership));

      Set<ProgrammeMembership> allTheirProgrammeMemberships =
          programmeMembershipService.findByPersonId(getPersonId(aggregateProgrammeMembership));

      for (ProgrammeMembership theirProgrammeMembership : allTheirProgrammeMemberships) {
        if (!programmeMembershipsSynced
            .contains(getProgrammeMembershipsSimilarKey(theirProgrammeMembership))) {
          enrich(theirProgrammeMembership, true, true, false);
          programmeMembershipsSynced
              .add(getProgrammeMembershipsSimilarKey(theirProgrammeMembership));
        }
      }
    } else {
      syncProgrammeMembership(aggregateProgrammeMembership);
    }
  }

  /**
   * Enrich the programmeMembership with the given programme name, TIS ID, number and managing
   * deanery and then sync it.
   *
   * @param programmeMembership The programmeMembership to sync.
   * @param curriculumName      The curriculum name to enrich with.
   */
  private void populateCurriculumDetails(ProgrammeMembership programmeMembership,
      String curriculumTisId,
      String curriculumName,
      String curriculumSubType) {
    // Add extra data to programmeMembership data. This is unpacked again in
    // syncProgrammeMembership(ProgrammeMembership programmeMembership)
    // to derive the aggregate programmeMembership record.
    if (Strings.isNotBlank(curriculumName)) {
      Map<String, String> c = new HashMap<>();
      c.put(CURRICULUM_DATA_NAME, curriculumName);
      c.put(CURRICULUM_DATA_TIS_ID, curriculumTisId);
      c.put(CURRICULUM_DATA_SUB_TYPE, curriculumSubType);
      c.put(CURRICULUM_DATA_START_DATE, getCurriculumStartDate(programmeMembership));
      c.put(CURRICULUM_DATA_END_DATE, getCurriculumEndDate(programmeMembership));

      Set<Map<String, String>> curricula = new HashSet<>();
      curricula.add(c);

      String curriculaJson = getCurriculaJson(curricula);

      programmeMembership.getData().put(PROGRAMME_MEMBERSHIP_CURRICULA, curriculaJson);
    }
  }

  /**
   * Enrich the programmeMembership with the given programme name, TIS ID, number and managing
   * deanery and then sync it.
   *
   * @param programmeMembership The programmeMembership to sync.
   * @param programmeName       The programme name to enrich with.
   * @param programmeTisId      The programme TIS ID to enrich with.
   * @param programmeName       The programme name to enrich with.
   * @param programmeNumber     The programme number to enrich with.
   * @param managingDeanery     The managing deanery to enrich with.
   */
  private void populateProgrammeDetails(ProgrammeMembership programmeMembership,
      String programmeName,
      String programmeTisId,
      String programmeNumber,
      String managingDeanery) {
    // Add extra data to programmeMembership data.
    if (Strings.isNotBlank(programmeName)) {
      programmeMembership.getData()
          .put(PROGRAMME_MEMBERSHIP_DATA_PROGRAMME_NAME, programmeName);
    }
    if (Strings.isNotBlank(programmeTisId)) {
      programmeMembership.getData()
          .put(PROGRAMME_MEMBERSHIP_DATA_PROGRAMME_TIS_ID, programmeTisId);
    }
    if (Strings.isNotBlank(programmeNumber)) {
      programmeMembership.getData()
          .put(PROGRAMME_MEMBERSHIP_DATA_PROGRAMME_NUMBER, programmeNumber);
    }
    if (Strings.isNotBlank(managingDeanery)) {
      programmeMembership.getData()
          .put(PROGRAMME_MEMBERSHIP_DATA_MANAGING_DEANERY, managingDeanery);
    }
  }

  /**
   * Sync the (completely enriched) programmeMembership, aggregating with similar
   * programmeMemberships.
   * Note: 'similar' is defined as sharing the same personId,
   * programmeId, programmeStartDate, programmeEndDate and
   * programmeMembershipType.
   * @param programmeMembership The programmeMembership to sync.
   */
  private void syncProgrammeMembership(ProgrammeMembership programmeMembership) {
    // Set the required metadata so the record can be synced using common logic.
    programmeMembership.setOperation(LOAD);
    programmeMembership.setSchema("tcs");
    programmeMembership.setTable("ProgrammeMembership");

    // first get all similar programmeMemberships
    Set<ProgrammeMembership> programmeMemberships =
        getProgrammeMembershipsSimilarTo(programmeMembership);

    // initialise properties that will be aggregated
    // TIS ID
    Set<String> tisIds = new HashSet<>();
    tisIds.add(programmeMembership.getTisId());
    // programmeCompletionDate
    String programmeCompletionDate = getProgrammeCompletionDate(programmeMembership);
    LocalDate maxProgrammeCompletionDate =
        programmeCompletionDate == null ? null : LocalDate.parse(programmeCompletionDate);
    //curricula
    Set<Map<String, String>> allCurricula = getCurricula(programmeMembership);

    // it is possible for the similar programmeMemberships to reference data (e.g. curricula)
    // we do not yet have in the local store, in which case the sync will be aborted
    boolean doSync = true;

    // traverse the similar programmeMemberships to derive the aggregate properties
    for (ProgrammeMembership thisProgrammeMembership : programmeMemberships) {

      // TIS ID
      tisIds.add(thisProgrammeMembership.getTisId());

      // programmeCompletionDate
      maxProgrammeCompletionDate = getNewMaximumProgrammeCompletionDate(maxProgrammeCompletionDate,
          thisProgrammeMembership);

      // curricula
      String curriculumId = getCurriculumId(thisProgrammeMembership);
      if (curriculumId != null) {
        Optional<Curriculum> optionalCurriculum = curriculumSyncService.findById(curriculumId);

        if (optionalCurriculum.isPresent()) {
          enrich(thisProgrammeMembership, optionalCurriculum.get());
        } else {
          doSync = false;
          break;
          // Cannot sync this record because all the related curriculum data is not available in
          // local store.
          // Note that we don't need to worry about programme data availability, since by definition
          // programmeMemberships will all have the same programmeId as programmeMembership, which
          // has already been successfully enriched with locally-held programme data.
        }
      }
      Set<Map<String, String>> thisCurricula = getCurricula(thisProgrammeMembership);
      allCurricula.addAll(thisCurricula);
    }

    if (doSync) {
      // final preparation and insertion of aggregate data

      //TIS ID
      List<String> sortedTisIds = new ArrayList<>(tisIds);
      Collections.sort(sortedTisIds);
      String allSortedTisIds = String.join(",", sortedTisIds);
      programmeMembership.setTisId(allSortedTisIds);

      // programmeCompletionDate
      programmeMembership.getData().put(
          PROGRAMME_MEMBERSHIP_PROGRAMME_COMPLETION_DATE,
          String.valueOf(maxProgrammeCompletionDate));

      // curricula
      String allCurriculaJson = getCurriculaJson(allCurricula);
      programmeMembership.getData().put(
          PROGRAMME_MEMBERSHIP_CURRICULA,
          allCurriculaJson);

      // sync the complete aggregate programmeMembership record
      tcsSyncService.syncRecord(programmeMembership);
    }
  }

  /**
   * Delete all programmeMemberships for the person.
   *
   * @param programmeMembership The programme membership to retrieve the person from
   */
  private void deleteAllPersonsProgrammeMemberships(ProgrammeMembership programmeMembership) {
    programmeMembership.setOperation(DELETE);
    programmeMembership.setSchema("tcs");
    programmeMembership.setTable("ProgrammeMembership");
    tcsSyncService.syncRecord(programmeMembership); // delete all programme memberships for personId
  }

  /**
   * Get the greater (most recent) date from a current maximum date and a programmeMembership's
   * completion date.
   *
   * @param currentMaximumDate  The current maximum date
   * @param programmeMembership The programme membership to retrieve the completion date from.
   * @return The new maximum date.
   */
  private LocalDate getNewMaximumProgrammeCompletionDate(LocalDate currentMaximumDate,
      ProgrammeMembership programmeMembership) {
    LocalDate newMaximumDate = currentMaximumDate;
    String programmeCompletionDateString = getProgrammeCompletionDate(programmeMembership);
    if (programmeCompletionDateString != null) {
      LocalDate programmeCompletionDate = LocalDate.parse(programmeCompletionDateString);
      if (currentMaximumDate == null || programmeCompletionDate.isAfter(currentMaximumDate)) {
        newMaximumDate = programmeCompletionDate;
      }
    }
    return newMaximumDate;
  }

  /**
   * Get the programme memberships similar to the passed programme memberships.
   * Note: 'similar' is defined as sharing the same personId, programmeId, programmeStartDate,
   * programmeEndDate and programmeMembershipType.
   * @param programmeMembership The programme membership to use as the criteria
   * @return The set of similar programme memberships.
   */
  private Set<ProgrammeMembership> getProgrammeMembershipsSimilarTo(
      ProgrammeMembership programmeMembership) {
    String personId = getPersonId(programmeMembership);
    String programmeId = getProgrammeId(programmeMembership);
    String programmeMembershipType = getProgrammeMembershipType(programmeMembership);
    String programmeStartDate = getProgrammeStartDate(programmeMembership);
    String programmeEndDate = getProgrammeEndDate(programmeMembership);

    return programmeMembershipService.findBySimilar(personId, programmeId, programmeMembershipType,
        programmeStartDate, programmeEndDate);
  }

  /**
   * Get the programme memberships similar to the passed programme memberships.
   * Note: 'similar' means sharing the same personId, programmeId, programmeStartDate,
   * programmeEndDate and programmeMembershipType.
   * @param programmeMembership The programme membership to use as the criteria
   * @return The set of similar programme memberships.
   */
  private String getProgrammeMembershipsSimilarKey(ProgrammeMembership programmeMembership) {
    String personId = getPersonId(programmeMembership);
    String programmeId = getProgrammeId(programmeMembership);
    String programmeMembershipType = getProgrammeMembershipType(programmeMembership);
    String programmeStartDate = getProgrammeStartDate(programmeMembership);
    String programmeEndDate = getProgrammeEndDate(programmeMembership);

    StringBuilder key = new StringBuilder();
    key.append(personId);
    key.append(".");
    key.append(programmeId);
    key.append(".");
    key.append(programmeMembershipType);
    key.append(".");
    key.append(programmeStartDate);
    key.append(".");
    key.append(programmeEndDate);
    return key.toString();
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
  private Set<Map<String, String>> getCurricula(ProgrammeMembership programmeMembership) {
    ObjectMapper mapper = new ObjectMapper();

    Set<Map<String, String>> curricula = new HashSet<>();
    String curriculaString = programmeMembership.getData().get(PROGRAMME_MEMBERSHIP_CURRICULA);
    if (curriculaString != null) {
      try {
        curricula = mapper.readValue(curriculaString, new TypeReference<>() {
        });
      } catch (JsonProcessingException e) {
        log.error("Badly formed curricula JSON in {}", programmeMembership);
      }
    }

    return curricula;
  }

  /**
   * Get the Curricula JSON from the curricula string.
   *
   * @param curricula The curricula to convert to JSON.
   * @return The curricula as JSON.
   */
  private String getCurriculaJson(Set<Map<String, String>> curricula) {
    ObjectMapper mapper = new ObjectMapper();
    String curriculaJson = "[]";
    try {
      curriculaJson = mapper.writeValueAsString(curricula);
    } catch (Exception e) {
      return null;
    }
    return curriculaJson;
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
   * Note: this is taken from the programmeMembership, NOT the curriculum.
   * @param programmeMembership The ProgrammeMembership to get the starting date from.
   * @return The curriculum starting date.
   */
  private String getCurriculumStartDate(ProgrammeMembership programmeMembership) {
    return programmeMembership.getData().get(CURRICULUM_START_DATE);
  }

  /**
   * Get the EndDate for the curriculum.
   *
   * @param programmeMembership The ProgrammeMembership to get the curriculum end date from.
   * @return The curriculum end date.
   *
   *                            Note: this is taken from the programmeMembership, NOT the curriculum
   */
  private String getCurriculumEndDate(ProgrammeMembership programmeMembership) {
    return programmeMembership.getData().get(CURRICULUM_END_DATE);
  }
}

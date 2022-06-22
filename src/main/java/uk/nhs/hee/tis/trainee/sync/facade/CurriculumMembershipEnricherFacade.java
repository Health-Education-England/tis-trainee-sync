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
import uk.nhs.hee.tis.trainee.sync.model.CurriculumMembership;
import uk.nhs.hee.tis.trainee.sync.model.Programme;
import uk.nhs.hee.tis.trainee.sync.service.CurriculumMembershipSyncService;
import uk.nhs.hee.tis.trainee.sync.service.CurriculumSyncService;
import uk.nhs.hee.tis.trainee.sync.service.ProgrammeSyncService;
import uk.nhs.hee.tis.trainee.sync.service.TcsSyncService;

@Component
@Slf4j
public class CurriculumMembershipEnricherFacade {

  private static final String CURRICULUM_MEMBERSHIP_PROGRAMME_ID = "programmeId";
  private static final String CURRICULUM_MEMBERSHIP_CURRICULUM_ID = "curriculumId";
  private static final String CURRICULUM_MEMBERSHIP_PERSON_ID = "personId";
  private static final String CURRICULUM_MEMBERSHIP_PROGRAMME_MEMBERSHIP_TYPE =
      "programmeMembershipType";
  private static final String CURRICULUM_MEMBERSHIP_PROGRAMME_COMPLETION_DATE =
      "programmeCompletionDate";
  private static final String CURRICULUM_MEMBERSHIP_PROGRAMME_START_DATE = "programmeStartDate";
  private static final String CURRICULUM_MEMBERSHIP_PROGRAMME_END_DATE = "programmeEndDate";
  private static final String CURRICULUM_MEMBERSHIP_CURRICULA = "curricula";

  private static final String CURRICULUM_MEMBERSHIP_DATA_PROGRAMME_NAME = "programmeName";
  private static final String CURRICULUM_MEMBERSHIP_DATA_PROGRAMME_TIS_ID = "programmeTisId";
  private static final String CURRICULUM_MEMBERSHIP_DATA_PROGRAMME_NUMBER = "programmeNumber";
  private static final String CURRICULUM_MEMBERSHIP_DATA_MANAGING_DEANERY = "managingDeanery";

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

  private final CurriculumMembershipSyncService curriculumMembershipSyncService;
  private final ProgrammeSyncService programmeSyncService;
  private final CurriculumSyncService curriculumSyncService;

  private final TcsSyncService tcsSyncService;

  CurriculumMembershipEnricherFacade(
      CurriculumMembershipSyncService curriculumMembershipSyncService,
      ProgrammeSyncService programmeSyncService,
      CurriculumSyncService curriculumSyncService,
      TcsSyncService tcsSyncService) {
    this.curriculumMembershipSyncService = curriculumMembershipSyncService;
    this.programmeSyncService = programmeSyncService;
    this.curriculumSyncService = curriculumSyncService;
    this.tcsSyncService = tcsSyncService;
  }

  /**
   * Delete a curriculumMembership from tis-trainee-details.
   *
   * @param curriculumMembership The curriculum membership to delete.
   */
  public void delete(CurriculumMembership curriculumMembership) {

    deleteAllPersonsCurriculumMemberships(curriculumMembership);

    HashSet<String> curriculumMembershipsSynced = new HashSet<>();

    Set<CurriculumMembership> allTheirOtherCurriculumMemberships =
        curriculumMembershipSyncService.findByPersonId(getPersonId(curriculumMembership));

    for (CurriculumMembership theirCurriculumMembership : allTheirOtherCurriculumMemberships) {
      String similarKey = getCurriculumMembershipsSimilarKey(theirCurriculumMembership);
      if (!curriculumMembershipsSynced.contains(similarKey)) {
        enrich(theirCurriculumMembership, true, true, false);
        curriculumMembershipsSynced.add(similarKey);
      }
    }
  }

  /**
   * Sync an enriched curriculumMembership with the associated curriculum as the starting point.
   *
   * @param curriculum The curriculum triggering curriculum membership enrichment.
   */
  public void enrich(Curriculum curriculum) {
    final String finalCurriculumName = getCurriculumName(curriculum);
    final String finalCurriculumSubType = getCurriculumSubType(curriculum);
    final String finalCurriculumTisId = curriculum.getTisId();

    if (finalCurriculumName != null || finalCurriculumSubType != null) {
      Set<CurriculumMembership> curriculumMemberships =
          curriculumMembershipSyncService.findByCurriculumId(finalCurriculumTisId);

      curriculumMemberships.forEach(
          curriculumMembership -> {
            populateCurriculumDetails(curriculumMembership, finalCurriculumTisId,
                finalCurriculumName, finalCurriculumSubType);
            enrich(curriculumMembership, true, false, false);
          }
      );
    }
  }

  /**
   * Sync an enriched curriculumMembership with the associated programme as the starting point.
   *
   * @param programme The programme triggering curriculum membership enrichment.
   */
  public void enrich(Programme programme) {
    final String finalProgrammeName = getProgrammeName(programme);
    final String finalProgrammeTisId = programme.getTisId();
    final String finalProgrammeNumber = getProgrammeNumber(programme);
    final String finalManagingDeanery = getManagingDeanery(programme);

    if (finalProgrammeName != null || finalProgrammeTisId != null || finalProgrammeNumber != null
        || finalManagingDeanery != null) {
      Set<CurriculumMembership> curriculumMemberships =
          curriculumMembershipSyncService.findByProgrammeId(finalProgrammeTisId);

      curriculumMemberships.forEach(
          curriculumMembership -> {
            populateProgrammeDetails(curriculumMembership, finalProgrammeName, finalProgrammeTisId,
                finalProgrammeNumber, finalManagingDeanery);
            enrich(curriculumMembership, false, true, false);
          }
      );
    }
  }

  /**
   * Sync an enriched curriculumMembership with the curriculumMembership as the starting object.
   *
   * @param curriculumMembership The curriculumMembership to enrich.
   */
  public void enrich(CurriculumMembership curriculumMembership) {
    enrich(curriculumMembership, true, true, true);
  }

  /**
   * Sync an enriched curriculumMembership with the curriculumMembership. Optionally enrich
   * programme or curriculum details. Optionally completely resync all programme memberships for the
   * person.
   *
   * @param curriculumMembership                  The curriculumMembership to enrich.
   * @param doProgrammeEnrich                     Enrich programme details
   * @param doCurriculumEnrich                    Enrich curriculum details
   * @param doRebuildPersonsCurriculumMemberships Rebuild all curriculum memberships for person
   */
  private void enrich(CurriculumMembership curriculumMembership,
      boolean doProgrammeEnrich,
      boolean doCurriculumEnrich,
      boolean doRebuildPersonsCurriculumMemberships) {
    boolean doSync = true;

    if (doProgrammeEnrich) {
      String programmeId = getProgrammeId(curriculumMembership);

      if (programmeId != null) {
        Optional<Programme> optionalProgramme = programmeSyncService.findById(programmeId);

        if (optionalProgramme.isPresent()) {
          doSync = enrich(curriculumMembership, optionalProgramme.get());
        } else {
          programmeSyncService.request(programmeId);
          doSync = false;
        }
      }
    }

    if (doCurriculumEnrich) {
      String curriculumId = getCurriculumId(curriculumMembership);

      if (curriculumId != null) {
        Optional<Curriculum> optionalCurriculum = curriculumSyncService.findById(curriculumId);

        if (optionalCurriculum.isPresent()) {
          doSync &= enrich(curriculumMembership, optionalCurriculum.get());
        } else {
          curriculumSyncService.request(curriculumId);
          doSync = false;
        }
      }
    }

    if (doSync) {
      syncAggregateCurriculumMembership(curriculumMembership,
          doRebuildPersonsCurriculumMemberships);
    }
  }

  /**
   * Enrich the curriculumMembership with details from the Curriculum.
   *
   * @param curriculumMembership The curriculumMembership to enrich.
   * @param curriculum           The curriculum to enrich the curriculumMembership with.
   * @return Whether enrichment was successful.
   */
  private boolean enrich(CurriculumMembership curriculumMembership, Curriculum curriculum) {

    String curriculumName = getCurriculumName(curriculum);
    String curriculumTisId = curriculum.getTisId();
    String curriculumSubType = getCurriculumSubType(curriculum);

    if (curriculumName != null) {
      populateCurriculumDetails(curriculumMembership, curriculumTisId, curriculumName,
          curriculumSubType);
      return true;
    }

    return false;
  }

  /**
   * Enrich the curriculumMembership with details from the Programme.
   *
   * @param curriculumMembership The curriculumMembership to enrich.
   * @param programme            The programme to enrich the curriculumMembership with.
   * @return Whether enrichment was successful.
   */
  private boolean enrich(CurriculumMembership curriculumMembership, Programme programme) {

    String programmeName = getProgrammeName(programme);
    String programmeTisId = programme.getTisId();
    String programmeNumber = getProgrammeNumber(programme);
    String managingDeanery = getManagingDeanery(programme);

    if (programmeName != null || programmeTisId != null || programmeNumber != null
        || managingDeanery != null) {
      populateProgrammeDetails(curriculumMembership, programmeName, programmeTisId, programmeNumber,
          managingDeanery);
      return true;
    }

    return false;
  }

  /**
   * Sync the aggregated curriculumMembership.
   *
   * @param aggregateCurriculumMembership         The aggregated curriculumMembership to sync.
   * @param doRebuildPersonsCurriculumMemberships Re-sync all PMs for the person.
   */
  void syncAggregateCurriculumMembership(CurriculumMembership aggregateCurriculumMembership,
      boolean doRebuildPersonsCurriculumMemberships) {
    if (doRebuildPersonsCurriculumMemberships) {
      deleteAllPersonsCurriculumMemberships(aggregateCurriculumMembership);

      HashSet<String> curriculumMembershipsSynced = new HashSet<>();
      syncCurriculumMembership(aggregateCurriculumMembership);
      curriculumMembershipsSynced
          .add(getCurriculumMembershipsSimilarKey(aggregateCurriculumMembership));

      Set<CurriculumMembership> allTheirCurriculumMemberships =
          curriculumMembershipSyncService
              .findByPersonId(getPersonId(aggregateCurriculumMembership));

      for (CurriculumMembership theirCurriculumMembership : allTheirCurriculumMemberships) {
        if (!curriculumMembershipsSynced
            .contains(getCurriculumMembershipsSimilarKey(theirCurriculumMembership))) {
          enrich(theirCurriculumMembership, true, true, false);
          curriculumMembershipsSynced
              .add(getCurriculumMembershipsSimilarKey(theirCurriculumMembership));
        }
      }
    } else {
      syncCurriculumMembership(aggregateCurriculumMembership);
    }
  }

  /**
   * Enrich the curriculumMembership with the given programme name, TIS ID, number and managing
   * deanery and then sync it.
   *
   * @param curriculumMembership The curriculumMembership to sync.
   * @param curriculumName       The curriculum name to enrich with.
   */
  private void populateCurriculumDetails(CurriculumMembership curriculumMembership,
      String curriculumTisId,
      String curriculumName,
      String curriculumSubType) {
    // Add extra data to curriculumMembership data. This is unpacked again in
    // syncCurriculumMembership(CurriculumMembership curriculumMembership)
    // to derive the aggregate curriculumMembership record.
    if (Strings.isNotBlank(curriculumName)) {
      Map<String, String> c = new HashMap<>();
      c.put(CURRICULUM_DATA_NAME, curriculumName);
      c.put(CURRICULUM_DATA_TIS_ID, curriculumTisId);
      c.put(CURRICULUM_DATA_SUB_TYPE, curriculumSubType);
      c.put(CURRICULUM_DATA_START_DATE, getCurriculumStartDate(curriculumMembership));
      c.put(CURRICULUM_DATA_END_DATE, getCurriculumEndDate(curriculumMembership));

      Set<Map<String, String>> curricula = new HashSet<>();
      curricula.add(c);

      String curriculaJson = getCurriculaJson(curricula);

      curriculumMembership.getData().put(CURRICULUM_MEMBERSHIP_CURRICULA, curriculaJson);
    }
  }

  /**
   * Enrich the curriculumMembership with the given programme name, TIS ID, number and managing
   * deanery and then sync it.
   *
   * @param curriculumMembership The curriculumMembership to sync.
   * @param programmeName        The programme name to enrich with.
   * @param programmeTisId       The programme TIS ID to enrich with.
   * @param programmeName        The programme name to enrich with.
   * @param programmeNumber      The programme number to enrich with.
   * @param managingDeanery      The managing deanery to enrich with.
   */
  private void populateProgrammeDetails(CurriculumMembership curriculumMembership,
      String programmeName,
      String programmeTisId,
      String programmeNumber,
      String managingDeanery) {
    // Add extra data to curriculumMembership data.
    if (Strings.isNotBlank(programmeName)) {
      curriculumMembership.getData()
          .put(CURRICULUM_MEMBERSHIP_DATA_PROGRAMME_NAME, programmeName);
    }
    if (Strings.isNotBlank(programmeTisId)) {
      curriculumMembership.getData()
          .put(CURRICULUM_MEMBERSHIP_DATA_PROGRAMME_TIS_ID, programmeTisId);
    }
    if (Strings.isNotBlank(programmeNumber)) {
      curriculumMembership.getData()
          .put(CURRICULUM_MEMBERSHIP_DATA_PROGRAMME_NUMBER, programmeNumber);
    }
    if (Strings.isNotBlank(managingDeanery)) {
      curriculumMembership.getData()
          .put(CURRICULUM_MEMBERSHIP_DATA_MANAGING_DEANERY, managingDeanery);
    }
  }

  /**
   * Sync the (completely enriched) curriculumMembership, aggregating with similar
   * curriculumMemberships. Note: 'similar' is defined as sharing the same personId, programmeId,
   * programmeStartDate, programmeEndDate and programmeMembershipType.
   *
   * @param curriculumMembership The curriculumMembership to sync.
   */
  private void syncCurriculumMembership(CurriculumMembership curriculumMembership) {
    // Set the required metadata so the record can be synced using common logic.
    curriculumMembership.setOperation(LOAD);
    curriculumMembership.setSchema("tcs");
    curriculumMembership.setTable("CurriculumMembership");

    // first get all similar curriculumMemberships
    Set<CurriculumMembership> curriculumMemberships =
        getCurriculumMembershipsSimilarTo(curriculumMembership);

    // initialise properties that will be aggregated
    // TIS ID
    Set<String> tisIds = new HashSet<>();
    tisIds.add(curriculumMembership.getTisId());
    // programmeCompletionDate
    String programmeCompletionDate = getProgrammeCompletionDate(curriculumMembership);
    LocalDate maxProgrammeCompletionDate =
        programmeCompletionDate == null ? null : LocalDate.parse(programmeCompletionDate);
    //curricula
    Set<Map<String, String>> allCurricula = getCurricula(curriculumMembership);

    // it is possible for the similar curriculumMemberships to reference data (e.g. curricula)
    // we do not yet have in the local store, in which case the sync will be aborted
    boolean doSync = true;

    // traverse the similar curriculumMemberships to derive the aggregate properties
    for (CurriculumMembership thisCurriculumMembership : curriculumMemberships) {

      // TIS ID
      tisIds.add(thisCurriculumMembership.getTisId());

      // programmeCompletionDate
      maxProgrammeCompletionDate = getNewMaximumProgrammeCompletionDate(maxProgrammeCompletionDate,
          thisCurriculumMembership);

      // curricula
      String curriculumId = getCurriculumId(thisCurriculumMembership);
      if (curriculumId != null) {
        Optional<Curriculum> optionalCurriculum = curriculumSyncService.findById(curriculumId);

        if (optionalCurriculum.isPresent()) {
          enrich(thisCurriculumMembership, optionalCurriculum.get());
        } else {
          doSync = false;
          break;
          // Cannot sync this record because all the related curriculum data is not available in
          // local store.
          // Note that we don't need to worry about programme data availability, since by definition
          // curriculumMemberships will all have the same programmeId as curriculumMembership, which
          // has already been successfully enriched with locally-held programme data.
        }
      }
      Set<Map<String, String>> thisCurricula = getCurricula(thisCurriculumMembership);
      allCurricula.addAll(thisCurricula);
    }

    if (doSync) {
      // final preparation and insertion of aggregate data

      //TIS ID
      List<String> sortedTisIds = new ArrayList<>(tisIds);
      Collections.sort(sortedTisIds);
      String allSortedTisIds = String.join(",", sortedTisIds);
      curriculumMembership.setTisId(allSortedTisIds);

      // programmeCompletionDate
      curriculumMembership.getData().put(
          CURRICULUM_MEMBERSHIP_PROGRAMME_COMPLETION_DATE,
          String.valueOf(maxProgrammeCompletionDate));

      // curricula
      String allCurriculaJson = getCurriculaJson(allCurricula);
      curriculumMembership.getData().put(
          CURRICULUM_MEMBERSHIP_CURRICULA,
          allCurriculaJson);

      // sync the complete aggregate curriculumMembership record
      tcsSyncService.syncRecord(curriculumMembership);
    }
  }

  /**
   * Delete all curriculumMemberships for the person.
   *
   * @param curriculumMembership The curriculum membership to retrieve the person from
   */
  private void deleteAllPersonsCurriculumMemberships(CurriculumMembership curriculumMembership) {
    curriculumMembership.setOperation(DELETE);
    curriculumMembership.setSchema("tcs");
    curriculumMembership.setTable("CurriculumMembership");
    tcsSyncService.syncRecord(curriculumMembership); //delete all curriculummemberships for personId
  }

  /**
   * Get the greater (most recent) date from a current maximum date and a curriculumMembership's
   * completion date.
   *
   * @param currentMaximumDate   The current maximum date
   * @param curriculumMembership The curriculum membership to retrieve the completion date from.
   * @return The new maximum date.
   */
  private LocalDate getNewMaximumProgrammeCompletionDate(
      LocalDate currentMaximumDate,
      CurriculumMembership curriculumMembership) {
    LocalDate newMaximumDate = currentMaximumDate;
    String programmeCompletionDateString = getProgrammeCompletionDate(curriculumMembership);
    if (programmeCompletionDateString != null) {
      LocalDate programmeCompletionDate = LocalDate.parse(programmeCompletionDateString);
      if (currentMaximumDate == null || programmeCompletionDate.isAfter(currentMaximumDate)) {
        newMaximumDate = programmeCompletionDate;
      }
    }
    return newMaximumDate;
  }

  /**
   * Get the curriculum memberships similar to the passed curriculum membership. Note: 'similar' is
   * defined as sharing the same personId, programmeId, programmeStartDate, programmeEndDate and
   * programmeMembershipType.
   *
   * @param curriculumMembership The curriculum membership to use as the criteria
   * @return The set of similar curriculum memberships.
   */
  private Set<CurriculumMembership> getCurriculumMembershipsSimilarTo(
      CurriculumMembership curriculumMembership) {
    String personId = getPersonId(curriculumMembership);
    String programmeId = getProgrammeId(curriculumMembership);
    String programmeMembershipType = getProgrammeMembershipType(curriculumMembership);
    String programmeStartDate = getProgrammeStartDate(curriculumMembership);
    String programmeEndDate = getProgrammeEndDate(curriculumMembership);

    return curriculumMembershipSyncService.findBySimilar(personId, programmeId,
        programmeMembershipType, programmeStartDate, programmeEndDate);
  }

  /**
   * Get the curriculum memberships similarity key. Note: 'similar' means sharing the same personId,
   * programmeId, programmeStartDate, programmeEndDate and programmeMembershipType.
   *
   * @param curriculumMembership The curriculum membership to use as the criteria
   * @return The similarity key.
   */
  private String getCurriculumMembershipsSimilarKey(CurriculumMembership curriculumMembership) {
    String personId = getPersonId(curriculumMembership);
    String programmeId = getProgrammeId(curriculumMembership);
    String programmeMembershipType = getProgrammeMembershipType(curriculumMembership);
    String programmeStartDate = getProgrammeStartDate(curriculumMembership);
    String programmeEndDate = getProgrammeEndDate(curriculumMembership);

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
   * Get the Programme ID from the curriculumMembership.
   *
   * @param curriculumMembership The curriculumMembership to get the programme id from.
   * @return The programme id.
   */
  private String getProgrammeId(CurriculumMembership curriculumMembership) {
    return curriculumMembership.getData().get(CURRICULUM_MEMBERSHIP_PROGRAMME_ID);
  }

  /**
   * Get the Person ID from the curriculumMembership.
   *
   * @param curriculumMembership The curriculumMembership to get the person id from.
   * @return The person id.
   */
  private String getPersonId(CurriculumMembership curriculumMembership) {
    return curriculumMembership.getData().get(CURRICULUM_MEMBERSHIP_PERSON_ID);
  }

  /**
   * Get the Programme Membership Type from the curriculumMembership.
   *
   * @param curriculumMembership The curriculumMembership to get the membership type from.
   * @return The programme membership type.
   */
  private String getProgrammeMembershipType(CurriculumMembership curriculumMembership) {
    return curriculumMembership.getData().get(CURRICULUM_MEMBERSHIP_PROGRAMME_MEMBERSHIP_TYPE);
  }

  /**
   * Get the Programme Start Date from the curriculumMembership.
   *
   * @param curriculumMembership The curriculumMembership to get the start date from.
   * @return The programme start date.
   */
  private String getProgrammeStartDate(CurriculumMembership curriculumMembership) {
    return curriculumMembership.getData().get(CURRICULUM_MEMBERSHIP_PROGRAMME_START_DATE);
  }

  /**
   * Get the Programme End Date from the curriculumMembership.
   *
   * @param curriculumMembership The curriculumMembership to get the end date from.
   * @return The programme end date.
   */
  private String getProgrammeEndDate(CurriculumMembership curriculumMembership) {
    return curriculumMembership.getData().get(CURRICULUM_MEMBERSHIP_PROGRAMME_END_DATE);
  }

  /**
   * Get the Programme Completion Date from the curriculumMembership.
   *
   * @param curriculumMembership The curriculumMembership to get the membership type from.
   * @return The programme membership type.
   */
  private String getProgrammeCompletionDate(CurriculumMembership curriculumMembership) {
    return curriculumMembership.getData().get(CURRICULUM_MEMBERSHIP_PROGRAMME_COMPLETION_DATE);
  }

  /**
   * Get the Curriculum ID from the curriculumMembership.
   *
   * @param curriculumMembership The curriculumMembership to get the curriculum id from.
   * @return The programme id.
   */
  private String getCurriculumId(CurriculumMembership curriculumMembership) {
    return curriculumMembership.getData().get(CURRICULUM_MEMBERSHIP_CURRICULUM_ID);
  }

  /**
   * Get the Curricula from the curriculumMembership.
   *
   * @param curriculumMembership The curriculumMembership to get the curricula from.
   * @return The curricula.
   */
  private Set<Map<String, String>> getCurricula(CurriculumMembership curriculumMembership) {
    ObjectMapper mapper = new ObjectMapper();

    Set<Map<String, String>> curricula = new HashSet<>();
    String curriculaString = curriculumMembership.getData().get(CURRICULUM_MEMBERSHIP_CURRICULA);
    if (curriculaString != null) {
      try {
        curricula = mapper.readValue(curriculaString, new TypeReference<>() {
        });
      } catch (JsonProcessingException e) {
        log.error("Badly formed curricula JSON in {}", curriculumMembership);
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
   * Get the StartingDate for the curriculum. Note: this is taken from the curriculumMembership, NOT
   * the curriculum.
   *
   * @param curriculumMembership The CurriculumMembership to get the starting date from.
   * @return The curriculum starting date.
   */
  private String getCurriculumStartDate(CurriculumMembership curriculumMembership) {
    return curriculumMembership.getData().get(CURRICULUM_START_DATE);
  }

  /**
   * Get the EndDate for the curriculum.
   *
   * @param curriculumMembership The CurriculumMembership to get the curriculum end date from.
   * @return The curriculum end date.
   *     <p>
   *     Note: this is taken from the curriculumMembership, NOT the curriculum
   *     </p>
   */
  private String getCurriculumEndDate(CurriculumMembership curriculumMembership) {
    return curriculumMembership.getData().get(CURRICULUM_END_DATE);
  }
}

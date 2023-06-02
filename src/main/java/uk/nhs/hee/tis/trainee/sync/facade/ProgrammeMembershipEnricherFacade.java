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

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.trainee.sync.dto.AggregateCurriculumDto;
import uk.nhs.hee.tis.trainee.sync.dto.AggregateProgrammeMembershipDto;
import uk.nhs.hee.tis.trainee.sync.mapper.CurriculumMapper;
import uk.nhs.hee.tis.trainee.sync.mapper.ProgrammeMembershipMapper;
import uk.nhs.hee.tis.trainee.sync.model.Curriculum;
import uk.nhs.hee.tis.trainee.sync.model.CurriculumMembership;
import uk.nhs.hee.tis.trainee.sync.model.Programme;
import uk.nhs.hee.tis.trainee.sync.model.ProgrammeMembership;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.service.CurriculumMembershipSyncService;
import uk.nhs.hee.tis.trainee.sync.service.CurriculumSyncService;
import uk.nhs.hee.tis.trainee.sync.service.ProgrammeMembershipSyncService;
import uk.nhs.hee.tis.trainee.sync.service.ProgrammeSyncService;
import uk.nhs.hee.tis.trainee.sync.service.TcsSyncService;

@Component
@Slf4j
public class ProgrammeMembershipEnricherFacade {
  private static final String PROGRAMME_MEMBERSHIP_CURRICULUM_ID = "curriculumId";

  private static final String PROGRAMME_NAME = "programmeName";
  private static final String PROGRAMME_NUMBER = "programmeNumber";
  private static final String MANAGING_DEANERY = "owner";
  // from programme membership

  private static final String CURRICULUM_NAME = "name";
  private static final String CURRICULUM_SUB_TYPE = "curriculumSubType";
  // from programme membership

  private final ProgrammeMembershipSyncService programmeMembershipService;
  private final ProgrammeSyncService programmeSyncService;
  private final CurriculumMembershipSyncService curriculumMembershipService;
  private final CurriculumSyncService curriculumSyncService;

  private final TcsSyncService tcsSyncService;
  private final CurriculumMapper curriculumMapper;
  private final ProgrammeMembershipMapper programmeMembershipMapper;

  ProgrammeMembershipEnricherFacade(ProgrammeMembershipSyncService programmeMembershipService,
      ProgrammeSyncService programmeSyncService,
      CurriculumMembershipSyncService curriculumMembershipService,
      CurriculumSyncService curriculumSyncService, TcsSyncService tcsSyncService,
      CurriculumMapper curriculumMapper, ProgrammeMembershipMapper programmeMembershipMapper) {
    this.programmeMembershipService = programmeMembershipService;
    this.programmeSyncService = programmeSyncService;
    this.curriculumMembershipService = curriculumMembershipService;
    this.curriculumSyncService = curriculumSyncService;
    this.tcsSyncService = tcsSyncService;
    this.curriculumMapper = curriculumMapper;
    this.programmeMembershipMapper = programmeMembershipMapper;
  }

  /**
   * Delete a programmeMembership from tis-trainee-details.
   *
   * @param programmeMembership The programme membership to delete.
   */
  public void delete(ProgrammeMembership programmeMembership) {
    AggregateProgrammeMembershipDto programmeMembershipDto = programmeMembershipMapper.toDto(programmeMembership);
    deleteAllPersonsProgrammeMemberships(programmeMembershipDto);

    HashSet<String> programmeMembershipsSynced = new HashSet<>();

    Set<ProgrammeMembership> allTheirOtherProgrammeMemberships = programmeMembershipService.findByPersonId(programmeMembershipDto.getPersonId());

    for (ProgrammeMembership theirProgrammeMembership : allTheirOtherProgrammeMemberships) {
      String similarKey = getProgrammeMembershipsSimilarKey(programmeMembershipDto);
      if (!programmeMembershipsSynced.contains(similarKey)) {
        AggregateProgrammeMembershipDto dto = programmeMembershipMapper.toDto(theirProgrammeMembership);
        enrich(dto, true, true, false);
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

      programmeMembershipMapper.toDtos(programmeMemberships).forEach(dto -> {
        populateCurriculumDetails(dto, curriculum);
        enrich(dto, true, false, false);
      });
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

      programmeMembershipMapper.toDtos(programmeMemberships).forEach(dto -> {
        programmeMembershipMapper.populateProgrammeData(dto, programme);
        enrich(dto, false, true, false);
      });
    }
  }

  /**
   * Sync an enriched programmeMembership with the programmeMembership as the starting object.
   *
   * @param programmeMembership The programmeMembership to enrich.
   */
  public void enrich(ProgrammeMembership programmeMembership) {
    AggregateProgrammeMembershipDto dto = programmeMembershipMapper.toDto(programmeMembership);
    enrich(dto, true, true, true);
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
  private void enrich(AggregateProgrammeMembershipDto programmeMembership,
      boolean doProgrammeEnrich,
      boolean doCurriculumEnrich,
      boolean doRebuildPersonsProgrammeMemberships) {
    boolean doSync = true;

    if (doProgrammeEnrich) {
      String programmeId = programmeMembership.getProgrammeTisId();

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
      Set<CurriculumMembership> curriculumMemberships = curriculumMembershipService.findByProgrammeMembershipUuid(programmeMembership.getTisId());

      if (curriculumMemberships.isEmpty()) {
        curriculumMembershipService.requestForProgrammeMembership(programmeMembership.getTisId());
        doSync = false;
      } else {
        for (CurriculumMembership curriculumMembership : curriculumMemberships) {
          String curriculumId = curriculumMembership.getData()
              .get(PROGRAMME_MEMBERSHIP_CURRICULUM_ID);
          Optional<Curriculum> optionalCurriculum = curriculumSyncService.findById(curriculumId);

          if (optionalCurriculum.isPresent()) {
            doSync &= enrich(programmeMembership, optionalCurriculum.get());
          } else {
            curriculumSyncService.request(curriculumId);
            doSync = false;
          }
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
  private boolean enrich(AggregateProgrammeMembershipDto programmeMembership, Curriculum curriculum) {
    String curriculumName = getCurriculumName(curriculum);

    if (curriculumName != null) {
      populateCurriculumDetails(programmeMembership, curriculum);
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
  private boolean enrich(AggregateProgrammeMembershipDto programmeMembership, Programme programme) {
    String programmeName = getProgrammeName(programme);
    String programmeTisId = programme.getTisId();
    String programmeNumber = getProgrammeNumber(programme);
    String managingDeanery = getManagingDeanery(programme);

    if (programmeName != null || programmeTisId != null || programmeNumber != null
        || managingDeanery != null) {
      programmeMembershipMapper.populateProgrammeData(programmeMembership, programme);
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
  void syncAggregateProgrammeMembership(
      AggregateProgrammeMembershipDto aggregateProgrammeMembership,
      boolean doRebuildPersonsProgrammeMemberships) {

    if (doRebuildPersonsProgrammeMemberships) {
      deleteAllPersonsProgrammeMemberships(aggregateProgrammeMembership);

      HashSet<String> programmeMembershipsSynced = new HashSet<>();
      syncProgrammeMembership(aggregateProgrammeMembership);
      programmeMembershipsSynced.add(getProgrammeMembershipsSimilarKey(aggregateProgrammeMembership));

      Set<ProgrammeMembership> allTheirProgrammeMemberships =
          programmeMembershipService.findByPersonId(aggregateProgrammeMembership.getPersonId());

      for (ProgrammeMembership theirProgrammeMembership : allTheirProgrammeMemberships) {
        AggregateProgrammeMembershipDto dto = programmeMembershipMapper.toDto(theirProgrammeMembership);
        if (!programmeMembershipsSynced.contains(getProgrammeMembershipsSimilarKey(dto))) {
          enrich(dto, true, true, false);
          programmeMembershipsSynced.add(getProgrammeMembershipsSimilarKey(dto));
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
   * @param curriculum          The curriculum to enrich with.
   */
  private void populateCurriculumDetails(AggregateProgrammeMembershipDto programmeMembership,
      Curriculum curriculum) {
    // Add extra data to programmeMembership data. This is unpacked again in
    // syncProgrammeMembership(ProgrammeMembership programmeMembership)
    // to derive the aggregate programmeMembership record.
    if (Strings.isNotBlank(getCurriculumName(curriculum))) {
      AggregateCurriculumDto aggregateCurriculumDto = curriculumMapper.toDto(curriculum);

      // TODO: create service method to get CM by Curriculum ID plus PM UUID.
      Set<CurriculumMembership> curriculumMemberships = curriculumMembershipService.findByCurriculumId(
          aggregateCurriculumDto.getCurriculumTisId());
      Optional<CurriculumMembership> curriculumMembership = curriculumMemberships.stream()
          .filter(cm -> cm.getData().get("programmeMembershipUuid")
              .equals(programmeMembership.getTisId())).findFirst();

      curriculumMembership.ifPresent(
          cm -> curriculumMapper.populateCurriculumMembershipData(aggregateCurriculumDto, cm));

      programmeMembership.getCurricula().add(aggregateCurriculumDto);
    }
  }

  /**
   * Sync the (completely enriched) programmeMembership, aggregating with similar
   * programmeMemberships. Note: 'similar' is defined as sharing the same personId, programmeId,
   * programmeStartDate, programmeEndDate and programmeMembershipType.
   *
   * @param programmeMembership The programmeMembership to sync.
   */
  private void syncProgrammeMembership(AggregateProgrammeMembershipDto programmeMembership) {
    // initialise properties that will be aggregated
    // programmeCompletionDate
    // TODO: is this always null here?
    LocalDate currentProgrammeCompletionDate = programmeMembership.getProgrammeCompletionDate();
    //curricula
    List<AggregateCurriculumDto> allCurricula = programmeMembership.getCurricula();

    if (allCurricula != null) {
      LocalDate maxProgrammeCompletionDate = allCurricula.stream()
          .map(AggregateCurriculumDto::getCurriculumEndDate)
          .filter(Objects::nonNull)
          .filter(endDate -> currentProgrammeCompletionDate == null || endDate.isAfter(
              currentProgrammeCompletionDate))
          .max(LocalDate::compareTo)
          .orElse(currentProgrammeCompletionDate);

      programmeMembership.setProgrammeCompletionDate(maxProgrammeCompletionDate);
    }

    Record programmeMembershipRecord = programmeMembershipMapper.toRecord(programmeMembership);

    // Set the required metadata so the record can be synced using common logic.
    programmeMembershipRecord.setOperation(LOAD);
    programmeMembershipRecord.setSchema("tcs");
    programmeMembershipRecord.setTable("ProgrammeMembership");

    // sync the complete aggregate programmeMembership record
    tcsSyncService.syncRecord(programmeMembershipRecord);
  }

  /**
   * Delete all programmeMemberships for the person.
   *
   * @param programmeMembership The programme membership to retrieve the person from
   */
  private void deleteAllPersonsProgrammeMemberships(AggregateProgrammeMembershipDto programmeMembership) {
    Record pmRecord = programmeMembershipMapper.toRecord(programmeMembership);
    pmRecord.setOperation(DELETE);
    pmRecord.setSchema("tcs");
    pmRecord.setTable("ProgrammeMembership");
    tcsSyncService.syncRecord(pmRecord); // delete all programme memberships for personId
  }

  /**
   * Get the programme memberships similar to the passed programme memberships. Note: 'similar'
   * means sharing the same personId, programmeId, programmeStartDate, programmeEndDate and
   * programmeMembershipType.
   *
   * @param programmeMembership The programme membership to use as the criteria
   * @return The set of similar programme memberships.
   */
  private String getProgrammeMembershipsSimilarKey(AggregateProgrammeMembershipDto programmeMembership) {
    StringBuilder key = new StringBuilder();
    key.append(programmeMembership.getPersonId());
    key.append(".");
    key.append(programmeMembership.getProgrammeTisId());
    key.append(".");
    key.append(programmeMembership.getProgrammeMembershipType());
    key.append(".");
    key.append(programmeMembership.getStartDate());
    key.append(".");
    key.append(programmeMembership.getEndDate());
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
}

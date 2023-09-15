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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.trainee.sync.dto.AggregateCurriculumMembershipDto;
import uk.nhs.hee.tis.trainee.sync.dto.AggregateProgrammeMembershipDto;
import uk.nhs.hee.tis.trainee.sync.mapper.AggregateMapper;
import uk.nhs.hee.tis.trainee.sync.model.ConditionsOfJoining;
import uk.nhs.hee.tis.trainee.sync.model.Curriculum;
import uk.nhs.hee.tis.trainee.sync.model.CurriculumMembership;
import uk.nhs.hee.tis.trainee.sync.model.Programme;
import uk.nhs.hee.tis.trainee.sync.model.ProgrammeMembership;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.service.ConditionsOfJoiningSyncService;
import uk.nhs.hee.tis.trainee.sync.service.CurriculumMembershipSyncService;
import uk.nhs.hee.tis.trainee.sync.service.CurriculumSyncService;
import uk.nhs.hee.tis.trainee.sync.service.ProgrammeSyncService;
import uk.nhs.hee.tis.trainee.sync.service.TcsSyncService;

@Component
@Slf4j
public class ProgrammeMembershipEnricherFacade {

  private static final String PROGRAMME_MEMBERSHIP_CURRICULUM_ID = "curriculumId";
  private final ProgrammeSyncService programmeSyncService;
  private final ConditionsOfJoiningSyncService conditionsOfJoiningSyncService;
  private final CurriculumMembershipSyncService curriculumMembershipService;
  private final CurriculumSyncService curriculumSyncService;

  private final TcsSyncService tcsSyncService;
  private final AggregateMapper aggregateMapper;

  ProgrammeMembershipEnricherFacade(ProgrammeSyncService programmeSyncService,
      ConditionsOfJoiningSyncService conditionsOfJoiningSyncService,
      CurriculumMembershipSyncService curriculumMembershipService,
      CurriculumSyncService curriculumSyncService, TcsSyncService tcsSyncService,
      AggregateMapper aggregateMapper) {
    this.programmeSyncService = programmeSyncService;
    this.conditionsOfJoiningSyncService = conditionsOfJoiningSyncService;
    this.curriculumMembershipService = curriculumMembershipService;
    this.curriculumSyncService = curriculumSyncService;
    this.tcsSyncService = tcsSyncService;
    this.aggregateMapper = aggregateMapper;
  }

  /**
   * Sync an enriched programmeMembership with the programmeMembership as the starting object.
   *
   * @param programmeMembership The programmeMembership to enrich.
   */
  public void enrich(ProgrammeMembership programmeMembership) {
    List<AggregateCurriculumMembershipDto> aggregatedCurriculumMemberships =
        buildCurriculumMemberships(programmeMembership);
    Programme programme = getProgramme(programmeMembership);

    if (!aggregatedCurriculumMemberships.isEmpty() && programme != null) {
      // TODO: validate the aggregated data to ensure we have a "complete" PM?
      ConditionsOfJoining conditionsOfJoining = getConditionsOfJoining(programmeMembership);

      AggregateProgrammeMembershipDto aggregateProgrammeMembership =
          aggregateMapper.toAggregateProgrammeMembershipDto(programmeMembership, programme,
              aggregatedCurriculumMemberships, conditionsOfJoining);
      syncProgrammeMembership(aggregateProgrammeMembership);
    }
  }

  /**
   * Get the aggregated curriculum membership data for the given programme membership, any missing
   * data will be requested.
   *
   * @param programmeMembership The programme membership to get the curriculum memberships for.
   * @return The list of aggregated curriculum membership data, or an empty list if not all data was
   *     available.
   */
  private List<AggregateCurriculumMembershipDto> buildCurriculumMemberships(
      ProgrammeMembership programmeMembership) {
    List<AggregateCurriculumMembershipDto> aggregatedCurriculumMemberships = new ArrayList<>();
    boolean dataRequested = false;

    String programmeMembershipUuid = programmeMembership.getUuid().toString();
    Set<CurriculumMembership> curriculumMemberships =
        curriculumMembershipService.findByProgrammeMembershipUuid(programmeMembershipUuid);

    if (curriculumMemberships.isEmpty()) {
      curriculumMembershipService.requestForProgrammeMembership(programmeMembershipUuid);
      dataRequested = true;
    } else {
      for (CurriculumMembership curriculumMembership : curriculumMemberships) {
        String curriculumId = curriculumMembership.getData()
            .get(PROGRAMME_MEMBERSHIP_CURRICULUM_ID);
        Optional<Curriculum> optionalCurriculum = curriculumSyncService.findById(curriculumId);

        if (optionalCurriculum.isPresent()) {
          Curriculum curriculum = optionalCurriculum.get();
          aggregatedCurriculumMemberships.add(
              aggregateMapper.toAggregateCurriculumMembershipDto(curriculum, curriculumMembership));
        } else {
          curriculumSyncService.request(curriculumId);
          dataRequested = true;
        }
      }
    }

    if (dataRequested) {
      log.info(
          "CurriculumMembership aggregation skipped for ProgrammeMembership {}.",
          programmeMembershipUuid);
    }

    return dataRequested ? List.of() : aggregatedCurriculumMemberships;
  }

  /**
   * Get the programme data associated with the given programme membership, any missing data will be
   * requested.
   *
   * @param programmeMembership The programme membership to get the programme for.
   * @return The programme, or null if the data was unavailable and requested.
   */
  private Programme getProgramme(ProgrammeMembership programmeMembership) {
    Programme programme = null;
    Long programmeId = programmeMembership.getProgrammeId();

    if (programmeId != null) {
      Optional<Programme> optionalProgramme = programmeSyncService.findById(programmeId.toString());

      if (optionalProgramme.isPresent()) {
        programme = optionalProgramme.get();
      } else {
        programmeSyncService.request(programmeId.toString());
      }
    }

    return programme;
  }

  /**
   * Get the Conditions of joining data associated with the given programme membership.
   *
   * @param programmeMembership The programme membership to get the Conditions of joining for.
   * @return The Conditions of joining, or null if the data was unavailable.
   */
  private ConditionsOfJoining getConditionsOfJoining(ProgrammeMembership programmeMembership) {
    ConditionsOfJoining conditionsOfJoining = null;
    UUID programmeMembershipUuid = programmeMembership.getUuid();

    if (programmeMembershipUuid != null) {
      Optional<ConditionsOfJoining> optionalConditionsOfJoining
          = conditionsOfJoiningSyncService.findById(programmeMembershipUuid.toString());

      if (optionalConditionsOfJoining.isPresent()) {
        conditionsOfJoining = optionalConditionsOfJoining.get();
      }
    }

    return conditionsOfJoining;
  }

  /**
   * Sync the (completely enriched) programmeMembership.
   *
   * @param programmeMembership The programmeMembership to sync.
   */
  private void syncProgrammeMembership(AggregateProgrammeMembershipDto programmeMembership) {
    Record programmeMembershipRecord = aggregateMapper.toRecord(programmeMembership);

    // Set the required metadata so the record can be synced using common logic.
    programmeMembershipRecord.setOperation(LOAD);
    programmeMembershipRecord.setSchema("tcs");
    programmeMembershipRecord.setTable("ProgrammeMembership");

    // sync the complete aggregate programmeMembership record
    tcsSyncService.syncRecord(programmeMembershipRecord);
  }
}

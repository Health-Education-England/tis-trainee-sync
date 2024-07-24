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

import static uk.nhs.hee.tis.trainee.sync.event.DbcEventListener.DBC_DBC;
import static uk.nhs.hee.tis.trainee.sync.event.LocalOfficeEventListener.LOCAL_OFFICE_ABBREVIATION;
import static uk.nhs.hee.tis.trainee.sync.event.ProgrammeEventListener.PROGRAMME_OWNER;
import static uk.nhs.hee.tis.trainee.sync.event.UserDesignatedBodyEventListener.UDB_USER_NAME;
import static uk.nhs.hee.tis.trainee.sync.model.Operation.LOAD;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.trainee.sync.dto.AggregateCurriculumMembershipDto;
import uk.nhs.hee.tis.trainee.sync.dto.AggregateProgrammeMembershipDto;
import uk.nhs.hee.tis.trainee.sync.dto.ProgrammeMembershipEventDto;
import uk.nhs.hee.tis.trainee.sync.mapper.AggregateMapper;
import uk.nhs.hee.tis.trainee.sync.mapper.HeeUserMapper;
import uk.nhs.hee.tis.trainee.sync.mapper.ProgrammeMembershipEventMapper;
import uk.nhs.hee.tis.trainee.sync.model.ConditionsOfJoining;
import uk.nhs.hee.tis.trainee.sync.model.Curriculum;
import uk.nhs.hee.tis.trainee.sync.model.CurriculumMembership;
import uk.nhs.hee.tis.trainee.sync.model.Dbc;
import uk.nhs.hee.tis.trainee.sync.model.HeeUser;
import uk.nhs.hee.tis.trainee.sync.model.LocalOffice;
import uk.nhs.hee.tis.trainee.sync.model.Programme;
import uk.nhs.hee.tis.trainee.sync.model.ProgrammeMembership;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.model.Specialty;
import uk.nhs.hee.tis.trainee.sync.model.UserDesignatedBody;
import uk.nhs.hee.tis.trainee.sync.model.UserRole;
import uk.nhs.hee.tis.trainee.sync.service.ConditionsOfJoiningSyncService;
import uk.nhs.hee.tis.trainee.sync.service.CurriculumMembershipSyncService;
import uk.nhs.hee.tis.trainee.sync.service.CurriculumSyncService;
import uk.nhs.hee.tis.trainee.sync.service.DbcSyncService;
import uk.nhs.hee.tis.trainee.sync.service.HeeUserSyncService;
import uk.nhs.hee.tis.trainee.sync.service.LocalOfficeSyncService;
import uk.nhs.hee.tis.trainee.sync.service.ProgrammeSyncService;
import uk.nhs.hee.tis.trainee.sync.service.SpecialtySyncService;
import uk.nhs.hee.tis.trainee.sync.service.TcsSyncService;
import uk.nhs.hee.tis.trainee.sync.service.UserDesignatedBodySyncService;
import uk.nhs.hee.tis.trainee.sync.service.UserRoleSyncService;

@Component
@Slf4j
public class ProgrammeMembershipEnricherFacade {

  private static final String PROGRAMME_MEMBERSHIP_CURRICULUM_ID = "curriculumId";
  private static final String CURRICULUM_MEMBERSHIP_SPECIALTY_ID = "specialtyId";
  private final ProgrammeSyncService programmeSyncService;
  private final ConditionsOfJoiningSyncService conditionsOfJoiningSyncService;
  private final CurriculumMembershipSyncService curriculumMembershipService;
  private final CurriculumSyncService curriculumSyncService;
  private final SpecialtySyncService specialtySyncService;
  private final LocalOfficeSyncService localOfficeSyncService;
  private final DbcSyncService dbcSyncService;
  private final UserDesignatedBodySyncService userDesignatedBodyService;
  private final UserRoleSyncService userRoleService;
  private final HeeUserSyncService heeUserService;

  private final TcsSyncService tcsSyncService;
  private final AggregateMapper aggregateMapper;
  private final ProgrammeMembershipEventMapper eventMapper;
  private final HeeUserMapper heeUserMapper;

  ProgrammeMembershipEnricherFacade(ProgrammeSyncService programmeSyncService,
      ConditionsOfJoiningSyncService conditionsOfJoiningSyncService,
      CurriculumMembershipSyncService curriculumMembershipService,
      CurriculumSyncService curriculumSyncService, SpecialtySyncService specialtySyncService,
      TcsSyncService tcsSyncService, LocalOfficeSyncService localOfficeSyncService,
      DbcSyncService dbcSyncService, UserDesignatedBodySyncService userDesignatedBodyService,
      UserRoleSyncService userRoleService, HeeUserSyncService heeUserService,
      AggregateMapper aggregateMapper, ProgrammeMembershipEventMapper eventMapper,
      HeeUserMapper heeUserMapper) {
    this.programmeSyncService = programmeSyncService;
    this.conditionsOfJoiningSyncService = conditionsOfJoiningSyncService;
    this.curriculumMembershipService = curriculumMembershipService;
    this.curriculumSyncService = curriculumSyncService;
    this.specialtySyncService = specialtySyncService;
    this.tcsSyncService = tcsSyncService;
    this.localOfficeSyncService = localOfficeSyncService;
    this.dbcSyncService = dbcSyncService;
    this.userDesignatedBodyService = userDesignatedBodyService;
    this.userRoleService = userRoleService;
    this.heeUserService = heeUserService;
    this.aggregateMapper = aggregateMapper;
    this.eventMapper = eventMapper;
    this.heeUserMapper = heeUserMapper;
  }

  /**
   * Get the aggregated programme membership data for the given programme membership, any missing
   * data will be requested.
   *
   * @param programmeMembership The programme membership to get the aggregate programme membership
   *                            for.
   * @return The aggregated programme membership, or null if not all data was available.
   */
  public AggregateProgrammeMembershipDto buildAggregateProgrammeMembershipDto(
      ProgrammeMembership programmeMembership) {
    List<AggregateCurriculumMembershipDto> aggregatedCurriculumMemberships =
        buildCurriculumMemberships(programmeMembership);
    Programme programme = getProgramme(programmeMembership);
    if (!aggregatedCurriculumMemberships.isEmpty() && programme != null) {
      // TODO: validate the aggregated data to ensure we have a "complete" PM?
      LocalOffice localOffice = getLocalOffice(programme);
      Dbc dbc = getDbc(localOffice);
      HeeUser responsibleOfficer = getResponsibleOfficer(dbc);
      ConditionsOfJoining conditionsOfJoining = getConditionsOfJoining(programmeMembership);

      return aggregateMapper.toAggregateProgrammeMembershipDto(programmeMembership, programme,
          aggregatedCurriculumMemberships, conditionsOfJoining, dbc, responsibleOfficer);
    } else {
      return null;
    }
  }

  /**
   * Broadcast a conditions of joining sync event.
   *
   * @param programmeMembership The programme membership to which the Conditions of Joining
   *                            belongs.
   */
  public void broadcastCoj(ProgrammeMembership programmeMembership) {
    AggregateProgrammeMembershipDto aggregatePmDto
        = buildAggregateProgrammeMembershipDto(programmeMembership);

    if (aggregatePmDto != null) {
      ProgrammeMembershipEventDto pmEventDto
          = eventMapper.toProgrammeMembershipEventDto(aggregatePmDto);
      tcsSyncService.publishDetailsChangeEvent(pmEventDto);
    } else {
      log.warn("Aggregate programme membership (uuid '{}') could not be built, so CoJ "
              + "signing event could not be broadcast. This may reflect a data issue: all"
              + "related programme and curriculum data SHOULD be present in the database.",
          programmeMembership.getUuid());
    }
  }

  /**
   * Sync an enriched programmeMembership with the programmeMembership as the starting object.
   *
   * @param programmeMembership The programmeMembership to enrich.
   */
  public void enrich(ProgrammeMembership programmeMembership) {
    AggregateProgrammeMembershipDto aggregateProgrammeMembership =
        buildAggregateProgrammeMembershipDto(programmeMembership);

    if (aggregateProgrammeMembership != null) {
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
          String specialtyId = curriculum.getData().get(CURRICULUM_MEMBERSHIP_SPECIALTY_ID);
          Optional<Specialty> optionalSpecialty = specialtySyncService.findById(specialtyId);
          if (optionalSpecialty.isPresent()) {
            Specialty specialty = optionalSpecialty.get();
            aggregatedCurriculumMemberships.add(
                aggregateMapper.toAggregateCurriculumMembershipDto(curriculum,
                    curriculumMembership, specialty));
          } else {
            specialtySyncService.request(specialtyId);
            dataRequested = true;
          }
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
   * Get the DBC data associated with the given local office, any missing data will be requested.
   *
   * @param localOffice The local office to get the DBC for.
   * @return The DBC, or null if the data was unavailable and requested.
   */
  private Dbc getDbc(LocalOffice localOffice) {
    Dbc dbc = null;
    if (localOffice != null) {
      String abbr = localOffice.getData().get(LOCAL_OFFICE_ABBREVIATION);

      if (abbr != null) {
        Optional<Dbc> optionalDbc = dbcSyncService.findByAbbr(abbr);

        if (optionalDbc.isPresent()) {
          dbc = optionalDbc.get();
        } else {
          dbcSyncService.requestByAbbr(abbr);
        }
      }
    }

    return dbc;
  }

  /**
   * Get the LocalOffice data associated with the given programme, any missing data will be
   * requested.
   *
   * @param programme The programme to get the local office for.
   * @return The local office, or null if the data was unavailable and requested.
   */
  private LocalOffice getLocalOffice(Programme programme) {
    LocalOffice localOffice = null;
    String localOfficeName = programme.getData().get(PROGRAMME_OWNER);

    if (localOfficeName != null) {
      Optional<LocalOffice> optionalLocalOffice = localOfficeSyncService.findByName(
          localOfficeName);

      if (optionalLocalOffice.isPresent()) {
        localOffice = optionalLocalOffice.get();
      } else {
        localOfficeSyncService.requestByName(localOfficeName);
      }
    }

    return localOffice;
  }

  private HeeUser getResponsibleOfficer(Dbc dbc) {
    AtomicReference<HeeUser> ro = new AtomicReference<>();
    if (dbc != null) {
      Set<UserDesignatedBody> udbSet
          = userDesignatedBodyService.findByDbc(dbc.getData().get(DBC_DBC));

      for (UserDesignatedBody udb : udbSet) {
        if (ro.get() == null) {
          //once we have a responsible officer, ignore any remaining users linked to the DB.
          String username = udb.getData().get(UDB_USER_NAME);
          Optional<UserRole> userRoleOptional = userRoleService.findRvOfficerRoleByUserName(
              username);
          if (userRoleOptional.isPresent()) {
            Optional<HeeUser> heeUserOptional = heeUserService.findByName(username);
            heeUserOptional.ifPresent(ro::set);
          }
        }
      }
    }
    return ro.get();
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

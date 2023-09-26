/*
 * The MIT License (MIT)
 *
 * Copyright 2023 Crown Copyright (Health Education England)
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

import static uk.nhs.hee.tis.trainee.sync.model.Operation.DELETE;

import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.trainee.sync.dto.AggregateProgrammeMembershipDto;
import uk.nhs.hee.tis.trainee.sync.mapper.AggregateMapper;
import uk.nhs.hee.tis.trainee.sync.mapper.ConditionsOfJoiningMapper;
import uk.nhs.hee.tis.trainee.sync.mapper.ProgrammeMembershipEventMapper;
import uk.nhs.hee.tis.trainee.sync.model.ConditionsOfJoining;
import uk.nhs.hee.tis.trainee.sync.model.Programme;
import uk.nhs.hee.tis.trainee.sync.model.ProgrammeMembership;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.repository.ConditionsOfJoiningRepository;


@Slf4j
@Service("tcs-ConditionsOfJoining")
public class ConditionsOfJoiningSyncService implements SyncService {

  private final ConditionsOfJoiningRepository repository;
  private final ProgrammeMembershipSyncService programmeMembershipService;
  private final ProgrammeSyncService programmeSyncService;
  private final ProgrammeMembershipEventMapper programmeMembershipEventMapper;
  private final AggregateMapper aggregateMapper;
  private final ConditionsOfJoiningMapper mapper;
  private final TcsSyncService tcsSyncService;

  ConditionsOfJoiningSyncService(ConditionsOfJoiningRepository repository,
      ProgrammeMembershipSyncService programmeMembershipService,
      ProgrammeSyncService programmeSyncService,
      ProgrammeMembershipEventMapper programmeMembershipEventMapper,
      AggregateMapper aggregateMapper,
      ConditionsOfJoiningMapper mapper,
      TcsSyncService tcsSyncService) {
    this.repository = repository;
    this.programmeMembershipService = programmeMembershipService;
    this.programmeSyncService = programmeSyncService;
    this.mapper = mapper;
    this.aggregateMapper = aggregateMapper;
    this.programmeMembershipEventMapper = programmeMembershipEventMapper;
    this.tcsSyncService = tcsSyncService;
  }

  @Override
  public void syncRecord(Record conditionsOfJoiningRecord) {
    if (!Objects.equals(conditionsOfJoiningRecord.getTable(), ConditionsOfJoining.ENTITY_NAME)) {
      String message = String.format("Invalid record type '%s'.",
          conditionsOfJoiningRecord.getClass());
      throw new IllegalArgumentException(message);
    }

    ConditionsOfJoining conditionsOfJoining = mapper.toEntity(conditionsOfJoiningRecord.getData());

    if (conditionsOfJoiningRecord.getOperation().equals(DELETE)) {
      repository.deleteById(conditionsOfJoining.getProgrammeMembershipUuid());
    } else {
      Optional<ConditionsOfJoining> savedCoj
          = findById(conditionsOfJoining.getProgrammeMembershipUuid());
      savedCoj.ifPresent(
          ofJoining -> conditionsOfJoining.setSyncedAt(ofJoining.getSyncedAt()));

      //saving the Conditions of Joining may set its syncedAt value
      ConditionsOfJoining savedConditionsOfJoining = repository.save(conditionsOfJoining);

      Optional<ProgrammeMembership> optionalProgrammeMembership
          = programmeMembershipService.findById(conditionsOfJoining.getProgrammeMembershipUuid());

      if (optionalProgrammeMembership.isPresent()) {
        Optional<Programme> optionalProgramme = programmeSyncService.findById(
            optionalProgrammeMembership.get().getProgrammeId().toString());

        if (optionalProgramme.isPresent()) {
          AggregateProgrammeMembershipDto aggregateProgrammeMembershipDto
              = aggregateMapper.toAggregateProgrammeMembershipDto(
              optionalProgrammeMembership.get(),
              optionalProgramme.get(),
              null, //TODO: should we populate this for completeness?
              savedConditionsOfJoining);

          Record programmeMembershipEventRecord
              = programmeMembershipEventMapper.toRecord(aggregateProgrammeMembershipDto);
          programmeMembershipEventRecord.setOperation(conditionsOfJoiningRecord.getOperation());
          tcsSyncService.publishDetailsChangeEvent(programmeMembershipEventRecord);
        }
      }
    }
  }

  public Optional<ConditionsOfJoining> findById(String id) {
    return repository.findById(id);
  }
}

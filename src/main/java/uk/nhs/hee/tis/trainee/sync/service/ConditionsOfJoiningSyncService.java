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
import uk.nhs.hee.tis.trainee.sync.mapper.ConditionsOfJoiningMapper;
import uk.nhs.hee.tis.trainee.sync.model.ConditionsOfJoining;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.repository.ConditionsOfJoiningRepository;


@Slf4j
@Service("tcs-ConditionsOfJoining")
public class ConditionsOfJoiningSyncService implements SyncService {

  private final ConditionsOfJoiningRepository repository;
  private final ConditionsOfJoiningMapper mapper;

  ConditionsOfJoiningSyncService(ConditionsOfJoiningRepository repository,
      ConditionsOfJoiningMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
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
      repository.save(conditionsOfJoining);
    }
  }

  public Optional<ConditionsOfJoining> findById(String id) {
    return repository.findById(id);
  }
}

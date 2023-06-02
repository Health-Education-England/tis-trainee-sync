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

package uk.nhs.hee.tis.trainee.sync.service;

import static uk.nhs.hee.tis.trainee.sync.model.Operation.DELETE;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.trainee.sync.mapper.ProgrammeMembershipMapper;
import uk.nhs.hee.tis.trainee.sync.model.ProgrammeMembership;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.repository.ProgrammeMembershipRepository;

@Slf4j
@Service("tcs-ProgrammeMembership")
public class ProgrammeMembershipSyncService implements SyncService {

  private final ProgrammeMembershipRepository repository;

  private final DataRequestService dataRequestService;

  private final RequestCacheService requestCacheService;
  private final ProgrammeMembershipMapper mapper;

  ProgrammeMembershipSyncService(ProgrammeMembershipRepository repository,
      DataRequestService dataRequestService, RequestCacheService requestCacheService,
      ProgrammeMembershipMapper mapper) {
    this.repository = repository;
    this.dataRequestService = dataRequestService;
    this.requestCacheService = requestCacheService;
    this.mapper = mapper;
  }

  @Override
  public void syncRecord(Record programmeMembershipRecord) {
    ProgrammeMembership programmeMembership = mapper.toEntity(programmeMembershipRecord.getData());

    if (programmeMembershipRecord.getOperation().equals(DELETE)) {
      repository.deleteById(programmeMembership.getUuid());
    } else {
      repository.save(programmeMembership);
    }

    requestCacheService.deleteItemFromCache(ProgrammeMembership.ENTITY_NAME,
        programmeMembership.getUuid().toString());
  }

  public Optional<ProgrammeMembership> findById(String uuid) {
    return repository.findById(UUID.fromString(uuid));
  }

  public Set<ProgrammeMembership> findByProgrammeId(String programmeId) {
    return repository.findByProgrammeId(Long.parseLong(programmeId));
  }

  public Set<ProgrammeMembership> findByCurriculumId(String curriculumId) {
    // TODO: get PMs for curriculum ID.
    return Collections.emptySet();
  }

  public Set<ProgrammeMembership> findByPersonId(String personId) {
    return repository.findByPersonId(Long.parseLong(personId));
  }

  public Set<ProgrammeMembership> findBySimilar(String personId, String programmeId,
      String programmeMembershipType, String programmeStartDate, String programmeEndDate) {
    return repository.findBySimilar(Long.parseLong(personId), Long.parseLong(programmeId),
        programmeMembershipType,
        LocalDate.parse(programmeStartDate), LocalDate.parse(programmeEndDate));
  }

  /**
   * Make a request to retrieve a specific programme membership.
   *
   * @param uuid The uuid of the post to be retrieved.
   */
  public void request(UUID uuid) {
    String uuidString = uuid.toString();

    if (!requestCacheService.isItemInCache(ProgrammeMembership.ENTITY_NAME, uuidString)) {
      log.info("Sending request for ProgrammeMembership [{}]", uuidString);

      try {
        requestCacheService.addItemToCache(ProgrammeMembership.ENTITY_NAME, uuidString,
            dataRequestService.sendRequest(ProgrammeMembership.ENTITY_NAME,
                Map.of("uuid", uuidString)));
      } catch (JsonProcessingException e) {
        log.error("Error while trying to request a ProgrammeMembership", e);
      }
    } else {
      log.debug("Already requested ProgrammeMembership [{}].", uuidString);
    }
  }
}

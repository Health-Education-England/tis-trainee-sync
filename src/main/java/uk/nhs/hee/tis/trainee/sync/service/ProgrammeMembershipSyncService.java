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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.trainee.sync.model.Programme;
import uk.nhs.hee.tis.trainee.sync.model.ProgrammeMembership;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.repository.ProgrammeMembershipRepository;

@Slf4j
@Service("tcs-ProgrammeMembership")
public class ProgrammeMembershipSyncService implements SyncService {

  private final ProgrammeMembershipRepository repository;

  private final DataRequestService dataRequestService;

  private final CacheService cacheService;

  ProgrammeMembershipSyncService(ProgrammeMembershipRepository repository,
      DataRequestService dataRequestService, CacheService cacheService) {
    this.repository = repository;
    this.dataRequestService = dataRequestService;
    this.cacheService = cacheService;
    this.cacheService.setKeyPrefix(ProgrammeMembership.ENTITY_NAME);
  }

  @Override
  public void syncRecord(Record programmeMembership) {
    if (!(programmeMembership instanceof ProgrammeMembership)) {
      String message = String.format("Invalid record type '%s'.", programmeMembership.getClass());
      throw new IllegalArgumentException(message);
    }

    if (programmeMembership.getOperation().equals(DELETE)) {
      repository.deleteById(programmeMembership.getTisId());
    } else {
      repository.save((ProgrammeMembership) programmeMembership);
    }

    cacheService.deleteItemFromCache(programmeMembership.getTisId());
  }

  public Optional<ProgrammeMembership> findById(String id) {
    return repository.findById(id);
  }

  public Set<ProgrammeMembership> findByProgrammeId(String programmeId) {
    return repository.findByProgrammeId(programmeId);
  }

  public Set<ProgrammeMembership> findByCurriculumId(String curriculumId) {
    return repository.findByCurriculumId(curriculumId);
  }

  public Set<ProgrammeMembership> findByPersonId(String personId) {
    return repository.findByPersonId(personId);
  }

  public Set<ProgrammeMembership> findBySimilar(String personId,
      String programmeId,
      String programmeMembershipType,
      String programmeStartDate,
      String programmeEndDate) {
    return repository.findBySimilar(personId, programmeId, programmeMembershipType,
        programmeStartDate, programmeEndDate);
  }

  /**
   * Make a request to retrieve a specific post.
   *
   * @param id The id of the post to be retrieved.
   */
  public void request(String id) {
    if (!cacheService.isItemInCache(id)) {
      log.info("Sending request for ProgrammeMembership [{}]", id);

      try {
        dataRequestService.sendRequest(ProgrammeMembership.ENTITY_NAME, Map.of("id", id));
        cacheService.addItemToCache(id);
      } catch (JsonProcessingException e) {
        log.error("Error while trying to request a ProgrammeMembership", e);
      }
    } else {
      log.debug("Already requested ProgrammeMembership [{}].", id);
    }
  }
}

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
import uk.nhs.hee.tis.trainee.sync.model.CurriculumMembership;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.repository.CurriculumMembershipRepository;

@Slf4j
@Service("tcs-CurriculumMembership")
public class CurriculumMembershipSyncService implements SyncService {

  private final CurriculumMembershipRepository repository;

  private final DataRequestService dataRequestService;

  private final CacheService cacheService;

  CurriculumMembershipSyncService(CurriculumMembershipRepository repository,
                                 DataRequestService dataRequestService,
                                 CacheService cacheService) {
    this.cacheService = cacheService;
    this.repository = repository;
    this.dataRequestService = dataRequestService;
  }

  @Override
  public void syncRecord(Record curriculumMembership) {
    if (!(curriculumMembership instanceof CurriculumMembership)) {
      String message = String.format("Invalid record type '%s'.", curriculumMembership.getClass());
      throw new IllegalArgumentException(message);
    }

    if (curriculumMembership.getOperation().equals(DELETE)) {
      repository.deleteById(curriculumMembership.getTisId());
    } else {
      repository.save((CurriculumMembership) curriculumMembership);
    }

    cacheService.deleteItemFromCache(curriculumMembership.getTisId());
  }

  public Optional<CurriculumMembership> findById(String id) {
    return repository.findById(id);
  }

  public Set<CurriculumMembership> findByProgrammeId(String programmeId) {
    return repository.findByProgrammeId(programmeId);
  }

  public Set<CurriculumMembership> findByCurriculumId(String curriculumId) {
    return repository.findByCurriculumId(curriculumId);
  }

  public Set<CurriculumMembership> findByPersonId(String personId) {
    return repository.findByPersonId(personId);
  }

  public Set<CurriculumMembership> findBySimilar(String personId,
                                                String programmeId,
                                                String programmeMembershipType,
                                                String programmeStartDate,
                                                String programmeEndDate) {
    return repository.findBySimilar(personId, programmeId, programmeMembershipType,
        programmeStartDate, programmeEndDate);
  }

  /**
   * Make a request to retrieve a specific Curriculum Membership.
   *
   * @param id The id of the Curriculum Membership to be retrieved.
   */
  public void request(String id) {
    if (!cacheService.isItemInCache(id)) {
      log.info("Sending request for CurriculumMembership [{}]", id);

      try {
        dataRequestService.sendRequest(CurriculumMembership.ENTITY_NAME, Map.of("id", id));
        cacheService.addItemToCache(id);
      } catch (JsonProcessingException e) {
        log.error("Error while trying to request a CurriculumMembership", e);
      }
    } else {
      log.debug("Already requested CurriculumMembership [{}].", id);
    }
  }
}

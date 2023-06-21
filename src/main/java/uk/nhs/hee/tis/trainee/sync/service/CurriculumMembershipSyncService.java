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
import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.trainee.sync.model.CurriculumMembership;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.repository.CurriculumMembershipRepository;


@Slf4j
@Service("tcs-CurriculumMembership")
public class CurriculumMembershipSyncService implements SyncService {

  private static final String PROGRAMME_MEMBERSHIP_UUID = "programmeMembershipUuid";

  private final CurriculumMembershipRepository repository;

  private final DataRequestService dataRequestService;

  private final RequestCacheService requestCacheService;

  private final QueueMessagingTemplate messagingTemplate;

  private final String queueUrl;

  CurriculumMembershipSyncService(CurriculumMembershipRepository repository,
      DataRequestService dataRequestService, QueueMessagingTemplate messagingTemplate,
      @Value("${application.aws.sqs.curriculum-membership}") String queueUrl,
      RequestCacheService requestCacheService) {
    this.repository = repository;
    this.dataRequestService = dataRequestService;
    this.messagingTemplate = messagingTemplate;
    this.queueUrl = queueUrl;
    this.requestCacheService = requestCacheService;
  }

  @Override
  public void syncRecord(Record curriculumMembership) {
    if (!(curriculumMembership instanceof CurriculumMembership)) {
      String message = String.format("Invalid record type '%s'.", curriculumMembership.getClass());
      throw new IllegalArgumentException(message);
    }

    // Send incoming records to the curriculum membership queue to be processed.
    messagingTemplate.convertAndSend(queueUrl, curriculumMembership);
  }

  /**
   * Synchronize the given curriculum membership.
   *
   * @param curriculumMembership The CurriculumMembership to synchronize.
   */
  public void syncCurriculumMembership(CurriculumMembership curriculumMembership) {
    if (curriculumMembership.getOperation().equals(DELETE)) {
      repository.deleteById(curriculumMembership.getTisId());
    } else {
      repository.save(curriculumMembership);
    }

    requestCacheService.deleteItemFromCache(CurriculumMembership.ENTITY_NAME,
        curriculumMembership.getTisId());
  }

  public Optional<CurriculumMembership> findById(String id) {
    return repository.findById(id);
  }

  public Set<CurriculumMembership> findByProgrammeId(String programmeId) {
    return repository.findByProgrammeId(programmeId);
  }

  /**
   * Find curriculum memberships with the given programme membership UUID.
   *
   * @param programmeMembershipUuid The UUID to search by.
   * @return The found curriculum memberships, empty if none found.
   */
  public Set<CurriculumMembership> findByProgrammeMembershipUuid(String programmeMembershipUuid) {
    return repository.findByProgrammeMembershipUuid(programmeMembershipUuid);
  }

  public Set<CurriculumMembership> findByCurriculumId(String curriculumId) {
    return repository.findByCurriculumId(curriculumId);
  }

  public Set<CurriculumMembership> findByPersonId(String personId) {
    return repository.findByPersonId(personId);
  }

  public Set<CurriculumMembership> findBySimilar(String personId, String programmeId,
      String programmeMembershipType, String programmeStartDate, String programmeEndDate) {
    return repository.findBySimilar(personId, programmeId, programmeMembershipType,
        programmeStartDate, programmeEndDate);
  }

  /**
   * Make a request to retrieve Curriculum Memberships for a specific Programme Membership.
   *
   * @param programmeMembershipId The id of the Programme Membership to retrieve Curriculum
   *                              Memberships for.
   */
  public void requestForProgrammeMembership(String programmeMembershipId) {
    if (!requestCacheService.isItemInCache(CurriculumMembership.ENTITY_NAME,
        programmeMembershipId)) {
      log.info("Sending request for CurriculumMemberships for Programme Membership [{}]",
          programmeMembershipId);

      try {
        requestCacheService.addItemToCache(CurriculumMembership.ENTITY_NAME, programmeMembershipId,
            dataRequestService.sendRequest(CurriculumMembership.ENTITY_NAME,
                Map.of(PROGRAMME_MEMBERSHIP_UUID, programmeMembershipId)));
      } catch (JsonProcessingException e) {
        log.error("Error while trying to request CurriculumMemberships", e);
      }
    } else {
      log.debug("Already requested CurriculumMemberships for Programme Membership [{}].",
          programmeMembershipId);
    }
  }
}

/*
 * The MIT License (MIT)
 *
 * Copyright 2024 Crown Copyright (Health Education England)
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.trainee.sync.model.PlacementSpecialty;
import uk.nhs.hee.tis.trainee.sync.model.UserDesignatedBody;
import uk.nhs.hee.tis.trainee.sync.model.UserRole;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.repository.UserRoleRepository;

@Slf4j
@Service("auth-UserRole")
public class UserRoleSyncService implements SyncService {

  private static final String USER_NAME = "userName";
  private static final String ROLE_NAME = "roleName";

  private final UserRoleRepository repository;

  private final DataRequestService dataRequestService;

  private final RequestCacheService requestCacheService;

  private final FifoMessagingService fifoMessagingService;

  private final String queueUrl;

  UserRoleSyncService(UserRoleRepository repository, DataRequestService dataRequestService,
      FifoMessagingService fifoMessagingService,
      @Value("${application.aws.sqs.user-role}") String queueUrl,
      RequestCacheService requestCacheService) {
    this.repository = repository;
    this.dataRequestService = dataRequestService;
    this.fifoMessagingService = fifoMessagingService;
    this.queueUrl = queueUrl;
    this.requestCacheService = requestCacheService;
  }

  @Override
  public void syncRecord(Record userRole) {
    if (!(userRole instanceof UserRole)) {
      String message = String.format("Invalid record type '%s'.", userRole.getClass());
      throw new IllegalArgumentException(message);
    }

    // Send incoming user role records to the HEE user role queue to be processed.
    fifoMessagingService.sendMessageToFifoQueue(queueUrl, userRole);
  }

  /**
   * Synchronize the given user role.
   *
   * @param userRole The user role to synchronize.
   */
  public void syncUserRole(UserRole userRole) {

    if (userRole.getOperation().equals(DELETE)) {
        repository.deleteById(userRole.getTisId());
    } else {
      repository.save(userRole);
    }

    requestCacheService.deleteItemFromCache(UserRole.ENTITY_NAME, userRole.getTisId());
  }

  public Optional<UserRole> findById(String id) {
    return repository.findById(id);
  }

  public Optional<UserRole> findByUserNameAndRoleName(String userName, String roleName) {
    return repository.findByUserNameAndRoleName(userName, roleName);
  }

  //TODO: would this ever be needed?
  /**
   * Make a request to retrieve a specific user role.
   *
   * @param userName The user name of the user role to be retrieved.
   * @param roleName The role name of the user role to be retrieved.
   */
  public void request(String userName, String roleName) {
    String id = String.format("%s_%s", userName, roleName);
    if (!requestCacheService.isItemInCache(UserRole.ENTITY_NAME, id)) {
      log.info("Sending request for user role [{}]", id);

      try {
        requestCacheService.addItemToCache(UserRole.ENTITY_NAME, id,
            dataRequestService.sendRequest(UserRole.ENTITY_NAME,
                Map.of(USER_NAME, userName, ROLE_NAME, roleName)));
      } catch (JsonProcessingException e) {
        log.error("Error while trying to request a user role", e);
      }
    } else {
      log.debug("Already requested user role [{}].", id);
    }
  }
}

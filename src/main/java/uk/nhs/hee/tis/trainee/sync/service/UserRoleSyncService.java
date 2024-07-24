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

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.model.UserRole;
import uk.nhs.hee.tis.trainee.sync.repository.UserRoleRepository;

/**
 * A service for managing User role synchronisation.
 */
@Slf4j
@Service("auth-UserRole")
public class UserRoleSyncService implements SyncService {

  public static final String USER_ROLE_USERNAME = "userName";
  public static final String USER_ROLE_ROLE = "roleName";

  private final UserRoleRepository repository;

  UserRoleSyncService(UserRoleRepository repository) {
    this.repository = repository;
  }

  @Override
  public void syncRecord(Record userRole) {
    if (!(userRole instanceof UserRole)) {
      String message = String.format("Invalid record type '%s'.", userRole.getClass());
      throw new IllegalArgumentException(message);
    }

    if (userRole.getOperation().equals(DELETE)) {
      repository.deleteById(userRole.getTisId());
    } else {
      repository.save((UserRole) userRole);
    }
  }

  public Optional<UserRole> findById(String id) {
    return repository.findById(id);
  }

  public Optional<UserRole> findRvOfficerRoleByUserName(String userName) {
    return repository.findRvOfficerRoleByUserName(userName);
  }
}

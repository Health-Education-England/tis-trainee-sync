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
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.model.UserDesignatedBody;
import uk.nhs.hee.tis.trainee.sync.repository.UserDesignatedBodyRepository;

/**
 * A service for managing User designated body synchronisation.
 */
@Slf4j
@Service("auth-UserDesignatedBody")
public class UserDesignatedBodySyncService implements SyncService {

  private final UserDesignatedBodyRepository repository;

  UserDesignatedBodySyncService(UserDesignatedBodyRepository repository) {
    this.repository = repository;
  }

  @Override
  public void syncRecord(Record userDesignatedBody) {
    if (!(userDesignatedBody instanceof UserDesignatedBody)) {
      String message = String.format("Invalid record type '%s'.", userDesignatedBody.getClass());
      throw new IllegalArgumentException(message);
    }

    if (userDesignatedBody.getOperation().equals(DELETE)) {
      repository.deleteById(userDesignatedBody.getTisId());
    } else {
      repository.save((UserDesignatedBody) userDesignatedBody);
    }
  }

  public Optional<UserDesignatedBody> findById(String id) {
    return repository.findById(id);
  }

  public Set<UserDesignatedBody> findByUserName(String userName) {
    return repository.findByUserName(userName);
  }

  public Set<UserDesignatedBody> findByDbc(String dbc) {
    return repository.findByDbc(dbc);
  }

  public Optional<UserDesignatedBody> findByUserNameAndDesignatedBodyCode(String userName,
      String designatedBodyCode) {
    return repository.findByUserNameAndDesignatedBodyCode(userName, designatedBodyCode);
  }
}

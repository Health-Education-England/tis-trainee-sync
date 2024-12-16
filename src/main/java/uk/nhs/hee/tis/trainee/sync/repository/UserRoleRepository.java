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

package uk.nhs.hee.tis.trainee.sync.repository;

import java.util.Optional;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import uk.nhs.hee.tis.trainee.sync.model.UserRole;

/**
 * A repository for UserRole data.
 */
@CacheConfig(cacheNames = UserRole.ENTITY_NAME)
@Repository
public interface UserRoleRepository extends MongoRepository<UserRole, String> {

  @Cacheable
  @Override
  Optional<UserRole> findById(String id);

  //TODO check if save creates duplicates
  @CachePut(key = "#entity.tisId")
  @Override
  <T extends UserRole> T save(T entity);

  @CacheEvict
  @Override
  void deleteById(String id);

  @Query("{ $and: [ {'data.userName' : ?0}, {'data.roleName' : ?1} ]}")
  Optional<UserRole> findByUserNameAndRoleName(String userName, String roleName);

  @Query("{ $and: [ {'data.userName' : ?0}, {'data.roleName' : \"RVOfficer\"} ]}")
  Optional<UserRole> findRvOfficerRoleByUserName(String userName);

}

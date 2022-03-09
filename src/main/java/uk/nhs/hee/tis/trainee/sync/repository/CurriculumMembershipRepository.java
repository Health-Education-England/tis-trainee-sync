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

package uk.nhs.hee.tis.trainee.sync.repository;

import java.util.Optional;
import java.util.Set;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import uk.nhs.hee.tis.trainee.sync.model.CurriculumMembership;

@CacheConfig(cacheNames = CurriculumMembership.ENTITY_NAME)
@Repository
public interface CurriculumMembershipRepository
    extends MongoRepository<CurriculumMembership, String> {

  @Cacheable
  @Override
  Optional<CurriculumMembership> findById(String id);

  @CachePut(key = "#entity.tisId")
  @Override
  <T extends CurriculumMembership> T save(T entity);

  @CacheEvict
  @Override
  void deleteById(String id);

  @Query("{ 'data.programmeId' : ?0}")
  Set<CurriculumMembership> findByProgrammeId(String programmeId);

  @Query("{ 'data.curriculumId' : ?0}")
  Set<CurriculumMembership> findByCurriculumId(String curriculumId);

  @Query("{ 'data.personId' : ?0}")
  Set<CurriculumMembership> findByPersonId(String personId);

  @Query("{ $and: [ { 'data.personId' : ?0}, { 'data.programmeId' : ?1 }, "
      + "{ 'data.programmeMembershipType' : ?2}, { 'data.programmeStartDate' : ?3}, "
      + "{ 'data.programmeEndDate' : ?4} ] }")
  Set<CurriculumMembership> findBySimilar(String personId,
                                         String programmeId, String programmeMembershipType,
                                         String programmeStartDate, String programmeEndDate);
}

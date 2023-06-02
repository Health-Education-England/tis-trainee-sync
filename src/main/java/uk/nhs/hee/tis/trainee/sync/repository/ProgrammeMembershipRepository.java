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

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import uk.nhs.hee.tis.trainee.sync.model.ProgrammeMembership;

@CacheConfig(cacheNames = ProgrammeMembership.ENTITY_NAME)
public interface ProgrammeMembershipRepository
    extends MongoRepository<ProgrammeMembership, UUID> {

  @Cacheable
  @Override
  Optional<ProgrammeMembership> findById(UUID uuid);

  @CachePut(key = "#entity.uuid")
  @Override
  <T extends ProgrammeMembership> T save(T entity);

  @CacheEvict
  @Override
  void deleteById(UUID uuid);

  Set<ProgrammeMembership> findByProgrammeId(Long programmeId);

  Set<ProgrammeMembership> findByPersonId(Long personId);

  @Query("{ $and: [ { 'personId' : ?0}, { 'programmeId' : ?1 }, "
      + "{ 'programmeMembershipType' : ?2}, { 'programmeStartDate' : ?3}, "
      + "{ 'programmeEndDate' : ?4} ] }")
  Set<ProgrammeMembership> findBySimilar(Long personId, Long programmeId,
      String programmeMembershipType, LocalDate programmeStartDate, LocalDate programmeEndDate);
}

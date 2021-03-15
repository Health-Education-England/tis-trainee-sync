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
import uk.nhs.hee.tis.trainee.sync.model.PlacementSpecialty;
import uk.nhs.hee.tis.trainee.sync.model.ProgrammeMembership;

@CacheConfig(cacheNames = PlacementSpecialty.ENTITY_NAME)
@Repository
public interface PlacementSpecialtyRepository extends MongoRepository<PlacementSpecialty, String> {

  @Cacheable
  @Override
  Optional<PlacementSpecialty> findById(String id);

  @CachePut(key = "#entity.tisId")
  @Override
  <T extends PlacementSpecialty> T save(T entity);

  @CacheEvict
  @Override
  void deleteById(String id);

  @Query("{ $and: [ {'data.specialtyId' : ?0}, { 'data.placementSpecialtyType' : \"PRIMARY\"} ] }")
  Set<PlacementSpecialty> findPlacementSpecialtiesPrimaryOnlyBySpecialtyId(String specialtyId);
}

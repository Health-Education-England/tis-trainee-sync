package uk.nhs.hee.tis.trainee.sync.repository;

import java.util.Optional;
import java.util.Set;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import uk.nhs.hee.tis.trainee.sync.model.PlacementSpecialty;

@Repository
public interface PlacementSpecialtyRepository extends MongoRepository<PlacementSpecialty, String> {

  @Override
  Optional<PlacementSpecialty> findById(String id);

  @Override
  <T extends PlacementSpecialty> T save(T entity);

  @Override
  void deleteById(String id);

  @Query("{ $and: [ {'data.specialtyId' : ?0}, { 'data.placementSpecialtyType' : \"PRIMARY\"} ] }")
  Set<PlacementSpecialty> findPlacementSpecialtiesPrimaryOnlyBySpecialtyId(String specialtyId);
}

package uk.nhs.hee.tis.trainee.sync.repository;

import java.util.Optional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.mongodb.repository.MongoRepository;
import uk.nhs.hee.tis.trainee.sync.model.Specialty;

public interface SpecialtyRepository extends MongoRepository<Specialty, String> {

  @Cacheable
  @Override
  Optional<Specialty> findById(String id);

  @CachePut(key = "#entity.tisId")
  @Override
  <T extends Specialty> T save(T entity);

  @CacheEvict
  @Override
  void deleteById(String id);
}

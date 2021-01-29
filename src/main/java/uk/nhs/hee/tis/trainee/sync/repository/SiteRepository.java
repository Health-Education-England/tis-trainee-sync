package uk.nhs.hee.tis.trainee.sync.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import uk.nhs.hee.tis.trainee.sync.model.Site;

@Repository
public interface SiteRepository extends MongoRepository<Site, String> {

}

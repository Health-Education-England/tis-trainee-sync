package uk.nhs.hee.tis.trainee.sync.service;

import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.model.Site;
import uk.nhs.hee.tis.trainee.sync.repository.SiteRepository;

import java.util.Optional;

import static uk.nhs.hee.tis.trainee.sync.model.Operation.DELETE;

@Service("reference-Site")
public class SiteSyncService implements SyncService {

  private final SiteRepository repository;

  SiteSyncService(SiteRepository repository) {
    this.repository = repository;
  }

  @Override
  public void syncRecord(Record record) {
    if (!(record instanceof Site)) {
      String message = String.format("Invalid record type '%s'.", record.getClass());
      throw new IllegalArgumentException(message);
    }

    if (record.getOperation().equals(DELETE)) {
      repository.deleteById(record.getTisId());
    } else {
      repository.save((Site) record);
    }
  }

  public Optional<Site> findById(String id) {
    return repository.findById(id);
  }

  public void request(String id) {
    // TODO: Implement.
    throw new UnsupportedOperationException();
  }
}

package uk.nhs.hee.tis.trainee.sync.service;

import static uk.nhs.hee.tis.trainee.sync.model.Operation.DELETE;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.lettuce.core.RedisClient;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.trainee.sync.model.Programme;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.model.Specialty;
import uk.nhs.hee.tis.trainee.sync.repository.SpecialtyRepository;

@Slf4j
@Service("tcs-Specialty")
public class SpecialtySyncService extends CacheableService implements SyncService {

  private final SpecialtyRepository repository;

  private final DataRequestService dataRequestService;

  SpecialtySyncService(SpecialtyRepository repository, DataRequestService dataRequestService,
                       RedisClient redisClient) {
    super(redisClient, Specialty.ENTITY_NAME);
    this.repository = repository;
    this.dataRequestService = dataRequestService;
  }

  @Override
  public void syncRecord(Record specialty) {
    if (!(specialty instanceof Specialty)) {
      String message = String.format("Invalid record type '%s'.", specialty.getClass());
      throw new IllegalArgumentException(message);
    }

    if (specialty.getOperation().equals(DELETE)) {
      repository.deleteById(specialty.getTisId());
    } else {
      repository.save((Specialty) specialty);
    }

    deleteItemFromCache(specialty.getTisId());
  }

  public Optional<Specialty> findById(String id) {
    return repository.findById(id);
  }

  /**
   * Make a request to retrieve a specific specialty.
   *
   * @param id The id of the specialty to be retrieved.
   */
  public void request(String id) {
    if (!isItemInCache(id)) {
      log.info("Sending request for Specialty [{}]", id);

      try {
        dataRequestService.sendRequest(Specialty.ENTITY_NAME, Map.of("id", id));
        addItemToCache(id);
      } catch (JsonProcessingException e) {
        log.error("Error while trying to request a Specialty", e);
      }
    } else {
      log.debug("Already requested Specialty [{}].", id);
    }
  }

}

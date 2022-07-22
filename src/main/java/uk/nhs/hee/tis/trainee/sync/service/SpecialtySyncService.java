package uk.nhs.hee.tis.trainee.sync.service;

import static uk.nhs.hee.tis.trainee.sync.model.Operation.DELETE;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.model.Specialty;
import uk.nhs.hee.tis.trainee.sync.repository.SpecialtyRepository;

@Slf4j
@Service("tcs-Specialty")
public class SpecialtySyncService implements SyncService {

  private final SpecialtyRepository repository;

  private final DataRequestService dataRequestService;

  private final RequestCacheService requestCacheService;

  SpecialtySyncService(SpecialtyRepository repository, DataRequestService dataRequestService,
                       RequestCacheService requestCacheService) {
    this.repository = repository;
    this.dataRequestService = dataRequestService;
    this.requestCacheService = requestCacheService;
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

    requestCacheService.deleteItemFromCache(Specialty.ENTITY_NAME, specialty.getTisId());
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
    if (!requestCacheService.isItemInCache(Specialty.ENTITY_NAME, id)) {
      log.info("Sending request for Specialty [{}]", id);

      try {
        requestCacheService.addItemToCache(Specialty.ENTITY_NAME, id,
            dataRequestService.sendRequest(Specialty.ENTITY_NAME, Map.of("id", id)));
      } catch (JsonProcessingException e) {
        log.error("Error while trying to request a Specialty", e);
      }
    } else {
      log.debug("Already requested Specialty [{}].", id);
    }
  }

}

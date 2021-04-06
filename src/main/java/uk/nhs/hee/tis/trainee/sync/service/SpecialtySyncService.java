package uk.nhs.hee.tis.trainee.sync.service;

import static uk.nhs.hee.tis.trainee.sync.model.Operation.DELETE;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

  private final Set<String> requestedIds = new HashSet<>();

  SpecialtySyncService(SpecialtyRepository repository, DataRequestService dataRequestService) {
    this.repository = repository;
    this.dataRequestService = dataRequestService;
  }

  @Override
  public void syncRecord(Record record) {
    if (!(record instanceof Specialty)) {
      String message = String.format("Invalid record type '%s'.", record.getClass());
      throw new IllegalArgumentException(message);
    }

    if (record.getOperation().equals(DELETE)) {
      repository.deleteById(record.getTisId());
    } else {
      repository.save((Specialty) record);
    }

    String id = record.getTisId();
    requestedIds.remove(id);
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
    if (!requestedIds.contains(id)) {
      log.info("Sending request for Specialty [{}]", id);

      try {
        Map<String, String> whereMap = new HashMap<>();
        whereMap.put("id", id);
        dataRequestService.sendRequest(Specialty.ENTITY_NAME, whereMap);
        requestedIds.add(id);
      } catch (JsonProcessingException e) {
        log.error("Error while trying to request a Specialty", e);
      }
    } else {
      log.debug("Already requested Specialty [{}].", id);
    }
  }

}

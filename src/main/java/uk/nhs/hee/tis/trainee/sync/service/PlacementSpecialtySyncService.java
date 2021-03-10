package uk.nhs.hee.tis.trainee.sync.service;

import static uk.nhs.hee.tis.trainee.sync.model.Operation.DELETE;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.trainee.sync.model.Placement;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.model.PlacementSpecialty;
import uk.nhs.hee.tis.trainee.sync.repository.PlacementSpecialtyRepository;

@Slf4j
@Service("tcs-PlacementSpecialty")
public class PlacementSpecialtySyncService implements SyncService {

  private final PlacementSpecialtyRepository repository;

  private final DataRequestService dataRequestService;

  private final Set<String> requestedIds = new HashSet<>();

  PlacementSpecialtySyncService(PlacementSpecialtyRepository repository, DataRequestService dataRequestService) {
    this.repository = repository;
    this.dataRequestService = dataRequestService;
  }

  @Override
  public void syncRecord(Record record) {
    if (!(record instanceof PlacementSpecialty)) {
      String message = String.format("Invalid record type '%s'.", record.getClass());
      throw new IllegalArgumentException(message);
    }

    if (record.getOperation().equals(DELETE)) {
      repository.deleteById(record.getTisId());
    } else {
      if (record.getData().get("placementSpecialtyType").equals("PRIMARY")) {
        repository.save((PlacementSpecialty) record);
      }
    }

    String id = record.getTisId();
    requestedIds.remove(id);
  }

  public Set<Placement> findPlacementsBySpecialtyId(String id) {
    return repository.findPlacementsBySpecialtyId(id);
  }

  /**
   * Make a request to retrieve a specific placementPlacementSpecialty.
   *
   * @param id The id of the placementPlacementSpecialty to be retrieved.
   */
  public void request(String id) {
    if (!requestedIds.contains(id)) {
      log.info("Sending request for PlacementSpecialty [{}]", id);

      try {
        dataRequestService.sendRequest(PlacementSpecialty.ENTITY_NAME, id);
        requestedIds.add(id);
      } catch (JsonProcessingException e) {
        log.error("Error while trying to request a PlacementSpecialty", e);
      }
    } else {
      log.debug("Already requested PlacementSpecialty [{}].", id);
    }
  }

}

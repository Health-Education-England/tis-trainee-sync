package uk.nhs.hee.tis.trainee.sync.service;

import static uk.nhs.hee.tis.trainee.sync.model.Operation.DELETE;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.trainee.sync.model.PlacementSpecialty;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.repository.PlacementSpecialtyRepository;

@Slf4j
@Service("tcs-PlacementSpecialty")
public class PlacementSpecialtySyncService implements SyncService {

  private final PlacementSpecialtyRepository repository;

  private final DataRequestService dataRequestService;

  private final Set<String> requestedIds = new HashSet<>();

  private final static String PLACEMENT_ID = "placementId";

  PlacementSpecialtySyncService(PlacementSpecialtyRepository repository,
      DataRequestService dataRequestService) {
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
      repository.deleteById(record.getData().get(PLACEMENT_ID));
    } else {
      if (Objects.equals(record.getData().get("placementSpecialtyType"), "PRIMARY")) {
        Map<String, String> placementSpecialtyData = record.getData();
        String placementId = placementSpecialtyData.get("placementId");
        record.setTisId(placementId);
        repository.save((PlacementSpecialty) record);
      }
    }

    String id = record.getTisId();
    requestedIds.remove(id);
  }

  public Optional<PlacementSpecialty> findById(String id) {
    return repository.findById(id);
  }

  public Set<PlacementSpecialty> findPrimaryPlacementSpecialtiesBySpecialtyId(String id) {
    return repository.findPlacementSpecialtiesPrimaryOnlyBySpecialtyId(id);
  }

  /**
   * Make a request to retrieve a specific placementPlacementSpecialty.
   *
   * @param id The id of the placementPlacementSpecialty to be retrieved.
   *
   *           Note: since only one placement specialty per placement can be PRIMARY,
   *           placementId is used as the primary key for this repository.
   */
  public void request(String id) {
    if (!requestedIds.contains(id)) {
      log.info("Sending request for PlacementSpecialty [{}]", id);

      try {
        Map<String, String> whereMap = new HashMap<>();
        whereMap.put("placementId", id);
        whereMap.put("placementSpecialtyType", "PRIMARY");
        dataRequestService.sendRequest(PlacementSpecialty.ENTITY_NAME, whereMap);
        requestedIds.add(id);
      } catch (JsonProcessingException e) {
        log.error("Error while trying to request a PlacementSpecialty", e);
      }
    } else {
      log.debug("Already requested PlacementSpecialty [{}].", id);
    }
  }

}

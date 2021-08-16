package uk.nhs.hee.tis.trainee.sync.service;

import static uk.nhs.hee.tis.trainee.sync.model.Operation.DELETE;

import com.fasterxml.jackson.core.JsonProcessingException;
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

  private static final String PLACEMENT_ID = "placementId";
  private final PlacementSpecialtyRepository repository;
  private final DataRequestService dataRequestService;
  private final Set<String> requestedIds = new HashSet<>();

  PlacementSpecialtySyncService(PlacementSpecialtyRepository repository,
      DataRequestService dataRequestService) {
    this.repository = repository;
    this.dataRequestService = dataRequestService;
  }

  @Override
  public void syncRecord(Record placementSpecialty) {
    if (!(placementSpecialty instanceof PlacementSpecialty)) {
      String message = String.format("Invalid record type '%s'.", placementSpecialty.getClass());
      throw new IllegalArgumentException(message);
    }

    if (placementSpecialty.getOperation().equals(DELETE)) {
      String placementId = placementSpecialty.getData().get(PLACEMENT_ID);
      Optional<PlacementSpecialty> storedPlacementSpecialty = repository.findById(placementId);
      if (storedPlacementSpecialty.isEmpty() || haveSameSpecialtyIds(placementSpecialty,
          storedPlacementSpecialty.get())) {
        repository.deleteById(placementId);
      }
    } else {
      if (Objects.equals(placementSpecialty.getData().get("placementSpecialtyType"), "PRIMARY")) {
        Map<String, String> placementSpecialtyData = placementSpecialty.getData();
        String placementId = placementSpecialtyData.get(PLACEMENT_ID);
        placementSpecialty.setTisId(placementId);
        repository.save((PlacementSpecialty) placementSpecialty);
      }
    }

    String id = placementSpecialty.getTisId();
    requestedIds.remove(id);
  }

  public Optional<PlacementSpecialty> findById(String id) {
    return repository.findById(id);
  }

  public Set<PlacementSpecialty> findPrimaryPlacementSpecialtiesBySpecialtyId(String id) {
    return repository.findPlacementSpecialtiesPrimaryOnlyBySpecialtyId(id);
  }

  /**
   * Make a request to retrieve a specific placementPlacementSpecialty. Note: since only one
   * placement specialty per placement can be PRIMARY, placementId is used as the primary key for
   * this repository.
   *
   * @param id The id of the placementPlacementSpecialty to be retrieved.
   */
  public void request(String id) {
    if (!requestedIds.contains(id)) {
      log.info("Sending request for PlacementSpecialty [{}]", id);

      try {
        dataRequestService.sendRequest(PlacementSpecialty.ENTITY_NAME,
            Map.of(PLACEMENT_ID, id, "placementSpecialtyType", "PRIMARY"));
        requestedIds.add(id);
      } catch (JsonProcessingException e) {
        log.error("Error while trying to request a PlacementSpecialty", e);
      }
    } else {
      log.debug("Already requested PlacementSpecialty [{}].", id);
    }
  }

  private boolean haveSameSpecialtyIds(Record placementSpecialty,
      PlacementSpecialty storedPlacementSpecialty) {
    return Objects.equals(placementSpecialty.getData().get("specialtyId"),
        storedPlacementSpecialty.getData().get("specialtyId"));
  }
}

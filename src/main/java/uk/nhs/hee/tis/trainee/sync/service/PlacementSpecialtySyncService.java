/*
 * The MIT License (MIT)
 *
 * Copyright 2021 Crown Copyright (Health Education England)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package uk.nhs.hee.tis.trainee.sync.service;

import static uk.nhs.hee.tis.trainee.sync.model.Operation.DELETE;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

  private final QueueMessagingTemplate messagingTemplate;
  private final String queueUrl;

  PlacementSpecialtySyncService(PlacementSpecialtyRepository repository,
      DataRequestService dataRequestService,
      QueueMessagingTemplate messagingTemplate,
      @Value("${application.aws.sqs.placement-specialty}") String queueUrl) {
    this.repository = repository;
    this.dataRequestService = dataRequestService;
    this.messagingTemplate = messagingTemplate;
    this.queueUrl = queueUrl;
  }

  @Override
  public void syncRecord(Record placementSpecialty) {
    if (!(placementSpecialty instanceof PlacementSpecialty)) {
      String message = String.format("Invalid record type '%s'.", placementSpecialty.getClass());
      throw new IllegalArgumentException(message);
    }

    // Send incoming placement specialty records to the placement specialty queue to be processed.
    messagingTemplate.convertAndSend(queueUrl, placementSpecialty);
  }

  public void syncPlacementSpecialty(PlacementSpecialty placementSpecialty) {
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
        repository.save(placementSpecialty);
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

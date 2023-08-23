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
  private static final String PLACEMENT_SPECIALTY_ID = "specialtyId";
  private static final String PLACEMENT_SPECIALTY_TYPE = "placementSpecialtyType";
  private final PlacementSpecialtyRepository repository;
  private final DataRequestService dataRequestService;
  private final RequestCacheService requestCacheService;

  private final QueueMessagingTemplate messagingTemplate;
  private final String queueUrl;

  PlacementSpecialtySyncService(PlacementSpecialtyRepository repository,
      DataRequestService dataRequestService,
      QueueMessagingTemplate messagingTemplate,
      @Value("${application.aws.sqs.placement-specialty}") String queueUrl,
                                RequestCacheService requestCacheService) {
    this.repository = repository;
    this.dataRequestService = dataRequestService;
    this.messagingTemplate = messagingTemplate;
    this.queueUrl = queueUrl;
    this.requestCacheService = requestCacheService;
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

  /**
   * Synchronize the given placement specialty.
   *
   * @param placementSpecialty The placement specialty to synchronize.
   */
  public void syncPlacementSpecialty(PlacementSpecialty placementSpecialty) {

    String placementId = placementSpecialty.getData().get(PLACEMENT_ID);
    String specialtyId = placementSpecialty.getData().get(PLACEMENT_SPECIALTY_ID);
    PlacementSpecialty storedPlacementSpecialty = repository.findByPlacementIdAndSpecialtyId(placementId, specialtyId);

    if (placementSpecialty.getOperation().equals(DELETE)) {
      if (storedPlacementSpecialty != null) {
        repository.deleteById(storedPlacementSpecialty.getTisId());
      }
    } else {
      String placementSpecialtyType = placementSpecialty.getData().get("placementSpecialtyType");
      if (Objects.equals(placementSpecialtyType, "PRIMARY")
          || Objects.equals(placementSpecialtyType, "SUB_SPECIALTY")) {
        if (storedPlacementSpecialty != null) {
          placementSpecialty.setTisId(storedPlacementSpecialty.getTisId());
        }
        repository.save(placementSpecialty);
      }
    }

    requestCacheService.deleteItemFromCache(PlacementSpecialty.ENTITY_NAME,
        placementSpecialty.getTisId());
  }

  public Optional<PlacementSpecialty> findById(String id) {
    return repository.findById(id);
  }

  public PlacementSpecialty findPlacementSpecialtyByPlacementIdAndSpecialtyType(
      String id, String placementSpecialtyType) {
    return repository.findByPlacementIdAndSpecialtyType(id, placementSpecialtyType);
  }

  public Set<PlacementSpecialty> findNonOtherPlacementSpecialtiesBySpecialtyId(String id) {
    return repository.findPlacementSpecialtiesNonOtherBySpecialtyId(id);
  }

  /**
   * Make a request to retrieve a specific placementPlacementSpecialty. Note: since only one
   * placement specialty per placement can be PRIMARY, placementId is used as the primary key for
   * this repository.
   *
   * @param id The id of the placementPlacementSpecialty to be retrieved.
   */
  public void request(String id) {
    if (!requestCacheService.isItemInCache(PlacementSpecialty.ENTITY_NAME, id)) {
      log.info("Sending request for PlacementSpecialty [{}]", id);

      try {
        requestCacheService.addItemToCache(PlacementSpecialty.ENTITY_NAME, id,
            dataRequestService.sendRequest(PlacementSpecialty.ENTITY_NAME,
                Map.of(PLACEMENT_ID, id, "placementSpecialtyType", "PRIMARY")));
      } catch (JsonProcessingException e) {
        log.error("Error while trying to request a PlacementSpecialty", e);
      }
    } else {
      log.debug("Already requested PlacementSpecialty [{}].", id);
    }
  }
}

/*
 * The MIT License (MIT)
 *
 * Copyright 2020 Crown Copyright (Health Education England)
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

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import uk.nhs.hee.tis.trainee.sync.dto.TraineeDetailsDto;
import uk.nhs.hee.tis.trainee.sync.mapper.TraineeDetailsMapper;
import uk.nhs.hee.tis.trainee.sync.model.Record;

/**
 * A service for synchronizing reference records.
 */
@Slf4j
@Service("tcs")
public class TcsSyncService implements SyncService {

  private static final String API_TEMPLATE = "/api/{apiPath}";
  private static final String API_ID_TEMPLATE = "/api/{apiPath}/{tisId}";

  private static final String TABLE_CONTACT_DETAILS = "ContactDetails";
  private static final String TABLE_GDC_DETAILS = "GdcDetails";
  private static final String TABLE_GMC_DETAILS = "GmcDetails";
  private static final String TABLE_PERSON = "Person";
  private static final String TABLE_PERSON_OWNER = "PersonOwner";
  private static final String TABLE_PERSONAL_DETAILS = "PersonalDetails";

  private static final Map<String, String> TABLE_NAME_TO_API_PATH = Map.of(
      TABLE_CONTACT_DETAILS, "contact-details",
      TABLE_GDC_DETAILS, "gdc-details",
      TABLE_GMC_DETAILS, "gmc-details",
      TABLE_PERSON, "trainee-profile",
      TABLE_PERSON_OWNER, "person-owner",
      TABLE_PERSONAL_DETAILS, "personal-info"
  );

  private static final String REQUIRED_ROLE = "DR in Training";

  private final RestTemplate restTemplate;

  private final Map<String, Function<Record, TraineeDetailsDto>> tableNameToMappingFunction;

  @Value("${service.trainee.url}")
  private String serviceUrl;

  TcsSyncService(RestTemplate restTemplate, TraineeDetailsMapper mapper) {
    this.restTemplate = restTemplate;

    tableNameToMappingFunction = Map.of(
        TABLE_CONTACT_DETAILS, mapper::toContactDetails,
        TABLE_GDC_DETAILS, mapper::toGdcDetailsDto,
        TABLE_GMC_DETAILS, mapper::toGmcDetailsDto,
        TABLE_PERSON, mapper::toTraineeSkeleton,
        TABLE_PERSON_OWNER, mapper::toPersonOwnerDto,
        TABLE_PERSONAL_DETAILS, mapper::toPersonalInfoDto
    );
  }

  @Override
  public void syncRecord(Record record) {
    Optional<String> apiPath = getApiPath(record);

    if (apiPath.isEmpty()) {
      return;
    }

    String table = record.getTable();
    TraineeDetailsDto dto = tableNameToMappingFunction.get(record.getTable()).apply(record);
    String operationType = record.getOperation();

    if (table.equals(TABLE_PERSON)) {
      // Only sync trainees with the required role.
      if (hasRequiredRoleForSkeleton(record)) {
        syncSkeleton(dto, apiPath.get(), operationType);
      } else {
        log.info("Trainee with id {} did not have the required role '{}'.", dto.getTraineeTisId(),
            REQUIRED_ROLE);
      }
    } else {
      syncDetails(dto, apiPath.get(), operationType);
    }
  }

  /**
   * Create a skeleton record for a trainee with basic details, such as their TIS ID.
   *
   * @param dto           The trainee details to create the skeleton from.
   * @param apiPath       The API path to call.
   * @param operationType The operation type of the record being synchronized.
   */
  private void syncSkeleton(TraineeDetailsDto dto, String apiPath, String operationType) {
    switch (operationType) {
      case "insert":
      case "load":
      case "update":
        restTemplate.postForObject(serviceUrl + API_TEMPLATE, dto, Object.class, apiPath,
            dto.getTraineeTisId());
        break;
      default:
        log.warn("Unhandled record operation {}.", operationType);
    }
  }

  /**
   * Synchronize the trainee details, such as contact details and personal information.
   *
   * @param dto           The trainee details to synchronize the details from.
   * @param apiPath       The API path to call.
   * @param operationType The operation type of the record being synchronized.
   */
  private void syncDetails(TraineeDetailsDto dto, String apiPath, String operationType) {
    switch (operationType) {
      case "insert":
      case "load":
      case "update":
        try {
          restTemplate.patchForObject(serviceUrl + API_ID_TEMPLATE, dto, Object.class, apiPath,
              dto.getTraineeTisId());
        } catch (HttpStatusCodeException e) {
          if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
            log.warn("Trainee not found with id {}.", dto.getTraineeTisId());
          } else {
            throw e;
          }
        }
        break;
      default:
        log.warn("Unhandled record operation {}.", operationType);
    }
  }

  /**
   * Get the API path based on the {@link Record}.
   *
   * @param record The record to get the API path for.
   * @return An optional API path, empty if a supported table is not found.
   */
  private Optional<String> getApiPath(Record record) {
    String table = record.getTable();
    String apiPath = TABLE_NAME_TO_API_PATH.get(table);

    if (apiPath == null) {
      log.warn("Unhandled record table '{}'.", table);
    }

    return Optional.ofNullable(apiPath);
  }

  /**
   * Checks whether the record has the required role for a skeleton record to be created.
   *
   * @param record The record to verify.
   * @return Whether the required role was found.
   */
  private boolean hasRequiredRoleForSkeleton(Record record) {
    String concatRoles = record.getData().getOrDefault("role", "");
    String[] roles = concatRoles.split(",");

    for (String role : roles) {
      if (role.equals(REQUIRED_ROLE)) {
        return true;
      }
    }

    return false;
  }
}

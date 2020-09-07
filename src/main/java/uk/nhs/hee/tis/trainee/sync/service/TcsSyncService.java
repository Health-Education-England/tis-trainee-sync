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
import org.springframework.stereotype.Service;
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

  private static final String API_ID_TEMPLATE = "/api/{apiPath}/{tisId}";

  private static final Map<String, String> TABLE_NAME_TO_API_PATH = Map.of(
      "ContactDetails", "contact-details",
      "PersonalDetails", "personal-info"
  );

  private final RestTemplate restTemplate;

  private final TraineeDetailsMapper mapper;

  private final Map<String, Function<Record, TraineeDetailsDto>> tableNameToMappingFunction;

  @Value("${service.trainee.url}")
  private String serviceUrl;

  TcsSyncService(RestTemplate restTemplate, TraineeDetailsMapper mapper) {
    this.restTemplate = restTemplate;
    this.mapper = mapper;

    tableNameToMappingFunction = Map.of(
        "ContactDetails", mapper::toContactDetails,
        "PersonalDetails", mapper::toPersonalInfoDto
    );
  }

  @Override
  public void syncRecord(Record record) {
    Optional<String> apiPath = getApiPath(record);

    if (apiPath.isEmpty()) {
      return;
    }

    String operationType = record.getOperation();

    switch (operationType) {
      case "insert":
      case "load":
      case "update":
        TraineeDetailsDto dto = tableNameToMappingFunction.get(record.getTable()).apply(record);
        restTemplate.patchForObject(serviceUrl + API_ID_TEMPLATE, dto, Object.class, apiPath.get(),
            dto.getTisId());
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
}

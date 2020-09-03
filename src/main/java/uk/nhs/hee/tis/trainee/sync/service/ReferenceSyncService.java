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
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.nhs.hee.tis.trainee.sync.dto.ReferenceDto;
import uk.nhs.hee.tis.trainee.sync.mapper.ReferenceMapper;
import uk.nhs.hee.tis.trainee.sync.model.Record;

/**
 * A service for synchronizing reference records.
 */
@Slf4j
@Service("reference")
public class ReferenceSyncService implements SyncService {

  private static final String API_TEMPLATE = "/api/{referenceType}";
  private static final String API_ID_TEMPLATE = "/api/{referenceType}/{tisId}";

  private static final Map<String, String> TABLE_NAME_TO_REFERENCE_TYPE = Map.of(
      "College", "college",
      "Gender", "gender",
      "Grade", "grade",
      "PermitToWork", "immigration-status",
      "LocalOffice", "local-office"
  );

  private final RestTemplate restTemplate;

  private final ReferenceMapper mapper;

  @Value("${service.reference.url}")
  private String serviceUrl;

  ReferenceSyncService(RestTemplate restTemplate, ReferenceMapper mapper) {
    this.restTemplate = restTemplate;
    this.mapper = mapper;
  }

  @Override
  public void syncRecord(Record record) {
    Optional<String> referenceType = getReferenceType(record);

    if (referenceType.isEmpty()) {
      return;
    }

    // Inactive records should be deleted.
    boolean inactive = Objects.equals(record.getData().get("status"), "INACTIVE");
    String operationType = inactive ? "delete" : record.getOperation();

    ReferenceDto dto = mapper.toReference(record);

    switch (operationType) {
      case "insert":
      case "load":
        restTemplate.postForLocation(serviceUrl + API_TEMPLATE, dto, referenceType.get());
        break;
      case "update":
        restTemplate.put(serviceUrl + API_TEMPLATE, dto, referenceType.get());
        break;
      case "delete":
        restTemplate.delete(serviceUrl + API_ID_TEMPLATE, referenceType.get(), dto.getTisId());
        break;
      default:
        log.warn("Unhandled record operation '{}'.", operationType);
    }
  }

  /**
   * Get the reference type based on the {@link Record}.
   *
   * @param record The record to get the reference type of.
   * @return An optional reference type, empty if a supported table is not found.
   */
  private Optional<String> getReferenceType(Record record) {
    String table = record.getTable();
    String referenceType = TABLE_NAME_TO_REFERENCE_TYPE.get(table);

    if (referenceType == null) {
      log.warn("Unhandled record table '{}'.", table);
    }

    return Optional.ofNullable(referenceType);
  }
}

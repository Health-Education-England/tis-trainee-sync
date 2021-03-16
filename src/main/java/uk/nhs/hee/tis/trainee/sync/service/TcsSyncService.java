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

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.nhs.hee.tis.trainee.sync.dto.TraineeDetailsDto;
import uk.nhs.hee.tis.trainee.sync.mapper.TraineeDetailsMapper;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.Person;
import uk.nhs.hee.tis.trainee.sync.model.Record;

/**
 * A service for synchronizing reference records.
 */
@Slf4j
@Service("tcs")
public class TcsSyncService implements SyncService {

  private static final String API_ID_TEMPLATE = "/api/{apiPath}/{tisId}";

  private static final String TABLE_CONTACT_DETAILS = "ContactDetails";
  private static final String TABLE_GDC_DETAILS = "GdcDetails";
  private static final String TABLE_GMC_DETAILS = "GmcDetails";
  private static final String TABLE_PERSON = "Person";
  private static final String TABLE_PERSON_OWNER = "PersonOwner";
  private static final String TABLE_PERSONAL_DETAILS = "PersonalDetails";
  private static final String TABLE_QUALIFICATION = "Qualification";
  private static final String TABLE_PLACEMENT = "Placement";
  private static final String TABLE_PROGRAMME_MEMBERSHIP = "ProgrammeMembership";
  private static final String TABLE_CURRICULUM = "Curriculum";
  private static final String TABLE_PLACEMENT_SPECIALTY = "PlacementSpecialty";
  private static final String TABLE_SPECIALTY = "Specialty";

  private static final Map<String, String> TABLE_NAME_TO_API_PATH = Map.ofEntries(
      Map.entry(TABLE_CONTACT_DETAILS, "contact-details"),
      Map.entry(TABLE_GDC_DETAILS, "gdc-details"),
      Map.entry(TABLE_GMC_DETAILS, "gmc-details"),
      Map.entry(TABLE_PERSON, "basic-details"),
      Map.entry(TABLE_PERSON_OWNER, "person-owner"),
      Map.entry(TABLE_PERSONAL_DETAILS, "personal-info"),
      Map.entry(TABLE_QUALIFICATION, "qualification"),
      Map.entry(TABLE_PLACEMENT, "placement"),
      Map.entry(TABLE_PROGRAMME_MEMBERSHIP, "programme-membership"),
      Map.entry(TABLE_CURRICULUM, "curriculum"),
      Map.entry(TABLE_PLACEMENT_SPECIALTY, "placement-specialty"),
      Map.entry(TABLE_SPECIALTY, "specialty")
  );

  private static final String REQUIRED_ROLE = "DR in Training";

  private final RestTemplate restTemplate;

  private final PersonService personService;

  private final Map<String, Function<Record, TraineeDetailsDto>> tableNameToMappingFunction;

  @Value("${service.trainee.url}")
  private String serviceUrl;

  TcsSyncService(RestTemplate restTemplate,
      TraineeDetailsMapper mapper, PersonService personService) {
    this.restTemplate = restTemplate;
    this.personService = personService;

    tableNameToMappingFunction = Map.ofEntries(
        Map.entry(TABLE_CONTACT_DETAILS, mapper::toContactDetails),
        Map.entry(TABLE_GDC_DETAILS, mapper::toGdcDetailsDto),
        Map.entry(TABLE_GMC_DETAILS, mapper::toGmcDetailsDto),
        Map.entry(TABLE_PERSON, mapper::toBasicDetailsDto),
        Map.entry(TABLE_PERSON_OWNER, mapper::toPersonOwnerDto),
        Map.entry(TABLE_PERSONAL_DETAILS, mapper::toPersonalInfoDto),
        Map.entry(TABLE_QUALIFICATION, mapper::toQualificationDto),
        Map.entry(TABLE_PLACEMENT, mapper::toPlacementDto),
        Map.entry(TABLE_PROGRAMME_MEMBERSHIP, mapper::toProgrammeMembershipDto),
        Map.entry(TABLE_CURRICULUM, mapper::toCurriculumDto),
        Map.entry(TABLE_PLACEMENT_SPECIALTY, mapper::toPlacementSpecialtyDto),
        Map.entry(TABLE_SPECIALTY, mapper::toSpecialtyDto)
    );
  }

  @Override
  public void syncRecord(Record record) {
    Optional<String> apiPath = getApiPath(record);

    if (apiPath.isEmpty()) {
      return;
    }

    TraineeDetailsDto dto = tableNameToMappingFunction.get(record.getTable()).apply(record);

    boolean doSync;

    if (record instanceof Person && !findById(dto.getTraineeTisId())) {
      if (hasRequiredRoleForProfileCreation(record)) {
        personService.save((Person) record);
        doSync = true;
      } else {
        log.info("Trainee with id{} did not have the required role '{}'.", dto.getTraineeTisId(),
            REQUIRED_ROLE);
        return;
      }
    } else {
      doSync = findById(dto.getTraineeTisId());
    }

    if (doSync) {
      Operation operationType = record.getOperation();
      syncDetails(dto, apiPath.get(), operationType);
    }
  }

  public boolean findById(String tisId) {
    return personService.findById(tisId).isPresent();
  }

  /**
   * Synchronize the trainee details, such as contact details and personal information.
   *
   * @param dto           The trainee details to synchronize the details from.
   * @param apiPath       The API path to call.
   * @param operationType The operation type of the record being synchronized.
   */
  private void syncDetails(TraineeDetailsDto dto, String apiPath, Operation operationType) {
    switch (operationType) {
      case INSERT:
      case LOAD:
      case UPDATE:
        restTemplate.patchForObject(serviceUrl + API_ID_TEMPLATE, dto, Object.class, apiPath,
            dto.getTraineeTisId());
        break;
      case DELETE:
        if (apiPath.equals(TABLE_NAME_TO_API_PATH.get(TABLE_PROGRAMME_MEMBERSHIP))) {
          restTemplate.delete(serviceUrl + API_ID_TEMPLATE, apiPath, dto.getTraineeTisId());
        } else if (apiPath.equals(TABLE_NAME_TO_API_PATH.get(TABLE_PLACEMENT))) {
          restTemplate.delete(serviceUrl + API_ID_TEMPLATE + "/{placementTisId}",
              apiPath, dto.getTraineeTisId(), dto.getTisId());
        } else if (apiPath.equals(TABLE_NAME_TO_API_PATH.get(TABLE_PERSON))) {
          personService.deleteById(dto.getTraineeTisId());
        } else {
          log.warn("Unhandled record operation {}.", operationType);
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
   * Checks whether the Person record has the required role for a profile record to be created.
   *
   * @param record The record to verify.
   * @return Whether the required role was found.
   */
  public boolean hasRequiredRoleForProfileCreation(Record record) {
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

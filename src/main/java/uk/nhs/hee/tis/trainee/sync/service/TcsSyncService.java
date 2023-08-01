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

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.AmazonSNSException;
import com.amazonaws.services.sns.model.PublishRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException.NotFound;
import org.springframework.web.client.RestTemplate;
import uk.nhs.hee.tis.trainee.sync.config.EventNotificationProperties;
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
  private static final String API_SUB_ID_TEMPLATE = "/api/{apiPath}/{tisId}/{subId}";
  private static final String API_DELETE_PROFILE_TEMPLATE = "/api/trainee-profile/{tisId}";

  private static final String TABLE_CONTACT_DETAILS = "ContactDetails";
  private static final String TABLE_GDC_DETAILS = "GdcDetails";
  private static final String TABLE_GMC_DETAILS = "GmcDetails";
  private static final String TABLE_PERSON = "Person";
  private static final String TABLE_PERSON_OWNER = "PersonOwner";
  private static final String TABLE_PERSONAL_DETAILS = "PersonalDetails";
  private static final String TABLE_QUALIFICATION = "Qualification";
  private static final String TABLE_PLACEMENT = "Placement";
  private static final String TABLE_PROGRAMME_MEMBERSHIP = "ProgrammeMembership";
  private static final String TABLE_CURRICULUM_MEMBERSHIP = "CurriculumMembership";
  private static final String TABLE_CURRICULUM = "Curriculum";

  // Some API endpoints have been replaced with message queues.
  private static final String DISABLED = "DISABLED";
  private static final Map<String, String> TABLE_NAME_TO_API_PATH = Map.ofEntries(
      Map.entry(TABLE_CONTACT_DETAILS, DISABLED),
      Map.entry(TABLE_GDC_DETAILS, DISABLED),
      Map.entry(TABLE_GMC_DETAILS, DISABLED),
      Map.entry(TABLE_PERSON, "basic-details"),
      Map.entry(TABLE_PERSON_OWNER, DISABLED),
      Map.entry(TABLE_PERSONAL_DETAILS, DISABLED),
      Map.entry(TABLE_QUALIFICATION, "qualification"),
      Map.entry(TABLE_PLACEMENT, "placement"),
      Map.entry(TABLE_PROGRAMME_MEMBERSHIP, "programme-membership"),
      //use same trainee-details API endpoint
      Map.entry(TABLE_CURRICULUM, "curriculum")
  );

  private static final String REQUIRED_ROLE = "DR in Training";
  private static final String[] REQUIRED_NOT_ROLES = {"Placeholder", "Dummy Record"};

  private final RestTemplate restTemplate;

  private final PersonService personService;

  private final Map<String, Function<Record, TraineeDetailsDto>> tableNameToMappingFunction;

  private final EventNotificationProperties eventNotificationProperties;

  private final AmazonSNS snsClient;
  private final ObjectMapper objectMapper;

  @Value("${service.trainee.url}")
  private String serviceUrl;

  TcsSyncService(RestTemplate restTemplate,
      TraineeDetailsMapper mapper,
      PersonService personService,
      EventNotificationProperties eventNotificationProperties,
      AmazonSNS snsClient,
      ObjectMapper objectMapper) {
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
        Map.entry(TABLE_PROGRAMME_MEMBERSHIP, mapper::toAggregateProgrammeMembershipDto),
        Map.entry(TABLE_CURRICULUM_MEMBERSHIP, mapper::toProgrammeMembershipDto), //use same DTO
        Map.entry(TABLE_CURRICULUM, mapper::toCurriculumDto)
    );
    this.eventNotificationProperties = eventNotificationProperties;
    this.snsClient = snsClient;
    this.objectMapper = objectMapper;
  }

  @Override
  public void syncRecord(Record recrd) {
    Optional<String> apiPath = getApiPath(recrd);

    if (apiPath.isEmpty()) {
      return;
    }

    TraineeDetailsDto dto = tableNameToMappingFunction.get(recrd.getTable()).apply(recrd);

    boolean doSync;

    if (recrd instanceof Person && !findById(dto.getTraineeTisId())) {
      if (hasRequiredRoleForProfileCreation(recrd) && !hasNoProfileIfRole(recrd)) {
        personService.save((Person) recrd);
        doSync = true;
      } else {
        log.info("Trainee with id{} did not have the required role set '{}' and not '{}'.",
            dto.getTraineeTisId(), REQUIRED_ROLE, Arrays.toString(REQUIRED_NOT_ROLES));
        return;
      }
    } else {
      doSync = findById(dto.getTraineeTisId());
      if (!doSync) {
        log.info("Trainee with id {} was not found, so {} table data will not be sync'd",
            dto.getTraineeTisId(), recrd.getTable());
      }
    }

    if (doSync) {
      publishDetailsChangeEvent(recrd);
      Operation operationType = recrd.getOperation();
      syncDetails(dto, apiPath.get(), operationType);
    }
  }

  /**
   * Publish record change messages to SNS. A change could be an update or delete.
   *
   * @param recrd The change record.
   */
  public void publishDetailsChangeEvent(Record recrd) {
    PublishRequest request = null;
    String snsTopic = tableToSnsTopic(recrd.getTable(), recrd.getOperation());

    if (snsTopic != null) {
      // record change should be broadcast
      Map<String, Object> treeValues = switch (recrd.getOperation()) {
        case DELETE -> Map.of(
            "tisId", recrd.getTisId(),
            "record", new Record()
        );
        case INSERT, LOAD, UPDATE -> Map.of(
            "tisId", recrd.getTisId(),
            "record", recrd
        );
        default -> null; //should never happen
      };

      if (treeValues != null) {
        JsonNode eventJson = objectMapper.valueToTree(treeValues);
        request = new PublishRequest()
            .withMessage(eventJson.toString())
            .withTopicArn(snsTopic);

        if (snsTopic.endsWith(".fifo")) {
          // Create a message group to ensure FIFO per unique object.
          String messageGroup = String.format("%s_%s_%s", recrd.getSchema(), recrd.getTable(),
              recrd.getTisId());
          request.setMessageGroupId(messageGroup);
        }
      }
    }

    if (request != null) {
      try {
        snsClient.publish(request);
        log.info("Trainee details change event sent to SNS.");
      } catch (AmazonSNSException e) {
        String message = String.format("Failed to send to SNS topic '%s'", snsTopic);
        log.error(message, e);
      }
    }
  }

  /**
   * Obtain the SNS topic that is used to broadcast changes to the given table.
   *
   * @param table     The table for the record in question.
   * @param operation The operation for the record.
   * @return The SNS topic ARN, or null if the table changes are not broadcast.
   */
  private String tableToSnsTopic(String table, Operation operation) {
    return switch (operation) {
      case DELETE -> switch (table) {
        // Personal Details deletes are treated as an empty update.
        case TABLE_CONTACT_DETAILS -> eventNotificationProperties.updateContactDetails();
        case TABLE_GDC_DETAILS -> eventNotificationProperties.updateGdcDetails();
        case TABLE_GMC_DETAILS -> eventNotificationProperties.updateGmcDetails();
        case TABLE_PERSONAL_DETAILS -> eventNotificationProperties.updatePersonalInfo();
        case TABLE_PERSON_OWNER -> eventNotificationProperties.updatePersonOwner();
        case TABLE_PLACEMENT -> eventNotificationProperties.deletePlacementEvent();
        case TABLE_PROGRAMME_MEMBERSHIP, TABLE_CURRICULUM_MEMBERSHIP ->
            eventNotificationProperties.deleteProgrammeMembershipEvent();
        default -> null;
      };
      case INSERT, LOAD, UPDATE -> switch (table) {
        case TABLE_CONTACT_DETAILS -> eventNotificationProperties.updateContactDetails();
        case TABLE_GDC_DETAILS -> eventNotificationProperties.updateGdcDetails();
        case TABLE_GMC_DETAILS -> eventNotificationProperties.updateGmcDetails();
        case TABLE_PERSONAL_DETAILS -> eventNotificationProperties.updatePersonalInfo();
        case TABLE_PERSON_OWNER -> eventNotificationProperties.updatePersonOwner();
        case TABLE_PLACEMENT -> eventNotificationProperties.updatePlacementEvent();
        case TABLE_PROGRAMME_MEMBERSHIP, TABLE_CURRICULUM_MEMBERSHIP ->
            eventNotificationProperties.updateProgrammeMembershipEvent();
        default -> null;
      };
      default -> null; //other operations are ignored
    };
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
    if (apiPath.equals(DISABLED)) {
      log.debug("API call skipped, as message already dispatched.");
      return;
    }

    switch (operationType) {
      case INSERT, LOAD, UPDATE ->
          restTemplate.patchForObject(serviceUrl + API_ID_TEMPLATE, dto, Object.class, apiPath,
              dto.getTraineeTisId());
      case DELETE -> {
        deleteDetails(dto, apiPath);
      }
      default -> log.warn("Unhandled record operation {}.", operationType);
    }
  }

  private void deleteDetails(TraineeDetailsDto dto, String apiPath) {
    try {
      if (apiPath.equals(TABLE_NAME_TO_API_PATH.get(TABLE_PROGRAMME_MEMBERSHIP))) {
        restTemplate.delete(serviceUrl + API_SUB_ID_TEMPLATE, apiPath, dto.getTraineeTisId(),
            dto.getTisId());
      } else if (apiPath.equals(TABLE_NAME_TO_API_PATH.get(TABLE_PLACEMENT)) || apiPath.equals(
          TABLE_NAME_TO_API_PATH.get(TABLE_QUALIFICATION))) {
        restTemplate.delete(serviceUrl + API_SUB_ID_TEMPLATE, apiPath, dto.getTraineeTisId(),
            dto.getTisId());
      } else if (apiPath.equals(TABLE_NAME_TO_API_PATH.get(TABLE_PERSON))) {
        restTemplate.delete(serviceUrl + API_DELETE_PROFILE_TEMPLATE, dto.getTraineeTisId());
        personService.deleteById(dto.getTraineeTisId());
      } else if (apiPath.equals(TABLE_NAME_TO_API_PATH.get(TABLE_QUALIFICATION))) {
        log.warn("Unhandled Qualification operation {}.", Operation.DELETE);
      } else {
        TraineeDetailsDto emptyDto = new TraineeDetailsDto();
        restTemplate.patchForObject(serviceUrl + API_ID_TEMPLATE, emptyDto, Object.class, apiPath,
            dto.getTraineeTisId());
      }
    } catch (NotFound e) {
      log.info("The object to be deleted was not found, it may have already been deleted.", e);
    }
  }

  /**
   * Get the API path based on the {@link Record}.
   *
   * @param recrd The record to get the API path for.
   * @return An optional API path, empty if a supported table is not found.
   */
  private Optional<String> getApiPath(Record recrd) {
    String table = recrd.getTable();
    String apiPath = TABLE_NAME_TO_API_PATH.get(table);

    if (apiPath == null) {
      log.warn("Unhandled record table '{}'.", table);
    }

    return Optional.ofNullable(apiPath);
  }

  /**
   * Checks whether the Person record has the required role for a profile record to be created.
   *
   * @param recrd The record to verify.
   * @return Whether the required role was found.
   */
  private boolean hasRequiredRoleForProfileCreation(Record recrd) {
    String concatRoles = recrd.getData().getOrDefault("role", "");
    String[] roles = concatRoles.split(",");

    for (String role : roles) {
      if (role.equalsIgnoreCase(REQUIRED_ROLE)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks whether the Person record has a role that means a profile should not be created.
   *
   * @param recrd The record to verify.
   * @return Whether any of the excluded roles were found.
   */
  private boolean hasNoProfileIfRole(Record recrd) {
    String concatRoles = recrd.getData().getOrDefault("role", "");
    String[] roles = concatRoles.split(",");

    for (String role : roles) {
      if (Arrays.stream(REQUIRED_NOT_ROLES).anyMatch(role::equalsIgnoreCase)) {
        return true;
      }
    }
    return false;
  }
}

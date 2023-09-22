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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.nhs.hee.tis.trainee.sync.model.Operation.DELETE;
import static uk.nhs.hee.tis.trainee.sync.model.Operation.INSERT;
import static uk.nhs.hee.tis.trainee.sync.model.Operation.LOAD;
import static uk.nhs.hee.tis.trainee.sync.model.Operation.UPDATE;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.AmazonSNSException;
import com.amazonaws.services.sns.model.PublishRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.assertj.core.util.Strings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpClientErrorException.NotFound;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.nhs.hee.tis.trainee.sync.config.EventNotificationProperties;
import uk.nhs.hee.tis.trainee.sync.dto.TraineeDetailsDto;
import uk.nhs.hee.tis.trainee.sync.mapper.TraineeDetailsMapper;
import uk.nhs.hee.tis.trainee.sync.mapper.TraineeDetailsMapperImpl;
import uk.nhs.hee.tis.trainee.sync.mapper.util.TraineeDetailsUtil;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.Person;
import uk.nhs.hee.tis.trainee.sync.model.Record;

class TcsSyncServiceTest {

  private static final String REQUIRED_ROLE = "DR in Training";
  private static final String REQUIRED_NOT_ROLE_DUMMY = "Dummy Record";
  private static final String REQUIRED_NOT_ROLE_PLACEHOLDER = "Placeholder";

  private static final String UPDATE_CONTACT_DETAILS_EVENT_ARN = "update-contact-details-arn";
  private static final String UPDATE_CONDITIONS_OF_JOINING_EVENT_ARN = "update-conditions-of-joining-arn";
  private static final String UPDATE_GDC_DETAILS_EVENT_ARN = "update-gdc-details-arn";
  private static final String UPDATE_GMC_DETAILS_EVENT_ARN = "update-gmc-details-arn";
  private static final String UPDATE_PERSON_EVENT_ARN = "update-person-arn";
  private static final String UPDATE_PERSON_OWNER_EVENT_ARN = "update-person-owner-arn";
  private static final String UPDATE_PERSONAL_INFO_EVENT_ARN = "update-personal-info-arn";
  private static final String DELETE_PLACEMENT_EVENT_ARN = "delete-placement-arn";
  private static final String DELETE_PROGRAMME_MEMBERSHIP_EVENT_ARN = "delete-programme-arn";
  private static final String UPDATE_PLACEMENT_EVENT_ARN = "update-placement-arn";
  private static final String UPDATE_PROGRAMME_MEMBERSHIP_EVENT_ARN = "update-programme-arn";
  private static final String FIFO = ".fifo";

  private static final String TABLE_CONDITIONS_OF_JOINING = "ConditionsOfJoining";
  private static final String TABLE_CONTACT_DETAILS = "ContactDetails";
  private static final String TABLE_GDC_DETAILS = "GdcDetails";
  private static final String TABLE_GMC_DETAILS = "GmcDetails";
  private static final String TABLE_PERSON = "Person";
  private static final String TABLE_PERSON_OWNER = "PersonOwner";
  private static final String TABLE_PERSONAL_DETAILS = "PersonalDetails";
  private static final String TABLE_PLACEMENT = "Placement";
  private static final String TABLE_PROGRAMME_MEMBERSHIP = "ProgrammeMembership";
  private static final String TABLE_CURRICULUM_MEMBERSHIP = "CurriculumMembership";

  private static final Map<String, String> TABLE_NAME_TO_DELETE_EVENT_ARN = Map.ofEntries(
      Map.entry(TABLE_PLACEMENT, DELETE_PLACEMENT_EVENT_ARN),
      Map.entry(TABLE_PROGRAMME_MEMBERSHIP, DELETE_PROGRAMME_MEMBERSHIP_EVENT_ARN),
      Map.entry(TABLE_CURRICULUM_MEMBERSHIP, DELETE_PROGRAMME_MEMBERSHIP_EVENT_ARN)
  );
  private static final Map<String, String> TABLE_NAME_TO_UPDATE_EVENT_ARN = Map.ofEntries(
      Map.entry(TABLE_CONDITIONS_OF_JOINING, UPDATE_CONDITIONS_OF_JOINING_EVENT_ARN),
      Map.entry(TABLE_CONTACT_DETAILS, UPDATE_CONTACT_DETAILS_EVENT_ARN),
      Map.entry(TABLE_GDC_DETAILS, UPDATE_GDC_DETAILS_EVENT_ARN),
      Map.entry(TABLE_GMC_DETAILS, UPDATE_GMC_DETAILS_EVENT_ARN),
      Map.entry(TABLE_PERSON, UPDATE_PERSON_EVENT_ARN),
      Map.entry(TABLE_PERSON_OWNER, UPDATE_PERSON_OWNER_EVENT_ARN),
      Map.entry(TABLE_PERSONAL_DETAILS, UPDATE_PERSONAL_INFO_EVENT_ARN),
      Map.entry(TABLE_PLACEMENT, UPDATE_PLACEMENT_EVENT_ARN),
      Map.entry(TABLE_PROGRAMME_MEMBERSHIP, UPDATE_PROGRAMME_MEMBERSHIP_EVENT_ARN),
      Map.entry(TABLE_CURRICULUM_MEMBERSHIP, UPDATE_PROGRAMME_MEMBERSHIP_EVENT_ARN)
  );

  private TcsSyncService service;

  private RestTemplate restTemplate;

  private PersonService personService;

  private AmazonSNS snsService;

  private TraineeDetailsMapper mapper;

  private ObjectMapper objectMapper;

  private Map<String, String> data;

  private Record recrd;

  @BeforeEach
  void setUp() {
    mapper = new TraineeDetailsMapperImpl();
    Field field = ReflectionUtils.findField(TraineeDetailsMapperImpl.class, "traineeDetailsUtil");
    assert field != null;
    field.setAccessible(true);
    ReflectionUtils.setField(field, mapper, new TraineeDetailsUtil());

    objectMapper = new ObjectMapper();

    restTemplate = mock(RestTemplate.class);
    personService = mock(PersonService.class);
    snsService = mock(AmazonSNS.class);
    ObjectMapper objectMapper = new ObjectMapper();
    EventNotificationProperties eventNotificationProperties
        = new EventNotificationProperties(DELETE_PLACEMENT_EVENT_ARN,
        DELETE_PROGRAMME_MEMBERSHIP_EVENT_ARN, UPDATE_CONDITIONS_OF_JOINING_EVENT_ARN,
        UPDATE_CONTACT_DETAILS_EVENT_ARN, UPDATE_GDC_DETAILS_EVENT_ARN,
        UPDATE_GMC_DETAILS_EVENT_ARN, UPDATE_PERSON_EVENT_ARN,
        UPDATE_PERSON_OWNER_EVENT_ARN, UPDATE_PERSONAL_INFO_EVENT_ARN, UPDATE_PLACEMENT_EVENT_ARN,
        UPDATE_PROGRAMME_MEMBERSHIP_EVENT_ARN);
    service = new TcsSyncService(restTemplate, mapper, personService, eventNotificationProperties,
        snsService, objectMapper);

    data = new HashMap<>();
    data.put("id", "idValue");
    data.put("title", "titleValue");
    data.put("forenames", "forenamesValue");
    data.put("knownAs", "knownAsValue");
    data.put("surname", "surnameValue");
    data.put("maidenName", "maidenNameValue");
    data.put("telephoneNumber", "telephoneNumberValue");
    data.put("mobileNumber", "mobileNumberValue");
    data.put("email", "emailValue");
    data.put("address1", "address1Value");
    data.put("address2", "address2Value");
    data.put("address3", "address3Value");
    data.put("address4", "address4Value");
    data.put("postCode", "postCodeValue");
    data.put("publicHealthNumber", "publicHealthNumberValue");
    data.put("personId", "personIdValue");
    data.put("role", REQUIRED_ROLE);

    recrd = new Record();
    recrd.setTisId("idValue");
  }

  @Test
  void shouldNotSyncRecordWhenTableNotSupported() {
    recrd.setTable("unsupportedTable");

    service.syncRecord(recrd);

    verifyNoInteractions(restTemplate);
  }

  @Test
  void shouldSaveRecordIntoPersonRepositoryIfRecordIsPersonAndNotInPersonRepository() {
    Person person = new Person();
    person.setTisId("idValue");
    person.setTable("Person");
    person.setOperation(INSERT);
    data.put("role", REQUIRED_ROLE);
    person.setData(data);

    service.syncRecord(person);

    TraineeDetailsDto expectedDto = new TraineeDetailsDto();
    expectedDto.setTraineeTisId("idValue");
    expectedDto.setPublicHealthNumber("publicHealthNumberValue");

    verify(personService).save(person);
    verify(restTemplate)
        .patchForObject(anyString(), eq(expectedDto), eq(Object.class), eq("basic-details"),
            eq("idValue"));
    verifyNoMoreInteractions(restTemplate);
  }

  @ParameterizedTest(name = "Should not patch basic details when role is {0}")
  @ValueSource(strings = {"nonRequiredRole", "prefix-" + REQUIRED_ROLE, REQUIRED_ROLE + "-suffix",
      "prefix-" + REQUIRED_ROLE + "-suffix"})
  void shouldNotPatchBasicDetailsWhenRequiredRoleNotFound(String role) {
    Person person = new Person();
    person.setTisId("idValue");
    person.setTable("Person");
    person.setOperation(INSERT);
    person.setData(Collections.singletonMap("role", role));

    service.syncRecord(person);

    verifyNoInteractions(restTemplate);
    verify(personService, times(1)).findById(anyString());
  }

  @ParameterizedTest(name = "Should not patch basic details when role is {0}")
  @ValueSource(strings = {REQUIRED_NOT_ROLE_DUMMY, REQUIRED_NOT_ROLE_PLACEHOLDER})
  void shouldNotPatchBasicDetailsWhenRequiredNotRoleFound(String role) {
    Person person = new Person();
    person.setTisId("idValue");
    person.setTable("Person");
    person.setOperation(INSERT);
    person.setData(Collections.singletonMap("role", role));

    service.syncRecord(person);

    verifyNoInteractions(restTemplate);
    verify(personService, times(1)).findById(anyString());
  }

  @ParameterizedTest(name = "Should not patch basic details when role is {0}")
  @ValueSource(strings = {REQUIRED_NOT_ROLE_DUMMY, REQUIRED_NOT_ROLE_PLACEHOLDER})
  void shouldNotPatchBasicDetailsWhenRequiredNotRoleFoundWithRequiredRole(String notRole) {
    Person person = new Person();
    person.setTisId("idValue");
    person.setTable("Person");
    person.setOperation(INSERT);
    person.setData(Collections.singletonMap("role",
        Strings.concat(REQUIRED_ROLE, ",", notRole)));

    service.syncRecord(person);

    verifyNoInteractions(restTemplate);
    verify(personService, times(1)).findById(anyString());
  }

  @ParameterizedTest(
      name = "Should patch basic details when role is {0}, operation is load and table is Person")
  @ValueSource(strings = {"Dr in Training", "roleBefore," + REQUIRED_ROLE, REQUIRED_ROLE,
      REQUIRED_ROLE + ",roleAfter", "roleBefore," + REQUIRED_ROLE + ",roleAfter"})
  void shouldPatchBasicDetailsWhenRequiredRoleFound(String role) {
    Person person = new Person();
    person.setTisId("idValue");
    person.setTable("Person");
    person.setOperation(INSERT);
    data.put("role", role);
    person.setData(data);

    service.syncRecord(person);

    TraineeDetailsDto expectedDto = new TraineeDetailsDto();
    expectedDto.setTraineeTisId("idValue");
    expectedDto.setPublicHealthNumber("publicHealthNumberValue");

    verify(restTemplate)
        .patchForObject(anyString(), eq(expectedDto), eq(Object.class), eq("basic-details"),
            eq("idValue"));
    verifyNoMoreInteractions(restTemplate);
  }

  @ParameterizedTest(name =
      "Should patch basic details when operation is {0}, role is valid and table is Person")
  @EnumSource(value = Operation.class, names = {"LOAD", "INSERT", "UPDATE"})
  void shouldPatchBasicDetailsWhenValidOperations(Operation operation) {
    Person person = new Person();
    person.setTisId("idValue");
    person.setTable("Person");
    person.setOperation(operation);
    data.put("role", REQUIRED_ROLE);
    person.setData(data);

    when(personService.findById("idValue")).thenReturn(Optional.of(new Person()));

    service.syncRecord(person);

    TraineeDetailsDto expectedDto = new TraineeDetailsDto();
    expectedDto.setTraineeTisId("idValue");
    expectedDto.setPublicHealthNumber("publicHealthNumberValue");

    verify(restTemplate)
        .patchForObject(anyString(), eq(expectedDto), eq(Object.class), eq("basic-details"),
            eq("idValue"));
    verifyNoMoreInteractions(restTemplate);
  }

  @ParameterizedTest(
      name = "Should update contact details when operation is {0} and table is ContactDetails")
  @EnumSource(value = Operation.class, names = {"LOAD", "INSERT", "UPDATE", "DELETE"})
  void shouldUpdateContactDetails(Operation operation) throws JsonProcessingException {
    recrd.setTable("ContactDetails");
    recrd.setOperation(operation);
    recrd.setData(data);

    Optional<Person> person = Optional.of(new Person());

    when(personService.findById("idValue")).thenReturn(person);

    service.syncRecord(recrd);

    ArgumentCaptor<PublishRequest> requestCaptor = ArgumentCaptor.forClass(PublishRequest.class);
    verify(snsService).publish(requestCaptor.capture());

    PublishRequest request = requestCaptor.getValue();
    assertThat("Unexpected topic ARN.", request.getTopicArn(),
        is(UPDATE_CONTACT_DETAILS_EVENT_ARN));

    Map<String, Object> message = objectMapper.readValue(request.getMessage(),
        new TypeReference<>() {
        });
    assertThat("Unexpected TIS ID.", message.get("tisId"), is("idValue"));

    Record messageRecord = objectMapper.convertValue(message.get("record"), Record.class);
    if (operation.equals(DELETE)) {
      assertThat("Unexpected record.", messageRecord, is(new Record()));
    } else {
      assertThat("Unexpected record.", messageRecord, is(recrd));
    }

    verifyNoMoreInteractions(snsService);
    verifyNoInteractions(restTemplate);
  }

  @ParameterizedTest(
      name = "Should update GDC details when operation is {0} and table is GdcDetails")
  @EnumSource(value = Operation.class, names = {"LOAD", "INSERT", "UPDATE", "DELETE"})
  void shouldUpdateGdcDetails(Operation operation) throws JsonProcessingException {
    Map<String, String> data = new HashMap<>();
    data.put("gdcNumber", "gdcNumberValue");
    data.put("gdcStatus", "gdcStatusValue");

    recrd.setTable("GdcDetails");
    recrd.setOperation(operation);
    recrd.setData(data);

    Optional<Person> person = Optional.of(new Person());

    when(personService.findById("idValue")).thenReturn(person);

    service.syncRecord(recrd);

    ArgumentCaptor<PublishRequest> requestCaptor = ArgumentCaptor.forClass(PublishRequest.class);
    verify(snsService).publish(requestCaptor.capture());

    PublishRequest request = requestCaptor.getValue();
    assertThat("Unexpected topic ARN.", request.getTopicArn(), is(UPDATE_GDC_DETAILS_EVENT_ARN));

    Map<String, Object> message = objectMapper.readValue(request.getMessage(),
        new TypeReference<>() {
        });
    assertThat("Unexpected TIS ID.", message.get("tisId"), is("idValue"));

    Record messageRecord = objectMapper.convertValue(message.get("record"), Record.class);
    if (operation.equals(DELETE)) {
      assertThat("Unexpected record.", messageRecord, is(new Record()));
    } else {
      assertThat("Unexpected record.", messageRecord, is(recrd));
    }

    verifyNoMoreInteractions(snsService);
    verifyNoInteractions(restTemplate);
  }

  @ParameterizedTest(
      name = "Should update GMC details when operation is {0} and table is GmcDetails")
  @EnumSource(value = Operation.class, names = {"LOAD", "INSERT", "UPDATE", "DELETE"})
  void shouldUpdateGmcDetails(Operation operation) throws JsonProcessingException {
    Map<String, String> data = new HashMap<>();
    data.put("gmcNumber", "gmcNumberValue");
    data.put("gmcStatus", "gmcStatusValue");

    recrd.setTable("GmcDetails");
    recrd.setOperation(operation);
    recrd.setData(data);

    Optional<Person> person = Optional.of(new Person());

    when(personService.findById("idValue")).thenReturn(person);

    service.syncRecord(recrd);

    ArgumentCaptor<PublishRequest> requestCaptor = ArgumentCaptor.forClass(PublishRequest.class);
    verify(snsService).publish(requestCaptor.capture());

    PublishRequest request = requestCaptor.getValue();
    assertThat("Unexpected topic ARN.", request.getTopicArn(), is(UPDATE_GMC_DETAILS_EVENT_ARN));

    Map<String, Object> message = objectMapper.readValue(request.getMessage(),
        new TypeReference<>() {
        });
    assertThat("Unexpected TIS ID.", message.get("tisId"), is("idValue"));

    Record messageRecord = objectMapper.convertValue(message.get("record"), Record.class);
    if (operation.equals(DELETE)) {
      assertThat("Unexpected record.", messageRecord, is(new Record()));
    } else {
      assertThat("Unexpected record.", messageRecord, is(recrd));
    }

    verifyNoMoreInteractions(snsService);
    verifyNoInteractions(restTemplate);
  }

  @ParameterizedTest(
      name = "Should update person owner when operation is {0} and table is PersonOwner")
  @EnumSource(value = Operation.class, names = {"LOAD", "INSERT", "UPDATE", "DELETE"})
  void shouldUpdatePersonOwnerInfo(Operation operation) throws JsonProcessingException {
    Map<String, String> data = new HashMap<>();
    data.put("owner", "personOwnerValue");

    recrd.setTable("PersonOwner");
    recrd.setOperation(operation);
    recrd.setData(data);

    Optional<Person> person = Optional.of(new Person());

    when(personService.findById("idValue")).thenReturn(person);

    service.syncRecord(recrd);

    ArgumentCaptor<PublishRequest> requestCaptor = ArgumentCaptor.forClass(PublishRequest.class);
    verify(snsService).publish(requestCaptor.capture());

    PublishRequest request = requestCaptor.getValue();
    assertThat("Unexpected topic ARN.", request.getTopicArn(), is(UPDATE_PERSON_OWNER_EVENT_ARN));

    Map<String, Object> message = objectMapper.readValue(request.getMessage(),
        new TypeReference<>() {
        });
    assertThat("Unexpected TIS ID.", message.get("tisId"), is("idValue"));

    Record messageRecord = objectMapper.convertValue(message.get("record"), Record.class);
    if (operation.equals(DELETE)) {
      assertThat("Unexpected record.", messageRecord, is(new Record()));
    } else {
      assertThat("Unexpected record.", messageRecord, is(recrd));
    }

    verifyNoMoreInteractions(snsService);
    verifyNoInteractions(restTemplate);
  }

  @ParameterizedTest(
      name = "Should update personal info when operation is {0} and table is PersonalDetails")
  @EnumSource(value = Operation.class, names = {"LOAD", "INSERT", "UPDATE", "DELETE"})
  void shouldUpdatePersonalInfo(Operation operation) throws JsonProcessingException {
    Map<String, String> data = new HashMap<>();
    data.put("dateOfBirth", "1978-03-23");
    data.put("gender", "genderValue");

    recrd.setTable("PersonalDetails");
    recrd.setOperation(operation);
    recrd.setData(data);
    Optional<Person> person = Optional.of(new Person());

    when(personService.findById("idValue")).thenReturn(person);

    service.syncRecord(recrd);

    ArgumentCaptor<PublishRequest> requestCaptor = ArgumentCaptor.forClass(PublishRequest.class);
    verify(snsService).publish(requestCaptor.capture());

    PublishRequest request = requestCaptor.getValue();
    assertThat("Unexpected topic ARN.", request.getTopicArn(), is(UPDATE_PERSONAL_INFO_EVENT_ARN));

    Map<String, Object> message = objectMapper.readValue(request.getMessage(),
        new TypeReference<>() {
        });
    assertThat("Unexpected TIS ID.", message.get("tisId"), is("idValue"));

    Record messageRecord = objectMapper.convertValue(message.get("record"), Record.class);
    if (operation.equals(DELETE)) {
      assertThat("Unexpected record.", messageRecord, is(new Record()));
    } else {
      assertThat("Unexpected record.", messageRecord, is(recrd));
    }

    verifyNoMoreInteractions(snsService);
    verifyNoInteractions(restTemplate);
  }

  @ParameterizedTest(
      name = "Should patch qualifications when operation is {0} and table is Qualification")
  @EnumSource(value = Operation.class, names = {"LOAD", "INSERT", "UPDATE"})
  void shouldPatchQualifications(Operation operation) {
    LocalDate now = LocalDate.now();

    Map<String, String> data = Map.of(
        "personId", "personIdValue",
        "qualification", "qualificationValue",
        "qualificationAttainedDate", now.toString(),
        "medicalSchool", "medicalSchoolValue");

    recrd.setTable("Qualification");
    recrd.setOperation(operation);
    recrd.setData(data);

    Optional<Person> person = Optional.of(new Person());

    when(personService.findById(anyString())).thenReturn(person);

    service.syncRecord(recrd);

    TraineeDetailsDto expectedDto = new TraineeDetailsDto();
    expectedDto.setTisId("idValue");
    expectedDto.setTraineeTisId("personIdValue");
    expectedDto.setQualification("qualificationValue");
    expectedDto.setDateAttained(now);
    expectedDto.setMedicalSchool("medicalSchoolValue");

    verify(restTemplate)
        .patchForObject(anyString(), eq(expectedDto), eq(Object.class), eq("qualification"),
            eq("personIdValue"));
    verifyNoMoreInteractions(restTemplate);
  }

  @ParameterizedTest(
      name = "Should patch placements when operation is {0} and table is Placement")
  @EnumSource(value = Operation.class, names = {"LOAD", "INSERT", "UPDATE"})
  void shouldPatchPlacements(Operation operation) throws JsonProcessingException {
    Set<Map<String, String>> otherSites = Set.of(Map.of(
            "name", "nameValue1",
            "siteKnownAs", "siteKnownAsValue1",
            "siteLocation", "siteLocationValue1"
        ),
        Map.of(
            "name", "nameValue2",
            "siteKnownAs", "siteKnownAsValue2",
            "siteLocation", "siteLocationValue2"
        )
    );

    Map<String, String> data = new HashMap<>();
    data.put("traineeId", "traineeIdValue");
    data.put("dateFrom", LocalDate.MIN.toString());
    data.put("dateTo", LocalDate.MAX.toString());
    data.put("gradeAbbreviation", "gradeAbbreviationValue");
    data.put("placementType", "placementTypeValue");
    data.put("status", "statusValue");
    data.put("employingBodyName", "employingBodyNameValue");
    data.put("trainingBodyName", "trainingBodyNameValue");
    data.put("site", "siteValue");
    data.put("siteLocation", "siteLocationValue");
    data.put("siteKnownAs", "siteKnownAsValue");
    data.put("otherSites", objectMapper.writeValueAsString(otherSites));
    data.put("specialty", "specialtyValue");
    data.put("placementWholeTimeEquivalent", "wholeTimeEquivalentValue");

    recrd.setTable("Placement");
    recrd.setOperation(operation);
    recrd.setData(data);

    Optional<Person> person = Optional.of(new Person());

    when(personService.findById(anyString())).thenReturn(person);

    service.syncRecord(recrd);

    ArgumentCaptor<TraineeDetailsDto> dtoCaptor = ArgumentCaptor.forClass(TraineeDetailsDto.class);
    verify(restTemplate)
        .patchForObject(anyString(), dtoCaptor.capture(), eq(Object.class), eq("placement"),
            eq("traineeIdValue"));
    verifyNoMoreInteractions(restTemplate);

    TraineeDetailsDto dto = dtoCaptor.getValue();
    assertThat("Unexpected trainee TIS ID.", dto.getTraineeTisId(), is("traineeIdValue"));
    assertThat("Unexpected start date.", dto.getStartDate(), is(LocalDate.MIN));
    assertThat("Unexpected end date.", dto.getEndDate(), is(LocalDate.MAX));
    assertThat("Unexpected grade.", dto.getGrade(), is("gradeAbbreviationValue"));
    assertThat("Unexpected placement type.", dto.getPlacementType(), is("placementTypeValue"));
    assertThat("Unexpected status.", dto.getStatus(), is("statusValue"));
    assertThat("Unexpected employing body.", dto.getEmployingBody(), is("employingBodyNameValue"));
    assertThat("Unexpected training body.", dto.getTrainingBody(), is("trainingBodyNameValue"));
    assertThat("Unexpected site.", dto.getSite(), is("siteValue"));
    assertThat("Unexpected site location.", dto.getSiteLocation(), is("siteLocationValue"));
    assertThat("Unexpected site known as.", dto.getSiteKnownAs(), is("siteKnownAsValue"));
    assertThat("Unexpected specialty.", dto.getSpecialty(), is("specialtyValue"));
    assertThat("Unexpected whole time equivalent.", dto.getWholeTimeEquivalent(),
        is("wholeTimeEquivalentValue"));

    Set<Map<String, String>> otherSitesData = dto.getOtherSites();
    assertThat("Unexpected other site count.", otherSites.size(), is(2));

    List<Map<String, String>> sortedOtherSitesData = otherSitesData.stream()
        .sorted(Comparator.comparing(os -> os.get("name"))).toList();
    Map<String, String> otherSiteData1 = sortedOtherSitesData.get(0);
    assertThat("Unexpected site name.", otherSiteData1.get("name"), is("nameValue1"));
    assertThat("Unexpected site known as.", otherSiteData1.get("siteKnownAs"),
        is("siteKnownAsValue1"));
    assertThat("Unexpected site location.", otherSiteData1.get("siteLocation"),
        is("siteLocationValue1"));

    Map<String, String> otherSiteData2 = sortedOtherSitesData.get(1);
    assertThat("Unexpected site name.", otherSiteData2.get("name"), is("nameValue2"));
    assertThat("Unexpected site known as.", otherSiteData2.get("siteKnownAs"),
        is("siteKnownAsValue2"));
    assertThat("Unexpected site location.", otherSiteData2.get("siteLocation"),
        is("siteLocationValue2"));
  }

  @Test
  void shouldDeleteQualificationWhenOperationDelete() {
    Map<String, String> data = Map.of(
        "personId", "personIdValue");

    recrd.setTable("Qualification");
    recrd.setOperation(DELETE);
    recrd.setData(data);

    Optional<Person> person = Optional.of(new Person());

    when(personService.findById(anyString())).thenReturn(person);

    service.syncRecord(recrd);

    verify(restTemplate)
        .delete(anyString(), eq("qualification"), eq("personIdValue"), eq("idValue"));
    verifyNoMoreInteractions(restTemplate);
  }

  @Test
  void shouldDeletePerson() {
    recrd.setTable("Person");
    recrd.setOperation(DELETE);

    Optional<Person> person = Optional.of(new Person());
    when(personService.findById(anyString())).thenReturn(person);

    service.syncRecord(recrd);

    verify(restTemplate).delete(contains("/trainee-profile/{tisId}"), eq("idValue"));
    verify(personService).deleteById("idValue");
  }

  @ParameterizedTest(
      name = "Should delete programme memberships when operation is {0} and table is "
          + "ProgrammeMembership")
  @EnumSource(value = Operation.class, names = {"DELETE"})
  void shouldDeleteProgrammeMembershipsWhenOperationDelete(Operation operation) {
    Map<String, String> data = Map.of(
        "personId", "personIdValue");

    recrd.setTable("ProgrammeMembership");
    recrd.setOperation(operation);
    recrd.setData(data);

    Optional<Person> person = Optional.of(new Person());

    when(personService.findById(anyString())).thenReturn(person);

    service.syncRecord(recrd);

    verify(restTemplate)
        .delete(anyString(), eq("programme-membership"),
            eq("personIdValue"), eq("idValue"));
    verifyNoMoreInteractions(restTemplate);
  }

  @ParameterizedTest(name = "Should delete placement when operation is {0} and table is Placement")
  @EnumSource(value = Operation.class, names = {"DELETE"})
  void shouldDeletePlacementWhenOperationDelete(Operation operation) {
    Map<String, String> data = Map.of(
        "traineeId", "traineeIdValue");

    recrd.setTable("Placement");
    recrd.setOperation(operation);
    recrd.setData(data);

    Optional<Person> person = Optional.of(new Person());

    when(personService.findById(anyString())).thenReturn(person);

    service.syncRecord(recrd);

    verify(restTemplate)
        .delete(anyString(), eq("placement"), eq("traineeIdValue"), eq("idValue"));
    verifyNoMoreInteractions(restTemplate);
  }

  @ParameterizedTest(name = "Should issue update event when operation is Delete and table is {0}")
  @ValueSource(strings = {TABLE_PLACEMENT, TABLE_PROGRAMME_MEMBERSHIP})
  void shouldIssueEventForSpecifiedTablesWhenOperationDelete(String table)
      throws JsonProcessingException {
    Map<String, String> data = Map.of("traineeId", "traineeIdValue");

    recrd.setTable(table);
    recrd.setOperation(DELETE);
    recrd.setData(data);

    Optional<Person> person = Optional.of(new Person());
    when(personService.findById(any())).thenReturn(person);

    service.syncRecord(recrd);

    ArgumentCaptor<PublishRequest> requestCaptor = ArgumentCaptor.forClass(PublishRequest.class);
    verify(snsService).publish(requestCaptor.capture());
    PublishRequest request = requestCaptor.getValue();
    Map<String, String> message = new ObjectMapper().readValue(request.getMessage(), Map.class);
    assertThat("Unexpected event id.", message.get("tisId"), is("idValue"));
    assertThat("Unexpected request topic ARN.", request.getTopicArn(),
        is(TABLE_NAME_TO_DELETE_EVENT_ARN.get(table)));

    verifyNoMoreInteractions(snsService);
  }

  /**
   * Provide the cartesian product of the tables and update operations that should trigger events.
   *
   * @return The stream of arguments.
   */
  private static Stream<Arguments> provideUpdateParameters() {
    return Stream.of(UPDATE, LOAD, INSERT).flatMap(operation ->
        Stream.of(TABLE_CONTACT_DETAILS, TABLE_GDC_DETAILS, TABLE_GMC_DETAILS, TABLE_PERSON_OWNER,
                TABLE_PERSONAL_DETAILS, TABLE_PLACEMENT, TABLE_PROGRAMME_MEMBERSHIP)
            .flatMap(table -> Stream.of(
                Arguments.of(operation, table)
            ))
    );
  }

  @ParameterizedTest(name = "Should issue update event when operation is {0} and table is {1}")
  @MethodSource("provideUpdateParameters")
  void shouldIssueEventForSpecifiedTablesWhenOperationUpdate(Operation operation, String table)
      throws JsonProcessingException {
    Map<String, String> data = Map.of("traineeId", "traineeIdValue");

    recrd.setTable(table);
    recrd.setOperation(operation);
    recrd.setData(data);

    Optional<Person> person = Optional.of(new Person());
    when(personService.findById(any())).thenReturn(person);

    service.syncRecord(recrd);

    ArgumentCaptor<PublishRequest> requestCaptor = ArgumentCaptor.forClass(PublishRequest.class);
    verify(snsService).publish(requestCaptor.capture());
    PublishRequest request = requestCaptor.getValue();
    Map<String, String> message = new ObjectMapper().readValue(request.getMessage(), Map.class);
    assertThat("Unexpected event id.", message.get("tisId"), is("idValue"));
    assertThat("Unexpected request topic ARN.", request.getTopicArn(),
        is(TABLE_NAME_TO_UPDATE_EVENT_ARN.get(table)));
    assertThat("Unexpected message group id.", request.getMessageGroupId(), nullValue());

    verifyNoMoreInteractions(snsService);
  }

  @ParameterizedTest
  @MethodSource("provideUpdateParameters")
  void shouldSetMessageGroupIdOnIssuedEventWhenFifoQueue(Operation operation, String table) {
    Map<String, String> data = Map.of("traineeId", "traineeIdValue");

    recrd.setSchema("dummySchema");
    recrd.setTisId("40");
    recrd.setTable(table);
    recrd.setOperation(operation);
    recrd.setData(data);

    Optional<Person> person = Optional.of(new Person());
    when(personService.findById(any())).thenReturn(person);

    EventNotificationProperties eventNotificationProperties = new EventNotificationProperties(
        DELETE_PLACEMENT_EVENT_ARN + FIFO, DELETE_PROGRAMME_MEMBERSHIP_EVENT_ARN + FIFO,
        UPDATE_CONDITIONS_OF_JOINING_EVENT_ARN + FIFO,
        UPDATE_CONTACT_DETAILS_EVENT_ARN + FIFO, UPDATE_GDC_DETAILS_EVENT_ARN + FIFO,
        UPDATE_GMC_DETAILS_EVENT_ARN + FIFO, UPDATE_PERSON_EVENT_ARN + FIFO,
        UPDATE_PERSON_OWNER_EVENT_ARN + FIFO, UPDATE_PERSONAL_INFO_EVENT_ARN + FIFO,
        UPDATE_PLACEMENT_EVENT_ARN + FIFO, UPDATE_PROGRAMME_MEMBERSHIP_EVENT_ARN + FIFO);
    TcsSyncService service = new TcsSyncService(restTemplate, mapper, personService,
        eventNotificationProperties, snsService, new ObjectMapper());

    service.syncRecord(recrd);

    ArgumentCaptor<PublishRequest> requestCaptor = ArgumentCaptor.forClass(PublishRequest.class);
    verify(snsService).publish(requestCaptor.capture());
    PublishRequest request = requestCaptor.getValue();
    assertThat("Unexpected message group id.", request.getMessageGroupId(),
        is("dummySchema_" + table + "_40"));

    verifyNoMoreInteractions(snsService);
  }

  @ParameterizedTest(name = "Should not issue update event: operation is Delete and table is {0}")
  @ValueSource(strings = {"some table"})
  void shouldNotIssueEventForOtherTablesWhenOperationDelete(String table) {
    Map<String, String> data = Map.of("traineeId", "traineeIdValue");

    recrd.setTable(table);
    recrd.setOperation(DELETE);
    recrd.setData(data);

    Optional<Person> person = Optional.of(new Person());
    when(personService.findById(any())).thenReturn(person);

    service.syncRecord(recrd);

    verifyNoInteractions(snsService);
  }

  @ParameterizedTest(name = "Should not issue update event: operation is {0} and unused table")
  @EnumSource(value = Operation.class, names = {"INSERT", "UPDATE", "LOAD"})
  void shouldNotIssueEventForOtherTablesWhenOperationUpdate(Operation operation) {
    Map<String, String> data = Map.of("traineeId", "traineeIdValue");

    recrd.setTable("some table");
    recrd.setOperation(operation);
    recrd.setData(data);

    Optional<Person> person = Optional.of(new Person());
    when(personService.findById(any())).thenReturn(person);

    service.syncRecord(recrd);

    verifyNoInteractions(snsService);
  }

  @ParameterizedTest(name = "Should not issue update event when operation is {0}")
  @EnumSource(value = Operation.class, names = {"DROP_TABLE", "CREATE_TABLE"})
  void shouldNotIssueEventWhenOperationIsNotApplicable(Operation operation) {
    Map<String, String> data = Map.of("traineeId", "traineeIdValue");

    recrd.setTable(TABLE_PLACEMENT);
    recrd.setOperation(operation);
    recrd.setData(data);

    Optional<Person> person = Optional.of(new Person());
    when(personService.findById(any())).thenReturn(person);

    service.syncRecord(recrd);

    verifyNoInteractions(snsService);
  }

  @Test
  void shouldNotThrowSnsExceptionsWhenIssuingEvent() {
    Map<String, String> data = Map.of("traineeId", "traineeIdValue");

    recrd.setTable(TABLE_PLACEMENT);
    recrd.setOperation(DELETE);
    recrd.setData(data);

    Optional<Person> person = Optional.of(new Person());
    when(personService.findById(any())).thenReturn(person);
    when(snsService.publish(any())).thenThrow(new AmazonSNSException("publish error"));

    assertDoesNotThrow(() -> service.syncRecord(recrd));
  }

  @Test
  void shouldNotDeleteCurriculumWhenOperationDelete() {
    Map<String, String> data = Map.of(
        "personId", "personIdValue",
        "traineeId", "traineeIdValue");

    recrd.setTable("Curriculum");
    recrd.setOperation(DELETE);
    recrd.setData(data);

    Optional<Person> person = Optional.of(new Person());

    when(personService.findById(anyString())).thenReturn(person);

    service.syncRecord(recrd);

    verifyNoInteractions(restTemplate);
  }

  @ParameterizedTest(name = "Should only update if the trainee is found within the "
      + "PersonRepository")
  @ValueSource(strings = {"ContactDetails", "GdcDetails", "GmcDetails", "Person", "PersonOwner",
      "PersonalDetails", "Qualification"})
  void shouldOnlyUpdateIfTheTraineeIsInTheRepository(String tableName) {
    recrd.setTable(tableName);
    recrd.setOperation(UPDATE);
    recrd.setData(data);

    service.syncRecord(recrd);

    verify(personService).findById(or(eq("idValue"), eq("personIdValue")));
    verifyNoInteractions(restTemplate);
  }

  @ParameterizedTest(
      name = "Should not throw error when delete returns a 404 error and table is {0}")
  @ValueSource(strings = {"Placement", "Qualification"})
  void shouldNotThrowErrorWhen404ErrorForDelete(String tableName) {
    recrd.setTable(tableName);
    recrd.setOperation(DELETE);
    data.put("traineeId", "personIdValue");
    recrd.setData(data);

    Optional<Person> person = Optional.of(new Person());

    when(personService.findById(or(eq("idValue"), eq("personIdValue"))))
        .thenReturn(person);

    doThrow(NotFound.create(HttpStatus.NOT_FOUND, "", HttpHeaders.EMPTY, new byte[0],
        StandardCharsets.UTF_8)).when(restTemplate)
        .delete(anyString(), anyString(), anyString(), anyString());

    assertDoesNotThrow(() -> service.syncRecord(recrd));
  }

  @ParameterizedTest(
      name = "Should throw error when delete returns a non-404 error and table is {0}")
  @ValueSource(strings = {"Placement", "Qualification"})
  void shouldThrowErrorWhenNon404ErrorForDelete(String tableName) {
    recrd.setTable(tableName);
    recrd.setOperation(DELETE);
    data.put("traineeId", "personIdValue");
    recrd.setData(data);

    Optional<Person> person = Optional.of(new Person());

    when(personService.findById(or(eq("idValue"), eq("personIdValue"))))
        .thenReturn(person);

    doThrow(NotFound.create(HttpStatus.FORBIDDEN, "", HttpHeaders.EMPTY, new byte[0],
        StandardCharsets.UTF_8)).when(restTemplate)
        .delete(anyString(), anyString(), anyString(), anyString());

    assertThrows(HttpClientErrorException.class, () -> service.syncRecord(recrd));
  }

  @ParameterizedTest(
      name = "Should throw error when trainee patch returns an error and table is {0}")
  @ValueSource(strings = {"Person", "Qualification"})
  void shouldThrowErrorWhenNon404ErrorForDetails(String tableName) {
    recrd.setTable(tableName);
    recrd.setOperation(UPDATE);
    recrd.setData(data);

    Optional<Person> person = Optional.of(new Person());

    when(personService.findById(or(eq("idValue"), eq("personIdValue"))))
        .thenReturn(person);

    when(
        restTemplate.patchForObject(anyString(), any(), eq(Object.class), anyString(), anyString()))
        .thenThrow(new HttpClientErrorException(HttpStatus.METHOD_NOT_ALLOWED));

    assertThrows(RestClientException.class, () -> service.syncRecord(recrd));
  }
}

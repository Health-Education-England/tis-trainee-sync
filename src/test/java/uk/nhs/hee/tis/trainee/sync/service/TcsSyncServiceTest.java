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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.nhs.hee.tis.trainee.sync.model.Operation.DELETE;
import static uk.nhs.hee.tis.trainee.sync.model.Operation.INSERT;
import static uk.nhs.hee.tis.trainee.sync.model.Operation.UPDATE;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.nhs.hee.tis.trainee.sync.dto.TraineeDetailsDto;
import uk.nhs.hee.tis.trainee.sync.mapper.TraineeDetailsMapperImpl;
import uk.nhs.hee.tis.trainee.sync.mapper.util.TraineeDetailsUtil;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.Person;
import uk.nhs.hee.tis.trainee.sync.model.Record;

class TcsSyncServiceTest {

  private static final String REQUIRED_ROLE = "DR in Training";

  private TcsSyncService service;

  private RestTemplate restTemplate;

  private PersonService personService;

  private Map<String, String> data;

  private Record record;

  @BeforeEach
  void setUp() {
    TraineeDetailsMapperImpl mapper = new TraineeDetailsMapperImpl();
    Field field = ReflectionUtils.findField(TraineeDetailsMapperImpl.class, "traineeDetailsUtil");
    assert field != null;
    field.setAccessible(true);
    ReflectionUtils.setField(field, mapper, new TraineeDetailsUtil());

    restTemplate = mock(RestTemplate.class);
    personService = mock(PersonService.class);
    service = new TcsSyncService(restTemplate, mapper, personService);

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

    record = new Record();
    record.setTisId("idValue");
  }

  @Test
  void shouldNotSyncRecordWhenTableNotSupported() {
    record.setTable("unsupportedTable");

    service.syncRecord(record);

    verifyNoInteractions(restTemplate);
  }

  @Test
  void shouldSaveRecordIntoPersonRepositoryIfRecordIsAPersonAndNotInPersonRepository() {
    Person record = new Person();
    record.setTisId("idValue");
    record.setTable("Person");
    record.setOperation(INSERT);
    data.put("role", REQUIRED_ROLE);
    record.setData(data);

    service.syncRecord(record);

    TraineeDetailsDto expectedDto = new TraineeDetailsDto();
    expectedDto.setTraineeTisId("idValue");
    expectedDto.setPublicHealthNumber("publicHealthNumberValue");

    verify(personService).save(record);
    verify(restTemplate)
        .patchForObject(anyString(), eq(expectedDto), eq(Object.class), eq("basic-details"),
            eq("idValue"));
    verifyNoMoreInteractions(restTemplate);
  }

  @ParameterizedTest(name = "Should not patch basic details when role is {0}")
  @ValueSource(strings = {"nonRequiredRole", "prefix-" + REQUIRED_ROLE, REQUIRED_ROLE + "-suffix",
      "prefix-" + REQUIRED_ROLE + "-suffix"})
  void shouldNotPatchBasicDetailsWhenRequiredRoleNotFound(String role) {
    Person record = new Person();
    record.setTisId("idValue");
    record.setTable("Person");
    record.setOperation(INSERT);
    record.setData(Collections.singletonMap("role", role));

    service.syncRecord(record);

    verifyNoInteractions(restTemplate);
    verify(personService,times(1)).findById(anyString());
  }

  @ParameterizedTest(
      name = "Should patch basic details when role is {0}, operation is load and table is Person")
  @ValueSource(strings = {"roleBefore," + REQUIRED_ROLE, REQUIRED_ROLE,
      REQUIRED_ROLE + ",roleAfter", "roleBefore," + REQUIRED_ROLE + ",roleAfter"})
  void shouldPatchBasicDetailsWhenRequiredRoleFound(String role) {

    Person record = new Person();
    record.setTisId("idValue");
    record.setTable("Person");
    record.setOperation(INSERT);
    data.put("role", role);
    record.setData(data);

    Optional<Person> person = Optional.of(new Person());

    when(personService.findById("idValue")).thenReturn(person);

    service.syncRecord(record);

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
    Person record = new Person();
    record.setTisId("idValue");
    record.setTable("Person");
    record.setOperation(operation);
    data.put("role", REQUIRED_ROLE);
    record.setData(data);

    Optional<Person> person = Optional.of(new Person());

    when(personService.findById("idValue")).thenReturn(person);

    service.syncRecord(record);

    TraineeDetailsDto expectedDto = new TraineeDetailsDto();
    expectedDto.setTraineeTisId("idValue");
    expectedDto.setPublicHealthNumber("publicHealthNumberValue");

    verify(restTemplate)
        .patchForObject(anyString(), eq(expectedDto), eq(Object.class), eq("basic-details"),
            eq("idValue"));
    verifyNoMoreInteractions(restTemplate);
  }

  @ParameterizedTest(
      name = "Should patch contact details when operation is {0} and table is ContactDetails")
  @EnumSource(value = Operation.class, names = {"LOAD", "INSERT", "UPDATE"})
  void shouldPatchContactDetails(Operation operation) {
    record.setTable("ContactDetails");
    record.setOperation(operation);
    record.setData(data);

    Optional<Person> person = Optional.of(new Person());

    when(personService.findById("idValue")).thenReturn(person);

    service.syncRecord(record);

    TraineeDetailsDto expectedDto = new TraineeDetailsDto();
    expectedDto.setTraineeTisId("idValue");
    expectedDto.setTitle("titleValue");
    expectedDto.setForenames("forenamesValue");
    expectedDto.setKnownAs("knownAsValue");
    expectedDto.setSurname("surnameValue");
    expectedDto.setMaidenName("maidenNameValue");
    expectedDto.setTelephoneNumber("telephoneNumberValue");
    expectedDto.setMobileNumber("mobileNumberValue");
    expectedDto.setEmail("emailValue");
    expectedDto.setAddress1("address1Value");
    expectedDto.setAddress2("address2Value");
    expectedDto.setAddress3("address3Value");
    expectedDto.setAddress4("address4Value");
    expectedDto.setPostCode("postCodeValue");

    verify(restTemplate)
        .patchForObject(anyString(), eq(expectedDto), eq(Object.class), eq("contact-details"),
            eq("idValue"));
    verifyNoMoreInteractions(restTemplate);
  }

  @ParameterizedTest(
      name = "Should patch GDC details when operation is {0} and table is GdcDetails")
  @EnumSource(value = Operation.class, names = {"LOAD", "INSERT", "UPDATE"})
  void shouldPatchGdcDetails(Operation operation) {
    Map<String, String> data = new HashMap<>();
    data.put("gdcNumber", "gdcNumberValue");
    data.put("gdcStatus", "gdcStatusValue");

    record.setTable("GdcDetails");
    record.setOperation(operation);
    record.setData(data);

    Optional<Person> person = Optional.of(new Person());

    when(personService.findById("idValue")).thenReturn(person);

    service.syncRecord(record);

    TraineeDetailsDto expectedDto = new TraineeDetailsDto();
    expectedDto.setTraineeTisId("idValue");
    expectedDto.setGdcNumber("gdcNumberValue");
    expectedDto.setGdcStatus("gdcStatusValue");

    verify(restTemplate)
        .patchForObject(anyString(), eq(expectedDto), eq(Object.class), eq("gdc-details"),
            eq("idValue"));
    verifyNoMoreInteractions(restTemplate);
  }

  @ParameterizedTest(
      name = "Should patch GMC details when operation is {0} and table is GmcDetails")
  @EnumSource(value = Operation.class, names = {"LOAD", "INSERT", "UPDATE"})
  void shouldPatchGmcDetails(Operation operation) {
    Map<String, String> data = new HashMap<>();
    data.put("gmcNumber", "gmcNumberValue");
    data.put("gmcStatus", "gmcStatusValue");

    record.setTable("GmcDetails");
    record.setOperation(operation);
    record.setData(data);

    Optional<Person> person = Optional.of(new Person());

    when(personService.findById("idValue")).thenReturn(person);

    service.syncRecord(record);

    TraineeDetailsDto expectedDto = new TraineeDetailsDto();
    expectedDto.setTraineeTisId("idValue");
    expectedDto.setGmcNumber("gmcNumberValue");
    expectedDto.setGmcStatus("gmcStatusValue");

    verify(restTemplate)
        .patchForObject(anyString(), eq(expectedDto), eq(Object.class), eq("gmc-details"),
            eq("idValue"));
    verifyNoMoreInteractions(restTemplate);
  }

  @ParameterizedTest(
      name = "Should patch person owner when operation is {0} and table is PersonOwner")
  @EnumSource(value = Operation.class, names = {"LOAD", "INSERT", "UPDATE"})
  void shouldPatchPersonOwnerInfo(Operation operation) {
    Map<String, String> data = new HashMap<>();
    data.put("owner", "personOwnerValue");

    record.setTable("PersonOwner");
    record.setOperation(operation);
    record.setData(data);

    Optional<Person> person = Optional.of(new Person());

    when(personService.findById("idValue")).thenReturn(person);

    service.syncRecord(record);

    TraineeDetailsDto expectedDto = new TraineeDetailsDto();
    expectedDto.setTraineeTisId("idValue");
    expectedDto.setPersonOwner("personOwnerValue");

    verify(restTemplate)
        .patchForObject(anyString(), eq(expectedDto), eq(Object.class), eq("person-owner"),
            eq("idValue"));
    verifyNoMoreInteractions(restTemplate);
  }

  @ParameterizedTest(
      name = "Should patch personal info when operation is {0} and table is PersonalDetails")
  @EnumSource(value = Operation.class, names = {"LOAD", "INSERT", "UPDATE"})
  void shouldPatchPersonalInfo(Operation operation) {
    Map<String, String> data = new HashMap<>();
    data.put("dateOfBirth", "1978-03-23");
    data.put("gender", "genderValue");

    record.setTable("PersonalDetails");
    record.setOperation(operation);
    record.setData(data);
    Optional<Person> person = Optional.of(new Person());

    when(personService.findById("idValue")).thenReturn(person);

    service.syncRecord(record);

    TraineeDetailsDto expectedDto = new TraineeDetailsDto();
    expectedDto.setTraineeTisId("idValue");
    expectedDto.setDateOfBirth("1978-03-23");
    expectedDto.setGender("genderValue");

    verify(restTemplate)
        .patchForObject(anyString(), eq(expectedDto), eq(Object.class), eq("personal-info"),
            eq("idValue"));
    verifyNoMoreInteractions(restTemplate);
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

    record.setTable("Qualification");
    record.setOperation(operation);
    record.setData(data);

    Optional<Person> person = Optional.of(new Person());

    when(personService.findById(anyString())).thenReturn(person);

    service.syncRecord(record);

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

  @ParameterizedTest(name = "Should only update if the trainee is found within the "
      + "PersonRepository")
  @ValueSource(strings = {"ContactDetails", "GdcDetails", "GmcDetails", "Person", "PersonOwner",
      "PersonalDetails", "Qualification"})
  void shouldOnlyUpdateIfTheTraineeIsInTheRepository(String tableName) {
    record.setTable(tableName);
    record.setOperation(UPDATE);
    record.setData(data);

    service.syncRecord(record);

    verify(personService).findById(or(eq("idValue"), eq("personIdValue")));
    verifyNoInteractions(restTemplate);
  }

  @ParameterizedTest(name = "Should trigger the default case stating that the operation DELETE is"
      + " unhandled for table {0}")
  @ValueSource(strings = {"ContactDetails", "GdcDetails", "GmcDetails", "Person", "PersonOwner",
      "PersonalDetails", "Qualification"})
  void shouldDoNothingWhenOperationIsDelete(String tableName) {
    record.setTable(tableName);
    record.setOperation(DELETE);
    record.setData(Map.of("role", REQUIRED_ROLE, "id", "idValue", "personId", "personIdValue"));

    Optional<Person> person = Optional.of(new Person());

    when(personService.findById(or(eq("idValue"), eq("personIdValue"))))
        .thenReturn(person);

    service.syncRecord(record);

    verify(personService).findById(or(eq("idValue"), eq("personIdValue")));
    verifyNoInteractions(restTemplate);
  }

  @ParameterizedTest(
      name = "Should throw error when trainee patch returns an error and table is {0}")
  @ValueSource(strings = {"ContactDetails", "GdcDetails", "GmcDetails", "Person", "PersonOwner",
      "PersonalDetails", "Qualification"})
  void shouldThrowErrorWhenNon404ErrorForDetails(String tableName) {
    record.setTable(tableName);
    record.setOperation(UPDATE);
    record.setData(data);

    Optional<Person> person = Optional.of(new Person());

    when(personService.findById(or(eq("idValue"), eq("personIdValue"))))
        .thenReturn(person);

    when(
        restTemplate.patchForObject(anyString(), any(), eq(Object.class), anyString(), anyString()))
        .thenThrow(new HttpClientErrorException(HttpStatus.METHOD_NOT_ALLOWED));

    assertThrows(RestClientException.class, () -> service.syncRecord(record));
  }
}

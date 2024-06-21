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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.nhs.hee.tis.trainee.sync.dto.ReferenceDto;
import uk.nhs.hee.tis.trainee.sync.dto.Status;
import uk.nhs.hee.tis.trainee.sync.mapper.ReferenceMapperImpl;
import uk.nhs.hee.tis.trainee.sync.mapper.util.ReferenceUtil;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.Record;

class ReferenceSyncServiceTest {

  private ReferenceSyncService service;

  private RestTemplate restTemplate;

  private Record recrd;

  @BeforeEach
  void setUp() {
    ReferenceMapperImpl mapper = new ReferenceMapperImpl();
    Field field = ReflectionUtils.findField(ReferenceMapperImpl.class, "referenceUtil");
    field.setAccessible(true);
    ReflectionUtils.setField(field, mapper, new ReferenceUtil());

    restTemplate = mock(RestTemplate.class);
    service = new ReferenceSyncService(restTemplate, mapper);

    recrd = new Record();
    recrd.setTisId("idValue");
  }

  @Test
  void shouldNotSyncRecordWhenTableNotSupported() {
    recrd.setTable("unsupportedTable");

    service.syncRecord(recrd);

    verifyNoInteractions(restTemplate);
  }

  @ParameterizedTest(name = "Should insert record when operation is LOAD and table is {0}")
  @CsvSource({"College,college", "Curriculum,curriculum", "DBC,dbc", "Gender,gender", "Grade,grade",
      "PermitToWork,immigration-status", "LocalOffice,local-office",
      "LocalOfficeContact,local-office-contact", "LocalOfficeContactType,local-office-contact-type",
      "ProgrammeMembershipType,programme-membership-type"})
  void shouldInsertRecordWhenOperationIsLoad(String tableName, String apiName) {
    recrd.setTable(tableName);
    recrd.setOperation(Operation.LOAD);

    Map<String, String> data = Map.of(
        "abbreviation", "abbreviationValue",
        "label", "labelValue",
        "status", "CURRENT",
        "uuid", "uuidValue",
        "code", "codeValue",
        "localOfficeId", "localOfficeIdValue",
        "contactTypeId", "contactTypeIdValue",
        "contact", "contactValue",
        "id", "idValue");
    recrd.setData(data);

    service.syncRecord(recrd);

    ReferenceDto expectedDto = new ReferenceDto();
    expectedDto.setTisId("idValue");
    expectedDto.setAbbreviation("abbreviationValue");
    expectedDto.setLabel("labelValue");
    expectedDto.setUuid("uuidValue");
    expectedDto.setCode("codeValue");
    expectedDto.setLocalOfficeId("localOfficeIdValue");
    expectedDto.setContactTypeId("contactTypeIdValue");
    expectedDto.setContact("contactValue");
    expectedDto.setStatus(Status.CURRENT);

    verify(restTemplate).postForLocation(anyString(), eq(expectedDto), eq(apiName));
    verifyNoMoreInteractions(restTemplate);
  }

  @ParameterizedTest(name = "Should insert record when operation is INSERT and table is {0}")
  @CsvSource({"College,college", "Curriculum,curriculum", "DBC,dbc", "Gender,gender", "Grade,grade",
      "PermitToWork,immigration-status", "LocalOffice,local-office",
      "LocalOfficeContact,local-office-contact", "LocalOfficeContactType,local-office-contact-type",
      "ProgrammeMembershipType,programme-membership-type"})
  void shouldInsertRecordWhenOperationIsInsert(String tableName, String apiName) {
    recrd.setTable(tableName);
    recrd.setOperation(Operation.INSERT);

    Map<String, String> data = Map.of(
        "abbreviation", "abbreviationValue",
        "label", "labelValue",
        "status", "CURRENT",
        "uuid", "uuidValue",
        "code", "codeValue",
        "localOfficeId", "localOfficeIdValue",
        "contactTypeId", "contactTypeIdValue",
        "contact", "contactValue",
        "id", "idValue");
    recrd.setData(data);

    service.syncRecord(recrd);

    ReferenceDto expectedDto = new ReferenceDto();
    expectedDto.setTisId("idValue");
    expectedDto.setAbbreviation("abbreviationValue");
    expectedDto.setLabel("labelValue");
    expectedDto.setUuid("uuidValue");
    expectedDto.setCode("codeValue");
    expectedDto.setLocalOfficeId("localOfficeIdValue");
    expectedDto.setContactTypeId("contactTypeIdValue");
    expectedDto.setContact("contactValue");
    expectedDto.setStatus(Status.CURRENT);

    verify(restTemplate).postForLocation(anyString(), eq(expectedDto), eq(apiName));
    verifyNoMoreInteractions(restTemplate);
  }

  @ParameterizedTest(name = "Should update record when operation is UPDATE and table is {0}")
  @CsvSource({"College,college", "Curriculum,curriculum", "DBC,dbc", "Gender,gender", "Grade,grade",
      "PermitToWork,immigration-status", "LocalOffice,local-office",
      "LocalOfficeContact,local-office-contact", "LocalOfficeContactType,local-office-contact-type",
      "ProgrammeMembershipType,programme-membership-type"})
  void shouldUpdateRecordWhenOperationIsUpdate(String tableName, String apiName) {
    recrd.setTable(tableName);
    recrd.setOperation(Operation.UPDATE);

    Map<String, String> data = Map.of(
        "abbreviation", "abbreviationValue",
        "label", "labelValue",
        "status", "CURRENT",
        "uuid", "uuidValue",
        "code", "codeValue",
        "localOfficeId", "localOfficeIdValue",
        "contactTypeId", "contactTypeIdValue",
        "contact", "contactValue",
        "id", "idValue");
    recrd.setData(data);

    service.syncRecord(recrd);

    ReferenceDto expectedDto = new ReferenceDto();
    expectedDto.setTisId("idValue");
    expectedDto.setAbbreviation("abbreviationValue");
    expectedDto.setLabel("labelValue");
    expectedDto.setUuid("uuidValue");
    expectedDto.setCode("codeValue");
    expectedDto.setLocalOfficeId("localOfficeIdValue");
    expectedDto.setContactTypeId("contactTypeIdValue");
    expectedDto.setContact("contactValue");
    expectedDto.setStatus(Status.CURRENT);

    verify(restTemplate).put(anyString(), eq(expectedDto), eq(apiName));
    verifyNoMoreInteractions(restTemplate);
  }

  @ParameterizedTest(name = "Should delete record when operation is DELETE and table is {0}")
  @CsvSource({"College,college", "Curriculum,curriculum", "DBC,dbc", "Gender,gender", "Grade,grade",
      "PermitToWork,immigration-status", "LocalOffice,local-office",
      "LocalOfficeContact,local-office-contact", "LocalOfficeContactType,local-office-contact-type",
      "ProgrammeMembershipType,programme-membership-type"})
  void shouldDeleteRecordWhenOperationIsDelete(String tableName, String apiName) {
    recrd.setTisId("40");
    recrd.setData(Collections.singletonMap("id", "40"));
    recrd.setTable(tableName);
    recrd.setOperation(Operation.DELETE);

    service.syncRecord(recrd);

    verify(restTemplate).delete(anyString(), eq(apiName), eq("40"));
    verifyNoMoreInteractions(restTemplate);
  }

  @ParameterizedTest(name = "Should delete record when operation is {0} and status is INACTIVE")
  @EnumSource(Operation.class)
  void shouldDeleteRecordWhenStatusIsInactive(Operation operation) {
    recrd.setTisId("40");
    recrd.setTable("Grade");
    recrd.setOperation(operation);
    recrd.setData(Collections.singletonMap("status", "INACTIVE"));

    service.syncRecord(recrd);

    verify(restTemplate).delete(anyString(), eq("grade"), eq("40"));
    verifyNoMoreInteractions(restTemplate);
  }

  @ParameterizedTest(name = "Should delete record when operation is {0} and status is DELETE")
  @EnumSource(Operation.class)
  void shouldDeleteRecordWhenStatusIsDelete(Operation operation) {
    recrd.setTisId("40");
    recrd.setTable("Grade");
    recrd.setOperation(operation);
    recrd.setData(Collections.singletonMap("status", "DELETE"));

    service.syncRecord(recrd);

    verify(restTemplate).delete(anyString(), eq("grade"), eq("40"));
    verifyNoMoreInteractions(restTemplate);
  }

  @Test
  void shouldNotThrowWhenValidationFails() {
    recrd.setTisId("40");
    recrd.setTable("Grade");
    recrd.setOperation(Operation.LOAD);

    HttpClientErrorException exception = new HttpClientErrorException(
        HttpStatus.UNPROCESSABLE_ENTITY);
    when(restTemplate.postForLocation(anyString(), any(ReferenceDto.class), anyString()))
        .thenThrow(exception);

    assertDoesNotThrow(() -> service.syncRecord(recrd));
  }

  @Test
  void shouldThrowWhenNonValidationFailure() {
    recrd.setTisId("40");
    recrd.setTable("Grade");
    recrd.setOperation(Operation.LOAD);
    recrd.setData(Collections.singletonMap("status", "CURRENT"));

    HttpClientErrorException exception = new HttpClientErrorException(
        HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    when(restTemplate.postForLocation(anyString(), any(ReferenceDto.class), anyString()))
        .thenThrow(exception);

    assertThrows(HttpClientErrorException.class, () -> service.syncRecord(recrd));
  }
}

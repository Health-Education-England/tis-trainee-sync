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

  private Record record;

  @BeforeEach
  void setUp() {
    ReferenceMapperImpl mapper = new ReferenceMapperImpl();
    Field field = ReflectionUtils.findField(ReferenceMapperImpl.class, "referenceUtil");
    field.setAccessible(true);
    ReflectionUtils.setField(field, mapper, new ReferenceUtil());

    restTemplate = mock(RestTemplate.class);
    service = new ReferenceSyncService(restTemplate, mapper);

    record = new Record();
    record.setTisId("idValue");
  }

  @Test
  void shouldNotSyncRecordWhenTableNotSupported() {
    record.setTable("unsupportedTable");

    service.syncRecord(record);

    verifyNoInteractions(restTemplate);
  }

  @ParameterizedTest(name = "Should insert record when operation is LOAD and table is {0}")
  @CsvSource({"College,college", "Curriculum,curriculum", "DBC,dbc", "Gender,gender", "Grade,grade",
      "PermitToWork,immigration-status", "LocalOffice,local-office"})
  void shouldInsertRecordWhenOperationIsLoad(String tableName, String apiName) {
    record.setTable(tableName);
    record.setOperation(Operation.LOAD);

    Map<String, String> data = Map.of(
        "abbreviation", "abbreviationValue",
        "label", "labelValue",
        "status", "CURRENT");
    record.setData(data);

    service.syncRecord(record);

    ReferenceDto expectedDto = new ReferenceDto();
    expectedDto.setTisId("idValue");
    expectedDto.setAbbreviation("abbreviationValue");
    expectedDto.setLabel("labelValue");
    expectedDto.setStatus(Status.CURRENT);

    verify(restTemplate).postForLocation(anyString(), eq(expectedDto), eq(apiName));
    verifyNoMoreInteractions(restTemplate);
  }

  @ParameterizedTest(name = "Should insert record when operation is INSERT and table is {0}")
  @CsvSource({"College,college", "Curriculum,curriculum", "DBC,dbc", "Gender,gender", "Grade,grade",
      "PermitToWork,immigration-status", "LocalOffice,local-office"})
  void shouldInsertRecordWhenOperationIsInsert(String tableName, String apiName) {
    record.setTable(tableName);
    record.setOperation(Operation.INSERT);

    Map<String, String> data = Map.of(
        "abbreviation", "abbreviationValue",
        "label", "labelValue",
        "status", "CURRENT");
    record.setData(data);

    service.syncRecord(record);

    ReferenceDto expectedDto = new ReferenceDto();
    expectedDto.setTisId("idValue");
    expectedDto.setAbbreviation("abbreviationValue");
    expectedDto.setLabel("labelValue");
    expectedDto.setStatus(Status.CURRENT);

    verify(restTemplate).postForLocation(anyString(), eq(expectedDto), eq(apiName));
    verifyNoMoreInteractions(restTemplate);
  }

  @ParameterizedTest(name = "Should update record when operation is UPDATE and table is {0}")
  @CsvSource({"College,college", "Curriculum,curriculum", "DBC,dbc", "Gender,gender", "Grade,grade",
      "PermitToWork,immigration-status", "LocalOffice,local-office"})
  void shouldUpdateRecordWhenOperationIsUpdate(String tableName, String apiName) {
    record.setTable(tableName);
    record.setOperation(Operation.UPDATE);

    Map<String, String> data = Map.of(
        "abbreviation", "abbreviationValue",
        "label", "labelValue",
        "status", "CURRENT");
    record.setData(data);

    service.syncRecord(record);

    ReferenceDto expectedDto = new ReferenceDto();
    expectedDto.setTisId("idValue");
    expectedDto.setAbbreviation("abbreviationValue");
    expectedDto.setLabel("labelValue");
    expectedDto.setStatus(Status.CURRENT);

    verify(restTemplate).put(anyString(), eq(expectedDto), eq(apiName));
    verifyNoMoreInteractions(restTemplate);
  }

  @ParameterizedTest(name = "Should delete record when operation is DELETE and table is {0}")
  @CsvSource({"College,college", "Curriculum,curriculum", "DBC,dbc", "Gender,gender", "Grade,grade",
      "PermitToWork,immigration-status", "LocalOffice,local-office"})
  void shouldDeleteRecordWhenOperationIsDelete(String tableName, String apiName) {
    record.setTisId("40");
    record.setTable(tableName);
    record.setOperation(Operation.DELETE);

    service.syncRecord(record);

    verify(restTemplate).delete(anyString(), eq(apiName), eq("40"));
    verifyNoMoreInteractions(restTemplate);
  }

  @ParameterizedTest(name = "Should delete record when operation is {0} and status is INACTIVE")
  @EnumSource(Operation.class)
  void shouldDeleteRecordWhenStatusIsInactive(Operation operation) {
    record.setTisId("40");
    record.setTable("Grade");
    record.setOperation(operation);
    record.setData(Collections.singletonMap("status", "INACTIVE"));

    service.syncRecord(record);

    verify(restTemplate).delete(anyString(), eq("grade"), eq("40"));
    verifyNoMoreInteractions(restTemplate);
  }

  @ParameterizedTest(name = "Should delete record when operation is {0} and status is DELETE")
  @EnumSource(Operation.class)
  void shouldDeleteRecordWhenStatusIsDelete(Operation operation) {
    record.setTisId("40");
    record.setTable("Grade");
    record.setOperation(operation);
    record.setData(Collections.singletonMap("status", "DELETE"));

    service.syncRecord(record);

    verify(restTemplate).delete(anyString(), eq("grade"), eq("40"));
    verifyNoMoreInteractions(restTemplate);
  }

  @Test
  void shouldNotThrowWhenValidationFails() {
    record.setTisId("40");
    record.setTable("Grade");
    record.setOperation(Operation.LOAD);

    HttpClientErrorException exception = new HttpClientErrorException(
        HttpStatus.UNPROCESSABLE_ENTITY);
    when(restTemplate.postForLocation(anyString(), any(ReferenceDto.class), anyString()))
        .thenThrow(exception);

    assertDoesNotThrow(() -> service.syncRecord(record));
  }

  @Test
  void shouldThrowWhenNonValidationFailure() {
    record.setTisId("40");
    record.setTable("Grade");
    record.setOperation(Operation.LOAD);
    record.setData(Collections.singletonMap("status", "CURRENT"));

    HttpClientErrorException exception = new HttpClientErrorException(
        HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    when(restTemplate.postForLocation(anyString(), any(ReferenceDto.class), anyString()))
        .thenThrow(exception);

    assertThrows(HttpClientErrorException.class, () -> service.syncRecord(record));
  }
}

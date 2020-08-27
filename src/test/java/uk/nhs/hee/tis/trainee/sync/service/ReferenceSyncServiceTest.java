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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.client.RestTemplate;
import uk.nhs.hee.tis.trainee.sync.dto.ReferenceDto;
import uk.nhs.hee.tis.trainee.sync.mapper.ReferenceMapperImpl;
import uk.nhs.hee.tis.trainee.sync.mapper.util.ReferenceUtil;
import uk.nhs.hee.tis.trainee.sync.model.Record;

class ReferenceSyncServiceTest {

  private ReferenceSyncService service;

  private RestTemplate restTemplate;

  @BeforeEach
  void setUp() {
    ReferenceMapperImpl mapper = new ReferenceMapperImpl();
    Field field = ReflectionUtils.findField(ReferenceMapperImpl.class, "referenceUtil");
    field.setAccessible(true);
    ReflectionUtils.setField(field, mapper, new ReferenceUtil());

    restTemplate = mock(RestTemplate.class);
    service = new ReferenceSyncService(restTemplate, mapper);
  }

  @Test
  void shouldNotSyncRecordWhenTableNotSupported() {
    Record record = new Record();
    record.setTable("unsupportedTable");

    service.syncRecord(record);

    verifyNoInteractions(restTemplate);
  }

  @Test
  void shouldNotSyncRecordWhenOperationNotSupported() {
    Record record = new Record();
    record.setTable("Grade");
    record.setOperation("unsupportedOperation");

    service.syncRecord(record);

    verifyNoInteractions(restTemplate);
  }

  @ParameterizedTest(name = "Should insert record when operation is LOAD and table is {0}")
  @CsvSource({"College,college", "Gender,gender", "Grade,grade", "PermitToWork,immigration-status",
      "LocalOffice,local-office"})
  void shouldInsertRecordWhenOperationIsLoad(String tableName, String apiName) {
    Record record = new Record();
    record.setTable(tableName);
    record.setOperation("load");

    service.syncRecord(record);

    verify(restTemplate).postForLocation(anyString(), any(ReferenceDto.class), eq(apiName));
    verifyNoMoreInteractions(restTemplate);
  }

  @ParameterizedTest(name = "Should insert record when operation is INSERT and table is {0}")
  @CsvSource({"College,college", "Gender,gender", "Grade,grade", "PermitToWork,immigration-status",
      "LocalOffice,local-office"})
  void shouldInsertRecordWhenOperationIsInsert(String tableName, String apiName) {
    Record record = new Record();
    record.setTable(tableName);
    record.setOperation("insert");

    service.syncRecord(record);

    verify(restTemplate).postForLocation(anyString(), any(ReferenceDto.class), eq(apiName));
    verifyNoMoreInteractions(restTemplate);
  }

  @ParameterizedTest(name = "Should update record when operation is UPDATE and table is {0}")
  @CsvSource({"College,college", "Gender,gender", "Grade,grade", "PermitToWork,immigration-status",
      "LocalOffice,local-office"})
  void shouldUpdateRecordWhenOperationIsUpdate(String tableName, String apiName) {
    Record record = new Record();
    record.setTable(tableName);
    record.setOperation("update");

    service.syncRecord(record);

    verify(restTemplate).put(anyString(), any(ReferenceDto.class), eq(apiName));
    verifyNoMoreInteractions(restTemplate);
  }

  @ParameterizedTest(name = "Should delete record when operation is DELETE and table is {0}")
  @CsvSource({"College,college", "Gender,gender", "Grade,grade", "PermitToWork,immigration-status",
      "LocalOffice,local-office"})
  void shouldDeleteRecordWhenOperationIsDelete(String tableName, String apiName) {
    Record record = new Record();
    record.setTable(tableName);
    record.setOperation("delete");
    record.setData(Collections.singletonMap("id", "40"));

    service.syncRecord(record);

    verify(restTemplate).delete(anyString(), eq(apiName), eq("40"));
    verifyNoMoreInteractions(restTemplate);
  }

  @ParameterizedTest(name = "Should delete record when operation is {0} and status is INACTIVE")
  @ValueSource(strings = {"load, insert, update, delete"})
  void shouldDeleteRecordWhenStatusIsInactive(String operation) {
    Record record = new Record();
    record.setTable("Grade");
    record.setOperation(operation);
    record.setData(Map.of("id", "40", "status", "INACTIVE"));

    service.syncRecord(record);

    verify(restTemplate).delete(anyString(), eq("grade"), eq("40"));
    verifyNoMoreInteractions(restTemplate);
  }
}

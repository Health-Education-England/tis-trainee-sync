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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.client.RestTemplate;
import uk.nhs.hee.tis.trainee.sync.dto.TraineeDetailsDto;
import uk.nhs.hee.tis.trainee.sync.mapper.TraineeDetailsMapperImpl;
import uk.nhs.hee.tis.trainee.sync.mapper.util.TraineeDetailsUtil;
import uk.nhs.hee.tis.trainee.sync.model.Record;

class TcsSyncServiceTest {

  private TcsSyncService service;

  private RestTemplate restTemplate;

  private Map<String, String> data;

  @BeforeEach
  void setUp() {
    TraineeDetailsMapperImpl mapper = new TraineeDetailsMapperImpl();
    Field field = ReflectionUtils.findField(TraineeDetailsMapperImpl.class, "traineeDetailsUtil");
    field.setAccessible(true);
    ReflectionUtils.setField(field, mapper, new TraineeDetailsUtil());

    restTemplate = mock(RestTemplate.class);
    service = new TcsSyncService(restTemplate, mapper);

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
    record.setTable("ContactDetails");
    record.setOperation("unsupportedOperation");

    service.syncRecord(record);

    verifyNoInteractions(restTemplate);
  }

  @ParameterizedTest(
      name = "Should patch contact details when operation is {0} and table is ContactDetails")
  @ValueSource(strings = {"load", "insert", "update"})
  void shouldPatchContactDetails(String operation) {
    Record record = new Record();
    record.setTable("ContactDetails");
    record.setOperation(operation);
    record.setData(data);

    service.syncRecord(record);

    TraineeDetailsDto expectedDto = new TraineeDetailsDto();
    expectedDto.setTisId("idValue");
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

  @ParameterizedTest(name = "Should do nothing when operation is DELETE and table is {0}")
  @CsvSource({"ContactDetails"})
  void shouldDoNothingWhenOperationIsDelete(String tableName) {
    Record record = new Record();
    record.setTable(tableName);
    record.setOperation("delete");

    service.syncRecord(record);

    verifyNoMoreInteractions(restTemplate);
  }
}

/*
 * The MIT License (MIT)
 *
 * Copyright 2023 Crown Copyright (Health Education England)
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

package uk.nhs.hee.tis.trainee.sync.mapper;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.nhs.hee.tis.trainee.sync.dto.RecordDto;
import uk.nhs.hee.tis.trainee.sync.mapper.util.RecordUtil;
import uk.nhs.hee.tis.trainee.sync.model.Record;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {RecordMapperImpl.class, RecordUtil.class})
class RecordMapperTest {

  private static final String UUID_FIELD = "uuid";
  private static final String UUID_VALUE = UUID.randomUUID().toString();
  private static final String ID_FIELD = "id";
  private static final String ID_VALUE = "123,456";

  private static final String TYPE_FIELD = "record-type";
  private static final String TYPE_VALUE = "data";
  private static final String OPERATION_FIELD = "operation";
  private static final String OPERATION_VALUE = "load";
  private static final String TIS_TRIGGER_FIELD = "tis-trigger";
  private static final String TIS_TRIGGER = "Update rejected";
  private static final String TIS_TRIGGER_DETAIL_FIELD = "tis-trigger-detail";
  private static final String TIS_TRIGGER_DETAIL = "Some details about this";

  @Autowired
  private RecordMapper mapper;
  Map<String, String> recordMetadata;

  @BeforeEach
  void setUp() {
    recordMetadata = Map.ofEntries(
        Map.entry(TYPE_FIELD, TYPE_VALUE),
        Map.entry(OPERATION_FIELD, OPERATION_VALUE),
        Map.entry(TIS_TRIGGER_FIELD, TIS_TRIGGER),
        Map.entry(TIS_TRIGGER_DETAIL_FIELD, TIS_TRIGGER_DETAIL)
    );
  }

  @Test
  void shouldPreferentiallyMapRecordDtoTisIdToRecordTisId() {
    Map<String, String> recordData = Map.ofEntries(
        Map.entry(ID_FIELD, ID_VALUE),
        Map.entry(UUID_FIELD, UUID_VALUE)
    );
    RecordDto recordDto = new RecordDto();
    recordDto.setData(recordData);
    recordDto.setMetadata(recordMetadata);

    Record record = mapper.toEntity(recordDto);
    assertThat("Unexpected tisId.", record.getTisId(), is(ID_VALUE));
  }

  @Test
  void shouldMapRecordDtoIdToRecordTisIdWhenOnlyTisId() {
    Map<String, String> recordData = Map.ofEntries(
        Map.entry(ID_FIELD, ID_VALUE)
    );
    RecordDto recordDto = new RecordDto();
    recordDto.setData(recordData);
    recordDto.setMetadata(recordMetadata);

    Record record = mapper.toEntity(recordDto);
    assertThat("Unexpected tisId.", record.getTisId(), is(ID_VALUE));
  }

  @Test
  void shouldMapRecordDtoUuidToRecordTisIdWhenOnlyUuid() {
    Map<String, String> recordData = Map.ofEntries(
        Map.entry(UUID_FIELD, UUID_VALUE)
    );
    RecordDto recordDto = new RecordDto();
    recordDto.setData(recordData);
    recordDto.setMetadata(recordMetadata);

    Record record = mapper.toEntity(recordDto);
    assertThat("Unexpected tisId.", record.getTisId(), is(UUID_VALUE));
  }

  @Test
  void shouldMapTisTrigger() {
    RecordDto recordDto = new RecordDto();
    recordDto.setMetadata(recordMetadata);

    Record recrd = mapper.toEntity(recordDto);
    assertThat("Unexpected tisTrigger.", recrd.getTisTrigger(), is(TIS_TRIGGER));
    assertThat("Unexpected tisTriggerDetail.", recrd.getTisTriggerDetail(),
        is(TIS_TRIGGER_DETAIL));
  }
}

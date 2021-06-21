/*
 * The MIT License (MIT)
 *
 * Copyright 2021 Crown Copyright (Health Education England)
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

package uk.nhs.hee.tis.trainee.sync.event;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Collections;
import javax.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import uk.nhs.hee.tis.trainee.sync.dto.RecordDto;
import uk.nhs.hee.tis.trainee.sync.service.RecordService;

@SpringBootTest
class RecordListenerTest {

  private RecordListener listener;

  private RecordService service;

  @BeforeEach
  void setUp() {
    service = mock(RecordService.class);
    listener = new RecordListener(service);
  }

  @Disabled("Unable to get the validation working during tests.")
  @Test
  void shouldNotProcessRecordWhenDataNull() {
    RecordDto record = new RecordDto();
    record.setMetadata(Collections.emptyMap());

    assertThrows(ConstraintViolationException.class, () -> listener.getRecord(record));

    verifyNoInteractions(service);
  }

  @Disabled("Unable to get the validation working during tests.")
  @Test
  void shouldNotProcessRecordWhenMetadataNull() {
    RecordDto record = new RecordDto();
    record.setData(Collections.emptyMap());

    assertThrows(ConstraintViolationException.class, () -> listener.getRecord(record));

    verifyNoInteractions(service);
  }

  @Disabled("Unable to get the validation working during tests.")
  @Test
  void shouldNotProcessRecordWhenDataAndMetadataNull() {
    RecordDto record = new RecordDto();

    assertThrows(ConstraintViolationException.class, () -> listener.getRecord(record));

    verifyNoInteractions(service);
  }

  @Test
  void shouldProcessRecordWhenDataAndMetadataNotNull() {
    RecordDto record = new RecordDto();
    record.setData(Collections.emptyMap());
    record.setMetadata(Collections.emptyMap());

    listener.getRecord(record);

    verify(service).processRecord(record);
  }
}

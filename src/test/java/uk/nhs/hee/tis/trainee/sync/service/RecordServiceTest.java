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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import uk.nhs.hee.tis.trainee.sync.model.Record;

class RecordServiceTest {

  private RecordService service;
  private ApplicationContext context;

  @BeforeEach
  void setUp() {
    context = mock(ApplicationContext.class);
    service = new RecordService(context);
  }

  @Test
  void shouldProcessRecordWithServiceMatchingSchema() {
    Record record = new Record();
    record.setSchema("testSchema");

    ReferenceSyncService syncService = mock(ReferenceSyncService.class);
    when(context.getBean("testSchema", SyncService.class)).thenReturn(syncService);

    service.processRecord(record);

    verify(syncService).syncRecord(record);
  }

  @Test
  void shouldNotThrowExceptionWhenSchemaNotSupported() {
    Record record = new Record();
    record.setSchema("testSchema");

    when(context.getBean("testSchema", SyncService.class))
        .thenThrow(new NoSuchBeanDefinitionException("Expected exception."));

    assertDoesNotThrow(() -> service.processRecord(record));
  }
}

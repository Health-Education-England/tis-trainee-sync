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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ReflectionUtils;
import uk.nhs.hee.tis.trainee.sync.dto.RecordDto;
import uk.nhs.hee.tis.trainee.sync.mapper.RecordMapperImpl;
import uk.nhs.hee.tis.trainee.sync.mapper.util.RecordUtil;
import uk.nhs.hee.tis.trainee.sync.model.Placement;
import uk.nhs.hee.tis.trainee.sync.model.Record;

class RecordServiceTest {

  private RecordService service;
  private ApplicationContext context;

  @BeforeEach
  void setUp() {
    context = mock(ApplicationContext.class);

    RecordMapperImpl mapper = new RecordMapperImpl();
    Field field = ReflectionUtils.findField(RecordMapperImpl.class, "recordUtil");
    field.setAccessible(true);
    ReflectionUtils.setField(field, mapper, new RecordUtil());

    service = new RecordService(context, mapper);
  }

  @Test
  void shouldThrowExceptionWhenRecordTypeIsNull() {
    RecordDto recordDto = new RecordDto();
    recordDto.setData(Collections.emptyMap());
    recordDto.setMetadata(
        Map.of("schema-name", "testSchema", "table-name", "testTable", "operation", "update"));

    assertThrows(IllegalArgumentException.class, () -> service.processRecord(recordDto));
  }

  @Test
  void shouldThrowExceptionWhenOperationIsNull() {
    RecordDto recordDto = new RecordDto();
    recordDto.setData(Collections.emptyMap());
    recordDto.setMetadata(
        Map.of("schema-name", "testSchema", "table-name", "testTable", "record-type", "data"));

    assertThrows(IllegalArgumentException.class, () -> service.processRecord(recordDto));
  }

  @Test
  void shouldSkipRecordWhenRecordTypeIsControl() {
    RecordDto recordDto = new RecordDto();
    recordDto.setData(Collections.emptyMap());
    recordDto.setMetadata(Map.of("schema-name", "testSchema", "table-name",
        "testTable", "operation", "update", "record-type", "control"));

    when(context.getBean("testTable", Record.class)).thenReturn(new Record());

    service.processRecord(recordDto);

    verify(context).getBean("testTable", Record.class);
    verifyNoMoreInteractions(context);
  }

  @Test
  void shouldUseTableServiceWhenTableServiceFound() {
    RecordDto recordDto = new RecordDto();
    recordDto.setData(Collections.emptyMap());
    recordDto.setMetadata(Map.of("schema-name", "testSchema", "table-name",
        "testTable", "operation", "update", "record-type", "data"));

    when(context.getBean("testTable", Record.class)).thenReturn(new Record());

    PlacementSyncService syncService = mock(PlacementSyncService.class);
    when(context.getBean("testSchema-testTable", SyncService.class)).thenReturn(syncService);

    service.processRecord(recordDto);

    ArgumentCaptor<Record> recordCaptor = ArgumentCaptor.forClass(Record.class);
    verify(syncService).syncRecord(recordCaptor.capture());

    Record record = recordCaptor.getValue();
    assertThat("Unexpected schema.", record.getSchema(), is("testSchema"));
    assertThat("Unexpected table.", record.getTable(), is("testTable"));
  }

  @Test
  void shouldUseSchemaServiceWhenTableServiceNotFoundAndSchemaServiceFound() {
    RecordDto recordDto = new RecordDto();
    recordDto.setData(Collections.emptyMap());
    recordDto.setMetadata(
        Map.of("schema-name", "testSchema", "table-name", "testTable", "operation", "load",
            "record-type", "data"));

    when(context.getBean("testTable", Record.class)).thenReturn(new Record());

    when(context.getBean("testSchema-testTable", SyncService.class))
        .thenThrow(new NoSuchBeanDefinitionException("Expected exception."));
    TcsSyncService syncService = mock(TcsSyncService.class);
    when(context.getBean("testSchema", SyncService.class)).thenReturn(syncService);

    service.processRecord(recordDto);

    ArgumentCaptor<Record> recordCaptor = ArgumentCaptor.forClass(Record.class);
    verify(syncService).syncRecord(recordCaptor.capture());

    Record record = recordCaptor.getValue();
    assertThat("Unexpected schema.", record.getSchema(), is("testSchema"));
    assertThat("Unexpected table.", record.getTable(), is("testTable"));
  }

  @Test
  void shouldNotThrowExceptionWhenTableServiceNotFoundAndSchemaServiceNotFound() {
    RecordDto recordDto = new RecordDto();
    recordDto.setData(Collections.emptyMap());
    recordDto.setMetadata(Map.of("schema-name", "testSchema", "table-name",
        "testTable", "operation", "load", "record-type", "data"));

    when(context.getBean("testTable", Record.class)).thenReturn(new Record());

    when(context.getBean("testSchema-testTable", SyncService.class))
        .thenThrow(new NoSuchBeanDefinitionException("Expected exception."));
    when(context.getBean("testSchema", SyncService.class))
        .thenThrow(new NoSuchBeanDefinitionException("Expected exception."));

    assertDoesNotThrow(() -> service.processRecord(recordDto));
  }

  @Test
  void shouldUseRecordSubTypeWithBeanMatchingTable() {
    RecordDto recordDto = new RecordDto();
    recordDto.setData(Collections.emptyMap());
    recordDto.setMetadata(Map.of("schema-name", "testSchema", "table-name",
        "testTable", "operation", "delete", "record-type", "data"));

    Placement placement = new Placement();

    when(context.getBean("testTable", Record.class)).thenReturn(placement);

    ReferenceSyncService syncService = mock(ReferenceSyncService.class);
    when(context.getBean("testSchema-testTable", SyncService.class)).thenReturn(syncService);

    service.processRecord(recordDto);

    ArgumentCaptor<Record> recordCaptor = ArgumentCaptor.forClass(Record.class);
    verify(syncService).syncRecord(recordCaptor.capture());

    Record record = recordCaptor.getValue();
    assertThat("Unexpected record class.", record, instanceOf(Placement.class));
    assertThat("Unexpected record instance.", record, sameInstance(placement));
    assertThat("Unexpected schema.", placement.getSchema(), is("testSchema"));
    assertThat("Unexpected table.", placement.getTable(), is("testTable"));
  }

  @Test
  void shouldUseParentRecordTypeWhenNoSubTypeForTable() {
    RecordDto recordDto = new RecordDto();
    recordDto.setData(Collections.emptyMap());
    recordDto.setMetadata(Map.of("schema-name", "testSchema", "table-name",
        "testTable", "operation", "insert", "record-type", "data"));

    when(context.getBean("testTable", Record.class))
        .thenThrow(new NoSuchBeanDefinitionException("Expected exception."));

    ReferenceSyncService syncService = mock(ReferenceSyncService.class);
    when(context.getBean("testSchema-testTable", SyncService.class)).thenReturn(syncService);

    service.processRecord(recordDto);

    ArgumentCaptor<Record> recordCaptor = ArgumentCaptor.forClass(Record.class);
    verify(syncService).syncRecord(recordCaptor.capture());

    Record record = recordCaptor.getValue();
    assertThat("Unexpected schema.", record.getSchema(), is("testSchema"));
    assertThat("Unexpected table.", record.getTable(), is("testTable"));
  }
}

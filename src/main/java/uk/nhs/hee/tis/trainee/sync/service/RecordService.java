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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.trainee.sync.dto.RecordDto;
import uk.nhs.hee.tis.trainee.sync.mapper.RecordMapper;
import uk.nhs.hee.tis.trainee.sync.model.ProgrammeMembership;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.model.RecordType;

@Slf4j
@Service
public class RecordService {

  private final ApplicationContext context;

  private final RecordMapper mapper;

  RecordService(ApplicationContext context, RecordMapper mapper) {
    this.context = context;
    this.mapper = mapper;
  }

  /**
   * Process the given record.
   *
   * @param recordDto The record to process.
   */
  public void processRecord(RecordDto recordDto) {
    Record recrd = convertToRecord(recordDto);

    if (recrd.getType().equals(RecordType.CONTROL)) {
      log.info("Skipping non-data record with operation '{}' on '{}.{}'.", recrd.getOperation(),
          recrd.getSchema(), recrd.getTable());
      return;
    }

    String schema = recrd.getSchema();

    try {
      SyncService service = getSyncService(recrd);
      service.syncRecord(recrd);
    } catch (BeansException e) {
      log.warn("Unhandled record schema '{}'.", schema);
    }
  }

  /**
   * Convert the DTO into an entity.
   *
   * @param recordDto The DTO to convert.
   * @return The Record entity, will be created as a Record subtype if available.
   */
  private Record convertToRecord(RecordDto recordDto) {
    Record source = mapper.toEntity(recordDto);
    String table = source.getTable();

    try {
      Record target = context.getBean(table, Record.class);
      mapper.copy(source, target);
      return target;
    } catch (BeansException e) {
      log.debug("No Record child type found for '{}'.", table);
      return source;
    }
  }

  /**
   * Get the sync service for the given record, the closest appropriate service is selected based on
   * the schema and table of the record.
   *
   * @param recrd The record to get the service for.
   * @return The discovered sync service.
   * @throws BeansException When no appropriate service was found.
   */
  private SyncService getSyncService(Record recrd) throws BeansException {
    SyncService service;
    String schema = recrd.getSchema();
    String table = recrd.getTable();

    try {
      String schemaTable = String.format("%s-%s", schema, table);
      service = context.getBean(schemaTable, SyncService.class);
      log.info("Sync service found for table '{}' in '{}'", table, schema);
    } catch (BeansException e) {
      log.debug("Sync service not found for table '{}', falling back to schema '{}'.", table,
          schema);
      service = context.getBean(schema, SyncService.class);
      log.info("Sync service found for schema '{}'", schema);
    }

    log.debug("Using sync service of type '{}'.", service.getClass());
    return service;
  }
}

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

package uk.nhs.hee.tis.trainee.sync.mapper;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import uk.nhs.hee.tis.trainee.sync.dto.RecordDto;
import uk.nhs.hee.tis.trainee.sync.mapper.util.RecordUtil;
import uk.nhs.hee.tis.trainee.sync.mapper.util.RecordUtil.Operation;
import uk.nhs.hee.tis.trainee.sync.mapper.util.RecordUtil.RecordType;
import uk.nhs.hee.tis.trainee.sync.model.Record;

@Mapper(componentModel = "spring", uses = RecordUtil.class)
public interface RecordMapper {

  @Mapping(target = "tisId", source = "data.id")
  @Mapping(target = "operation", source = "metadata", qualifiedBy = Operation.class)
  @Mapping(target = "type", source = "metadata", qualifiedBy = RecordType.class)
  @Mapping(target = "schema", source = "metadata.schema-name")
  @Mapping(target = "table", source = "metadata.table-name")
  @Mapping(target = "tisTrigger", source = "metadata.tis-trigger")
  @Mapping(target = "tisTriggerDetail", source = "metadata.tis-trigger-detail")
  Record toEntity(RecordDto recordDto);

  void copy(Record source, @MappingTarget Record target);

  /**
   * Set the tisId correctly from the UUID field if this is present.
   *
   * @param sourceDto the source DTO.
   * @param target    the record to update.
   */
  @AfterMapping
  default void setTisIdFromUuid(RecordDto sourceDto, @MappingTarget Record target) {
    String theUuid = sourceDto.getData().get("uuid");
    if (target.getTisId() == null && theUuid != null) {
      target.setTisId(theUuid);
    }
  }
}

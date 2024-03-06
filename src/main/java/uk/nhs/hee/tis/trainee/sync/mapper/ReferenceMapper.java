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

import static uk.nhs.hee.tis.trainee.sync.dto.Status.CURRENT;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import uk.nhs.hee.tis.trainee.sync.dto.ReferenceDto;
import uk.nhs.hee.tis.trainee.sync.mapper.util.ReferenceUtil;
import uk.nhs.hee.tis.trainee.sync.mapper.util.ReferenceUtil.Label;
import uk.nhs.hee.tis.trainee.sync.mapper.util.ReferenceUtil.Status;
import uk.nhs.hee.tis.trainee.sync.model.Record;

@Mapper(componentModel = "spring", uses = ReferenceUtil.class)
public interface ReferenceMapper {

  @Mapping(target = "abbreviation", source = "data.abbreviation")
  @Mapping(target = "label", source = "data", qualifiedBy = Label.class)
  @Mapping(target = "status", source = "data", qualifiedBy = Status.class)
  @Mapping(target = "placementGrade", source = "data.placementGrade")
  @Mapping(target = "trainingGrade", source = "data.trainingGrade")
  @Mapping(target = "curriculumSubType", source = "data.curriculumSubType")
  @Mapping(target = "type", source = "data.type")
  @Mapping(target = "internal", source = "data.internal")
  @Mapping(target = "uuid", source = "data.uuid")
  @Mapping(target = "code", source = "data.code")
  @Mapping(target = "localOfficeId", source = "data.localOfficeId")
  @Mapping(target = "contactTypeId", source = "data.contactTypeId")
  @Mapping(target = "contact", source = "data.contact")
  ReferenceDto toReference(Record recrd);

  /**
   * The LocalOfficeContact table does not have a status field, so set this manually.
   *
   * @param recrd  The record source.
   * @param target The DTO target.
   */
  @AfterMapping
  default void patchLocalOfficeContactStatus(Record recrd, @MappingTarget ReferenceDto target) {
    if (recrd.getTable() != null
        && recrd.getTable().equalsIgnoreCase("LocalOfficeContact")) {
      target.setStatus(CURRENT);
    }
  }
}

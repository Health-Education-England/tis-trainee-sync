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

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.nhs.hee.tis.trainee.sync.dto.ReferenceDto;
import uk.nhs.hee.tis.trainee.sync.mapper.util.ReferenceUtil;
import uk.nhs.hee.tis.trainee.sync.mapper.util.ReferenceUtil.Abbreviation;
import uk.nhs.hee.tis.trainee.sync.mapper.util.ReferenceUtil.CurriculumSubType;
import uk.nhs.hee.tis.trainee.sync.mapper.util.ReferenceUtil.Label;
import uk.nhs.hee.tis.trainee.sync.mapper.util.ReferenceUtil.PlacementGrade;
import uk.nhs.hee.tis.trainee.sync.mapper.util.ReferenceUtil.Status;
import uk.nhs.hee.tis.trainee.sync.mapper.util.ReferenceUtil.TrainingGrade;
import uk.nhs.hee.tis.trainee.sync.model.Record;

@Mapper(componentModel = "spring", uses = ReferenceUtil.class)
public interface ReferenceMapper {

  @Mapping(target = "abbreviation", source = "data", qualifiedBy = Abbreviation.class)
  @Mapping(target = "label", source = "data", qualifiedBy = Label.class)
  @Mapping(target = "status", source = "data", qualifiedBy = Status.class)
  @Mapping(target = "placementGrade", source = "data", qualifiedBy = PlacementGrade.class)
  @Mapping(target = "trainingGrade", source = "data", qualifiedBy = TrainingGrade.class)
  @Mapping(target = "curriculumSubType", source = "data", qualifiedBy = CurriculumSubType.class)
  ReferenceDto toReference(Record record);
}

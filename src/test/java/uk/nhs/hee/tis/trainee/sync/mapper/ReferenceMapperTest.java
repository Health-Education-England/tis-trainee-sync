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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.ReflectionUtils;
import uk.nhs.hee.tis.trainee.sync.dto.ReferenceDto;
import uk.nhs.hee.tis.trainee.sync.mapper.util.ReferenceUtil;
import uk.nhs.hee.tis.trainee.sync.model.Record;

class ReferenceMapperTest {

  private ReferenceMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new ReferenceMapperImpl();

    Field field = ReflectionUtils.findField(ReferenceMapperImpl.class, "referenceUtil");
    field.setAccessible(true);
    ReflectionUtils.setField(field, mapper, new ReferenceUtil());
  }

  @Test
  void shouldMapLabelToLabelWhenLabelPresent() {
    Record record = new Record();
    record.setData(Map.of("label", "labelContent", "name", "nameContent"));

    ReferenceDto reference = mapper.toReference(record);

    assertThat("Unexpected label.", reference.getLabel(), is("labelContent"));
  }

  @Test
  void shouldMapNameToLabelWhenLabelNotPresent() {
    Record record = new Record();
    record.setData(Collections.singletonMap("name", "nameContent"));

    ReferenceDto reference = mapper.toReference(record);

    assertThat("Unexpected label.", reference.getLabel(), is("nameContent"));
  }

  @Test
  void shouldMapPlacementGradeToBooleanWhenTrue() {
    Record record = new Record();
    record.setData(Collections.singletonMap("placementGrade", "true"));

    ReferenceDto reference = mapper.toReference(record);

    assertThat("Unexpected placement grade.", reference.getPlacementGrade(), is(true));
  }

  @Test
  void shouldMapPlacementGradeToBooleanWhenFalse() {
    Record record = new Record();
    record.setData(Collections.singletonMap("placementGrade", "false"));

    ReferenceDto reference = mapper.toReference(record);

    assertThat("Unexpected placement grade.", reference.getPlacementGrade(), is(false));
  }

  @Test
  void shouldMapPlacementGradeToNullWhenNotPresent() {
    ReferenceDto reference = mapper.toReference(new Record());

    assertThat("Unexpected placement grade.", reference.getPlacementGrade(), nullValue());
  }

  @Test
  void shouldMapTrainingGradeToBooleanWhenTrue() {
    Record record = new Record();
    record.setData(Collections.singletonMap("trainingGrade", "true"));

    ReferenceDto reference = mapper.toReference(record);

    assertThat("Unexpected training grade.", reference.getTrainingGrade(), is(true));
  }

  @Test
  void shouldMapTrainingGradeToBooleanWhenFalse() {
    Record record = new Record();
    record.setData(Collections.singletonMap("trainingGrade", "false"));

    ReferenceDto reference = mapper.toReference(record);

    assertThat("Unexpected training grade.", reference.getTrainingGrade(), is(false));
  }

  @Test
  void shouldMapTrainingGradeToNullWhenNotPresent() {
    ReferenceDto reference = mapper.toReference(new Record());

    assertThat("Unexpected training grade.", reference.getTrainingGrade(), nullValue());
  }

  @Test
  void shouldMapCurriculumSubTypeWhenPresent() {
    Record record = new Record();
    record.setData(Collections.singletonMap("curriculumSubType", "SUB_TYPE_1"));

    ReferenceDto reference = mapper.toReference(record);

    assertThat("Unexpected curriculum sub type.", reference.getCurriculumSubType(),
        is("SUB_TYPE_1"));
  }

  @Test
  void shouldMapCurriculumSubTypeToNullWhenNotPresent() {
    ReferenceDto reference = mapper.toReference(new Record());

    assertThat("Unexpected curriculum sub type.", reference.getCurriculumSubType(), nullValue());
  }
}

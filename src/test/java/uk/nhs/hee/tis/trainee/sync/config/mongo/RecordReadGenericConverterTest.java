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

package uk.nhs.hee.tis.trainee.sync.config.mongo;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter.ConvertiblePair;
import uk.nhs.hee.tis.trainee.sync.model.Placement;
import uk.nhs.hee.tis.trainee.sync.model.Record;

class RecordReadGenericConverterTest {

  private RecordReadGenericConverter converter;

  private ApplicationContext context;

  @BeforeEach
  void setUp() {
    context = mock(ApplicationContext.class);
    converter = new RecordReadGenericConverter(context);
  }

  @Test
  void shouldReturnSingleDocumentToRecordConvertableTypes() {
    Set<ConvertiblePair> convertibleTypes = converter.getConvertibleTypes();

    assertThat("Unexpected number of convertible types.", convertibleTypes.size(), is(1));

    ConvertiblePair convertiblePair = convertibleTypes.iterator().next();
    assertThat("Unexpected source type.", convertiblePair.getSourceType(), is(Document.class));
    assertThat("Unexpected target type.", convertiblePair.getTargetType(), is(Record.class));
  }

  @Test
  void shouldConvertToTheGivenSubType() {
    Document source = new Document();
    source.put("_id", "idValue");
    source.put("data1", "data1Value");
    source.put("data2", "data2Value");
    source.put("data3", "data3Value");
    source.put("data4", "data4Value");

    TypeDescriptor sourceType = TypeDescriptor.forObject(source);
    TypeDescriptor targetType = TypeDescriptor.valueOf(Placement.class);

    when(context.getBean(Placement.class)).thenReturn(new Placement());

    Object targetObject = converter.convert(source, sourceType, targetType);
    assertThat("Unexpected converted type.", targetObject, instanceOf(Placement.class));

    Placement target = (Placement) targetObject;
    assertThat("Unexpected tisID value.", target.getTisId(), is("idValue"));

    Map<String, String> data = target.getData();
    assertThat("Unexpected data size.", data.size(), is(4));
    assertThat("Unexpected data value.", data.get("data1"), is("data1Value"));
    assertThat("Unexpected data value.", data.get("data2"), is("data2Value"));
    assertThat("Unexpected data value.", data.get("data3"), is("data3Value"));
    assertThat("Unexpected data value.", data.get("data4"), is("data4Value"));

  }
}

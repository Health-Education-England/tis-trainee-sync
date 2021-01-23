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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.nhs.hee.tis.trainee.sync.model.Placement;

class RecordWriteConverterTest {

  private RecordWriteConverter converter;

  @BeforeEach
  void setUp() {
    converter = new RecordWriteConverter();
  }

  @Test
  void shouldConvertToDocument() {
    Placement record = new Placement();
    record.setTisId("idValue");

    Map<String, String> data = new HashMap<>();
    data.put("data1", "data1Value");
    data.put("data2", "data2Value");
    data.put("data3", "data3Value");
    data.put("data4", "data4Value");
    record.setData(data);

    Document document = converter.convert(record);

    assertThat("Unexpected identifier.", document.getString("_id"), is("idValue"));
    assertThat("Unexpected class.", document.getString("_class"), is(Placement.class.getName()));
    assertThat("Unexpected data value.", document.getString("data1"), is("data1Value"));
    assertThat("Unexpected data value.", document.getString("data2"), is("data2Value"));
    assertThat("Unexpected data value.", document.getString("data3"), is("data3Value"));
    assertThat("Unexpected data value.", document.getString("data4"), is("data4Value"));
  }
}

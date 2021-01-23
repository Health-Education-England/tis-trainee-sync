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

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import uk.nhs.hee.tis.trainee.sync.model.Record;

class MongoConfigurationTest {

  private MongoConfiguration configuration;

  @BeforeEach
  void setUp() {
    RecordWriteConverter writeConverter = new RecordWriteConverter();
    RecordReadGenericConverter readConverter = new RecordReadGenericConverter(null);
    configuration = new MongoConfiguration(writeConverter, readConverter);
  }

  @Test
  void shouldRegisterDocumentToRecordReadConverter() {
    MongoCustomConversions mongoCustomConversions = configuration.mongoCustomConversions();

    boolean registered = mongoCustomConversions.hasCustomReadTarget(Document.class, Record.class);
    assertThat("Custom conversion not registered.", registered, is(true));
  }

  @Test
  void shouldRegisterRecordToDocumentWriteConverter() {
    MongoCustomConversions mongoCustomConversions = configuration.mongoCustomConversions();

    boolean registered = mongoCustomConversions.hasCustomWriteTarget(Record.class, Document.class);
    assertThat("Custom conversion not registered.", registered, is(true));
  }
}

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

package uk.nhs.hee.tis.trainee.sync;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import io.awspring.cloud.autoconfigure.messaging.SqsAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import uk.nhs.hee.tis.trainee.sync.config.MongoConfiguration;

@SpringBootTest(properties = { "cloud.aws.region.static=eu-west-2" })
@ActiveProfiles("int")
@EnableAutoConfiguration(exclude = SqsAutoConfiguration.class)
class TisTraineeSyncApplicationTest {

  @MockBean
  private MongoConfiguration mongoConfiguration; // Mocked as it cannot be loaded without mongo.

  @MockBean
  private AmazonSQSAsync amazonSqsAsync;

  @Autowired
  ApplicationContext context;

  @Test
  void contextLoads() {
    assertThat("Unexpected bean.", context.getBean(MongoConfiguration.class),
        is(mongoConfiguration));
    assertThat("Unexpected bean.", context.getBean(AmazonSQSAsync.class), is(amazonSqsAsync));
  }
}

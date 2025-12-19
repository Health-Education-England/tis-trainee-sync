/*
 * The MIT License (MIT)
 *
 *  Copyright 2025 Crown Copyright (Health Education England)
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 *  associated documentation files (the "Software"), to deal in the Software without restriction,
 *  including without limitation the rights to use, copy, modify, merge, publish, distribute,
 *  sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all copies or
 *  substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 *  NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 *  DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package uk.nhs.hee.tis.trainee.sync.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

import io.awspring.cloud.sqs.operations.SqsReceiveOptions;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.Message;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.nhs.hee.tis.trainee.sync.DockerImageNames;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class FifoMessagingServiceIntegrationTest {

  private static final String QUEUE_NAME = "test-fifo-queue.fifo";

  // this is poor, but apparently there is no way to retrieve these constants from the SQS library
  private static final String MESSAGE_GROUP_ID_HEADER = "Sqs_Msa_MessageGroupId";
  private static final String MESSAGE_DEDUPLICATION_ID_HEADER = "Sqs_Msa_MessageDeduplicationId";

  @Container
  private static final LocalStackContainer localstack = new LocalStackContainer(
      DockerImageNames.LOCALSTACK)
      .withServices(SQS);

  @DynamicPropertySource
  private static void overrideProperties(DynamicPropertyRegistry registry) {

    registry.add("spring.cloud.aws.region.static", localstack::getRegion);
    registry.add("spring.cloud.aws.credentials.access-key", localstack::getAccessKey);
    registry.add("spring.cloud.aws.credentials.secret-key", localstack::getSecretKey);
    registry.add("spring.cloud.aws.sqs.endpoint",
        () -> localstack.getEndpointOverride(SQS).toString());
    registry.add("spring.cloud.aws.sqs.enabled", () -> true);
  }

  @Autowired
  private SqsTemplate sqsTemplate;

  @Autowired
  FifoMessagingService publisher;

  private static String queueUrl;

  @BeforeAll
  static void setUpBeforeAll() throws IOException, InterruptedException {
    localstack.execInContainer(
        "awslocal", "sqs", "create-queue", "--queue-name", QUEUE_NAME,
        "--attributes", "FifoQueue=true,ContentBasedDeduplication=false");

    String queueUrlOutput = localstack.execInContainer(
        "awslocal", "sqs", "get-queue-url", "--queue-name", QUEUE_NAME).getStdout();

    queueUrl = queueUrlOutput.split(":\\s*\"")[1].split("\"")[0];
  }

  @Test
  void shouldSendMessageWithMessageGroupId() {
    String testPayload = "Test message";
    publisher.sendMessageToFifoQueue(queueUrl, testPayload);

    Consumer<SqsReceiveOptions> optionsConsumer =
        opts -> {
          opts.queue(queueUrl);
          opts.maxNumberOfMessages(1);
          opts.pollTimeout(Duration.of(1, ChronoUnit.SECONDS));
        };

    Optional<Message<String>> msg = sqsTemplate.receive(optionsConsumer, String.class);

    assertThat("Expected message to be present.", msg.isPresent(), is(true));
    assertThat("Unexpected message body.", msg.get().getPayload().equals(testPayload), is(true));
    assertThat("Unexpected missing message group id.",
        msg.get().getHeaders().get(MESSAGE_GROUP_ID_HEADER), notNullValue());
  }

  @Test
  void shouldSendMessageWithMessageGroupIdAndDeduplicationId() {
    String testPayload = "Test message";
    String deduplicationId = "test-deduplication-id-12345";
    publisher.sendMessageToFifoQueue(queueUrl, testPayload, deduplicationId);

    Consumer<SqsReceiveOptions> optionsConsumer =
        opts -> {
          opts.queue(queueUrl);
          opts.maxNumberOfMessages(1);
          opts.pollTimeout(Duration.of(1, ChronoUnit.SECONDS));
        };

    Optional<Message<String>> msg = sqsTemplate.receive(optionsConsumer, String.class);

    assertThat("Expected message to be present.", msg.isPresent(), is(true));
    assertThat("Unexpected message body.", msg.get().getPayload().equals(testPayload), is(true));
    assertThat("Unexpected missing message group id.",
        msg.get().getHeaders().get(MESSAGE_GROUP_ID_HEADER), notNullValue());
    assertThat("Unexpected missing message deduplication id.",
        msg.get().getHeaders().get(MESSAGE_DEDUPLICATION_ID_HEADER), notNullValue());
  }
}

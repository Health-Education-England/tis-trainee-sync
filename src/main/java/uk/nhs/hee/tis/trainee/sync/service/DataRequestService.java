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

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DataRequestService {

  private AmazonSQS amazonSqs;

  private ObjectMapper objectMapper;

  private String queueUrl;

  /**
   * A service that sends messages into a queue.
   * @param amazonSqs    An AmazonSqs object.
   * @param objectMapper A tool to construct a Json string.
   * @param queueUrl     The url of the queue.
   */
  public DataRequestService(AmazonSQS amazonSqs,
                            ObjectMapper objectMapper,
                            @Value("${application.aws.sqs.queueUrl}") String queueUrl) {
    this.amazonSqs = amazonSqs;
    this.objectMapper = objectMapper;
    this.queueUrl = queueUrl;
  }

  public void sendRequest(String tableName, String id) throws JsonProcessingException {
    String messageBody = makeJson(tableName, id);
    SendMessageRequest sendMessageRequest = new SendMessageRequest()
        .withQueueUrl(queueUrl)
        .withMessageBody(messageBody);

    amazonSqs.sendMessage(sendMessageRequest);
  }

  private String makeJson(String tableName, String id) throws JsonProcessingException {
    ObjectNode rootNode = objectMapper.createObjectNode();
    rootNode.put("table", tableName);
    rootNode.put("id", id);
    return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
  }
}

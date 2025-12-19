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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DataRequestService {

  protected static final String DEFAULT_SCHEMA = "tcs";

  private SqsTemplate messagingTemplate;

  private ObjectMapper objectMapper;

  private String queueUrl;

  /**
   * A service that sends messages into a queue.
   *
   * @param messagingTemplate The messaging template to use.
   * @param objectMapper      A tool to construct a Json string.
   * @param queueUrl          The url of the queue.
   */
  public DataRequestService(SqsTemplate messagingTemplate, ObjectMapper objectMapper,
      @Value("${application.aws.sqs.request}") String queueUrl) {
    this.messagingTemplate = messagingTemplate;
    this.objectMapper = objectMapper;
    this.queueUrl = queueUrl;
  }

  /**
   * Send a request about a specific entry using key-value pairs and the appropriate message group
   * id to ensure the correct ordering of related requests.
   *
   * @param schema    The schema to which the table belongs.
   * @param tableName The name of the table whose requested data belong to.
   * @param whereMap  The key-value map defining the requested table entry.
   * @return the message that was sent
   * @throws JsonProcessingException Exception thrown when error occurs.
   */
  public String sendRequest(String schema, String tableName, Map<String, String> whereMap)
      throws JsonProcessingException {
    String messageBody = makeJson(tableName, whereMap);

    String tisId = whereMap.values().toArray()[0].toString();
    //note: ordering cannot be guaranteed, but only a single value map is ever provided except for
    //the exception PlacementSpecialty handled below.
    //All data requests are for primary (parent) table records, so they can be left as-is,
    //except for a PlacementSpecialty request which fortunately has the parent placement's id.
    if (tableName.equalsIgnoreCase("PlacementSpecialty")) {
      tableName = "Placement";
      tisId = whereMap.get("placementId");
    }
    String messageGroupId = String.format("%s_%s_%s", schema, tableName, tisId);

    log.info("Sending SQS message with body: [{}] and message group id '{}'", messageBody,
        messageGroupId);

    messagingTemplate.send(to -> to
        .queue(queueUrl)
        .payload(messageBody)
        .messageGroupId(messageGroupId));

    return messageBody;
  }

  /**
   * Send a request about a specific entry using key-value pairs and a default schema message group
   * id.
   *
   * @param tableName The name of the table whose requested data belong to.
   * @param whereMap  The key-value map defining the requested table entry.
   * @return the message that was sent
   * @throws JsonProcessingException Exception thrown when error occurs.
   */
  public String sendRequest(String tableName, Map<String, String> whereMap)
      throws JsonProcessingException {
    return sendRequest(DEFAULT_SCHEMA, tableName, whereMap);
  }

  /**
   * Return a string in Json format representing the request.
   *
   * @param tableName The name of the table whose requested data belong to.
   * @param whereMap  The key-value map defining the requested table entry.
   * @return A string in json format.
   * @throws JsonProcessingException Exception thrown when error occurs.
   */
  private String makeJson(String tableName, Map<String, String> whereMap)
      throws JsonProcessingException {
    ObjectNode rootNode = objectMapper.createObjectNode();
    rootNode.put("table", tableName);
    whereMap.forEach(rootNode::put);
    return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
  }
}

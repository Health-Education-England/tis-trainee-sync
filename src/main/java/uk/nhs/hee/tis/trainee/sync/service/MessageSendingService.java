package uk.nhs.hee.tis.trainee.sync.service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MessageSendingService {

  private AmazonSQS amazonSqs;

  private ObjectMapper objectMapper;

  private String queueName;

  /**
   * A service that sends messages into a queue.
   * @param amazonSQS An AmazonSqs object.
   * @param objectMapper A tool to construct a Json string.
   * @param queueName The name of the queue.
   */
  public MessageSendingService(AmazonSQS amazonSQS,
                               ObjectMapper objectMapper,
                               @Value("${application.aws.sqs.queueName}") String queueName) {
    this.amazonSqs = amazonSQS;
    this.objectMapper = objectMapper;
    this.queueName = queueName;
  }

  public void sendMessage(String tableName, String id) throws JsonProcessingException {
    String messageBody = makeJson(tableName, id);

    String queueUrl = amazonSqs.getQueueUrl(queueName).getQueueUrl();
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

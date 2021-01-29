package uk.nhs.hee.tis.trainee.sync.service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataRequestServiceTest {

  private DataRequestService testObj;

  private AmazonSQS amazonSqsMock;

  private ObjectMapper objectMapper;

  private String queueUrl = "mockQueueUrl";

  @BeforeEach
  public void setUp() {
    amazonSqsMock = mock(AmazonSQS.class);
    objectMapper = new ObjectMapper();
    testObj = new DataRequestService(amazonSqsMock, objectMapper, queueUrl);
  }

  @Test
  void shouldSendARequestViaMessage() throws JsonProcessingException {
    testObj.sendMessage("Post", "10");

    verify(amazonSqsMock).sendMessage(any(SendMessageRequest.class));
  }
}

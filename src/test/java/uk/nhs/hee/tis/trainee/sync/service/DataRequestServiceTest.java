package uk.nhs.hee.tis.trainee.sync.service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataRequestServiceTest {

  private DataRequestService testObj;

  private AmazonSQS amazonSqsMock;

  private String queueUrl = "mockQueueUrl";

  @BeforeEach
  public void setUp() {
    amazonSqsMock = mock(AmazonSQS.class);
    GetQueueUrlResult getQueueUrlResult = mock(GetQueueUrlResult.class);
    testObj = new DataRequestService(amazonSqsMock, new ObjectMapper(), queueUrl);
  }

  @Test
  void shouldSendAMessage() throws JsonProcessingException {
    testObj.sendMessage("Post", "10");

    String expectedMessageBody = "{\r\n" +
        "  \"table\" : \"Post\",\r\n" +
        "  \"id\" : \"10\"\r\n" +
        "}";

    verify(amazonSqsMock).sendMessage(argThat(sendMessageRequest -> {
      assertThat(sendMessageRequest.getMessageBody()).isEqualTo(expectedMessageBody);
      return true;
    }));
  }
}

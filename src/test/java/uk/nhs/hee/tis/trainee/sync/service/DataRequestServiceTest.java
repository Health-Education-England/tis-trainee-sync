package uk.nhs.hee.tis.trainee.sync.service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import uk.nhs.hee.tis.trainee.sync.model.Post;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
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
  void shouldSendAMessage() throws JsonProcessingException, JSONException {
    testObj.sendMessage("Post", "10");

    JSONObject expectedJsonObject = new JSONObject();
    expectedJsonObject.accumulate("table", "Post");
    expectedJsonObject.accumulate("id", "10");


    verify(amazonSqsMock).sendMessage(argThat(sendMessageRequest -> {
      try {
        JSONAssert.assertEquals(sendMessageRequest.getMessageBody(), expectedJsonObject, true);
      } catch (JSONException e) {
        e.printStackTrace();
      }
      return true;
    }));
  }
}

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

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DataRequestServiceTest {

  public static final String ID = "10";

  private DataRequestService testObj;

  private AmazonSQS amazonSqsMock;

  private ObjectMapper objectMapper;

  private final String queueUrl = "mockQueueUrl";

  @BeforeEach
  public void setUp() {
    amazonSqsMock = mock(AmazonSQS.class);
    objectMapper = new ObjectMapper();
    testObj = new DataRequestService(amazonSqsMock, objectMapper, queueUrl);
  }

  @Test
  void shouldSendARequestViaMessage() throws JsonProcessingException {
    Map<String, String> whereMapForAPost = Map.of("id", ID);
    testObj.sendRequest("Post", whereMapForAPost);

    verify(amazonSqsMock).sendMessage(any(SendMessageRequest.class));
  }

  @Test
  void shouldSendAWhereMapRequestViaMessage() throws JsonProcessingException {
    Map<String, String> whereMapForAPlacementSpecialty = Map
        .of("placementId", ID, "placementSpecialtyType", "PRIMARY");
    testObj.sendRequest("PlacementSpecialty", whereMapForAPlacementSpecialty);

    verify(amazonSqsMock).sendMessage(any(SendMessageRequest.class));
  }
}

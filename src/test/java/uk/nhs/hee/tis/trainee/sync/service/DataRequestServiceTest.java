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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static uk.nhs.hee.tis.trainee.sync.service.FifoMessagingService.MESSAGE_GROUP_ID_HEADER;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.Message;

class DataRequestServiceTest {

  public static final String ID = "10";

  private DataRequestService testObj;

  private SqsTemplate queueMessagingTemplate;

  private final String queueUrl = "mockQueueUrl";

  @BeforeEach
  void setUp() {
    queueMessagingTemplate = mock(SqsTemplate.class);
    ObjectMapper objectMapper = new ObjectMapper();
    testObj = new DataRequestService(queueMessagingTemplate, objectMapper, queueUrl);
  }

  @Test
  void shouldSendRequestViaMessage() throws JsonProcessingException {
    Map<String, String> whereMapForPost = Map.of("id", ID);
    testObj.sendRequest("Post", whereMapForPost);

    ArgumentCaptor<Message<String>> messageCaptor = ArgumentCaptor.captor();
    verify(queueMessagingTemplate).send(eq(queueUrl), messageCaptor.capture());

    Message<String> message = messageCaptor.getValue();

    String payload = message.getPayload();
    assertThat("Unexpected message.", payload, notNullValue());
    assertThat("Unexpected table.", payload, containsString("\"table\" : \"Post\""));
    assertThat("Unexpected id.", payload, containsString("\"id\" : \"10\""));

    Map<String, Object> headers = message.getHeaders();
    assertThat("Unexpected headers key.", headers.containsKey(MESSAGE_GROUP_ID_HEADER), is(true));
    String expectedMessageGroupId = String.format("%s_%s_%s", "tcs", "Post", ID);
    assertThat("Unexpected message group id value.", headers.get(MESSAGE_GROUP_ID_HEADER),
        is(expectedMessageGroupId));
  }

  @Test
  void shouldSendWhereMapRequestViaMessage() throws JsonProcessingException {
    Map<String, String> whereMapForPlacementSpecialty = Map
        .of("placementId", ID, "placementSpecialtyType", "PRIMARY");
    testObj.sendRequest("PlacementSpecialty", whereMapForPlacementSpecialty);

    ArgumentCaptor<Message<String>> messageCaptor = ArgumentCaptor.captor();
    verify(queueMessagingTemplate).send(eq(queueUrl), messageCaptor.capture());

    Message<String> message = messageCaptor.getValue();

    String payload = message.getPayload();
    assertThat("Unexpected message.", payload, notNullValue());
    assertThat("Unexpected table.", payload, containsString("\"table\" : \"PlacementSpecialty\""));
    assertThat("Unexpected placement id.", payload, containsString("\"placementId\" : \"10\""));
    assertThat("Unexpected placement type.", payload,
        containsString("\"placementSpecialtyType\" : \"PRIMARY\""));

    Map<String, Object> headers = message.getHeaders();
    assertThat("Unexpected headers key.", headers.containsKey(MESSAGE_GROUP_ID_HEADER), is(true));
    String expectedMessageGroupId = String.format("%s_%s_%s", "tcs", "Placement", ID);
    //note, Placement, not PlacementSpecialty
    assertThat("Unexpected message group id value.", headers.get(MESSAGE_GROUP_ID_HEADER),
        is(expectedMessageGroupId));
  }
}
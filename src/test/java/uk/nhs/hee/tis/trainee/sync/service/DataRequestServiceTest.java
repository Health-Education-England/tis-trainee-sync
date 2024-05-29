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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DataRequestServiceTest {

  public static final String ID = "10";

  private DataRequestService testObj;

  private QueueMessagingTemplate queueMessagingTemplate;

  private final String queueUrl = "mockQueueUrl";

  @BeforeEach
  void setUp() {
    queueMessagingTemplate = mock(QueueMessagingTemplate.class);
    ObjectMapper objectMapper = new ObjectMapper();
    testObj = new DataRequestService(queueMessagingTemplate, objectMapper, queueUrl);
  }

  @Test
  void shouldSendRequestViaMessage() throws JsonProcessingException {
    Map<String, String> whereMapForPost = Map.of("id", ID);
    testObj.sendRequest("Post", whereMapForPost);

    ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Map<String, Object>> headersCaptor = ArgumentCaptor.forClass(Map.class);
    verify(queueMessagingTemplate).convertAndSend(eq(queueUrl), stringCaptor.capture(),
        headersCaptor.capture());

    String message = stringCaptor.getValue();
    assertThat("Unexpected message.", message, notNullValue());
    assertThat("Unexpected table.", message, containsString("\"table\" : \"Post\""));
    assertThat("Unexpected id.", message, containsString("\"id\" : \"10\""));

    Map<String, Object> headers = headersCaptor.getValue();
    assertThat("Unexpected headers key.", headers.containsKey("message-group-id"), is(true));
    String expectedMessageGroupId = String.format("%s_%s_%s", "tcs", "Post", ID);
    assertThat("Unexpected message group id value.",
        headers.get("message-group-id").toString(), is(expectedMessageGroupId));
  }

  @Test
  void shouldSendWhereMapRequestViaMessage() throws JsonProcessingException {
    Map<String, String> whereMapForPlacementSpecialty = Map
        .of("placementId", ID, "placementSpecialtyType", "PRIMARY");
    testObj.sendRequest("PlacementSpecialty", whereMapForPlacementSpecialty);

    ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Map<String, Object>> headersCaptor = ArgumentCaptor.forClass(Map.class);
    verify(queueMessagingTemplate).convertAndSend(eq(queueUrl), stringCaptor.capture(),
        headersCaptor.capture());

    String message = stringCaptor.getValue();
    assertThat("Unexpected message.", message, notNullValue());
    assertThat("Unexpected table.", message, containsString("\"table\" : \"PlacementSpecialty\""));
    assertThat("Unexpected placement id.", message, containsString("\"placementId\" : \"10\""));
    assertThat("Unexpected placement type.", message,
        containsString("\"placementSpecialtyType\" : \"PRIMARY\""));

    Map<String, Object> headers = headersCaptor.getValue();
    assertThat("Unexpected headers key.", headers.containsKey("message-group-id"), is(true));
    String expectedMessageGroupId = String.format("%s_%s_%s", "tcs", "Placement", ID);
    //note, Placement, not PlacementSpecialty
    assertThat("Unexpected message group id value.",
        headers.get("message-group-id").toString(), is(expectedMessageGroupId));
  }
}

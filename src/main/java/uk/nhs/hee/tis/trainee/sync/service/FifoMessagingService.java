/*
 * The MIT License (MIT)
 *
 *  Copyright 2024 Crown Copyright (Health Education England)
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

import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.trainee.sync.model.Record;

@Service
@Slf4j
public class FifoMessagingService {

  private final QueueMessagingTemplate messagingTemplate;

  public FifoMessagingService(QueueMessagingTemplate messagingTemplate) {
    this.messagingTemplate = messagingTemplate;
  }

  public void sendMessageToFifoQueue(String queueUrl, Object toSend) {
    Map<String, Object> headers = new HashMap<>();
    String messageGroupId = getMessageGroupId(toSend);
    headers.put("message-group-id", messageGroupId);

    messagingTemplate.convertAndSend(queueUrl, toSend, headers);
  }

  protected String getMessageGroupId(Object toSend) {
    String id = "";
    Class<?> toSendClass = toSend.getClass();
    if (toSendClass.getSimpleName().equalsIgnoreCase("Record")
    || Record.class.isAssignableFrom(toSendClass)) {
      Record aRecord = (Record) toSend;
      id = switch (aRecord.getTable()) {
        case "ConditionsOfJoining", "CurriculumMembership" ->
            aRecord.getData().get("programmeMembershipUuid");
        case "PlacementSite", "PlacementSpecialty" -> aRecord.getData().get("placementId");
        case "PostSpecialty" -> aRecord.getData().get("postId");
        case "ProgrammeMembership" -> aRecord.getData().get("uuid");
        case "Qualification" -> aRecord.getData().get("personId");
        default -> aRecord.getTisId();
      };
      return String.format("%s_%s_%s", aRecord.getSchema(), aRecord.getTable(), id);
    } else {
      String table = toSendClass.getSimpleName();
      try {
        Field idField = switch (table) {
          case "ConditionsOfJoining" -> toSendClass.getDeclaredField("programmeMembershipUuid");
          case "ProgrammeMembership" -> toSendClass.getDeclaredField("uuid");
          default -> toSendClass.getDeclaredField("id"); //should only catch PlacementSite
        };
        idField.setAccessible(true);
        id = (String) idField.get(toSend);
      } catch (Exception e) {
        //should not happen
        log.error("Expected id field missing: {}", toSend);
      }
      return String.format("%s_%s_%s", "tcs", table, id);
    }
  }
}

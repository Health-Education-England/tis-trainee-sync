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
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.trainee.sync.model.Record;

@Service
@Slf4j
public class FifoMessagingService {

  private final QueueMessagingTemplate messagingTemplate;

  private static final String PROGRAMME_MEMBERSHIP_TABLE = "ProgrammeMembership";
  private static final String MESSAGE_GROUP_ID_FORMAT = "%s_%s_%s";

  public FifoMessagingService(QueueMessagingTemplate messagingTemplate) {
    this.messagingTemplate = messagingTemplate;
  }

  /**
   * Send a message to a FIFO queue with a Message Group Id header. No message deduplication value
   * is included.
   *
   * @param queueUrl The message queue URL.
   * @param toSend   The object to send.
   */
  public void sendMessageToFifoQueue(String queueUrl, Object toSend) {
    Map<String, Object> headers = new HashMap<>();
    String messageGroupId = getMessageGroupId(toSend);
    headers.put("message-group-id", messageGroupId);

    messagingTemplate.convertAndSend(queueUrl, toSend, headers);
  }

  /**
   * Get a properly formatted Message Group Id for an object, following the conventions on using the
   * 'primary' object Id where possible.
   *
   * @param toSend The object that will be sent in the message.
   * @return The Message Group Id, formatted as schema_table_id
   */
  protected String getMessageGroupId(Object toSend) {
    Class<?> toSendClass = toSend.getClass();
    if (toSendClass.getSimpleName().equalsIgnoreCase("Record")
        || Record.class.isAssignableFrom(toSendClass)) {
      Record aRecord = (Record) toSend;
      Pair<String, String> groupTableAndId = switch (aRecord.getTable()) {
        case "ConditionsOfJoining",
            "CurriculumMembership"
            -> Pair.of(PROGRAMME_MEMBERSHIP_TABLE, aRecord.getData().get("programmeMembershipUuid"));
        case "PlacementSite",
            "PlacementSpecialty"
            -> Pair.of("Placement", aRecord.getData().get("placementId"));
        case "PostSpecialty"
            -> Pair.of("Post", aRecord.getData().get("postId"));
        case PROGRAMME_MEMBERSHIP_TABLE
            -> Pair.of(PROGRAMME_MEMBERSHIP_TABLE, aRecord.getData().get("uuid"));
        case "Qualification"
            -> Pair.of("Person", aRecord.getData().get("personId"));
        default
            -> Pair.of(aRecord.getTable(), aRecord.getTisId());
      };
      return String.format(MESSAGE_GROUP_ID_FORMAT, aRecord.getSchema(), groupTableAndId.getFirst(),
          groupTableAndId.getSecond());
    } else {
      String table = toSendClass.getSimpleName();
      String id = "";
      try {
        Pair<String, Method> groupTableAndIdMethod = switch (table) {
          case "ConditionsOfJoining"
              -> Pair.of(PROGRAMME_MEMBERSHIP_TABLE,
              toSendClass.getDeclaredMethod("getProgrammeMembershipUuid"));
          case PROGRAMME_MEMBERSHIP_TABLE
              -> Pair.of(PROGRAMME_MEMBERSHIP_TABLE,
              toSendClass.getDeclaredMethod("getUuid"));
          case "PlacementSite"
              -> Pair.of("Placement",
              toSendClass.getDeclaredMethod("getPlacementId"));
          default
              -> Pair.of(table,
              toSendClass.getDeclaredMethod("getId")); //should not happen
        };

        id = groupTableAndIdMethod.getSecond().invoke(toSend).toString();
        return String.format(MESSAGE_GROUP_ID_FORMAT, "tcs", groupTableAndIdMethod.getFirst(), id);
      } catch (Exception e) {
        //should not happen
        log.error("Expected id field missing: {}", toSend);
      }
      return String.format(MESSAGE_GROUP_ID_FORMAT, "tcs", table, id);
    }
  }
}

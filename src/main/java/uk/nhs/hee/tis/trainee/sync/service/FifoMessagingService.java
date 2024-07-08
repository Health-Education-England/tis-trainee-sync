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
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.trainee.sync.model.Record;

/**
 * A service for sending messages to a FIFO queue with appropriate message group ids.
 */
@Service
@Slf4j
public class FifoMessagingService {

  private final QueueMessagingTemplate messagingTemplate;

  private static final String PROGRAMME_MEMBERSHIP_TABLE = "ProgrammeMembership";
  private static final String MESSAGE_GROUP_ID_FORMAT = "%s_%s_%s";
  protected static final String DEFAULT_SCHEMA = "tcs";

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
    //String messageGroupId = getMessageGroupId(toSend);
    //headers.put("message-group-id", messageGroupId);

    log.debug("Sending to FIFO queue {} with headers {}: {}", queueUrl, headers, toSend);
    messagingTemplate.convertAndSend(queueUrl, toSend, headers);
  }

  /**
   * Send a message to a FIFO queue with a Message Group Id header and a deduplication value to
   * override content-based deduplication.
   *
   * @param queueUrl        The message queue URL.
   * @param toSend          The object to send.
   * @param deduplicationId The deduplication ID to override default content-based deduplication.
   */
  public void sendMessageToFifoQueue(String queueUrl, Object toSend, String deduplicationId) {
    Map<String, Object> headers = new HashMap<>();
//    String messageGroupId = getMessageGroupId(toSend);
//    headers.put("message-group-id", messageGroupId);
//    headers.put("message-deduplication-id", deduplicationId);

    log.debug("Sending to FIFO queue {} with headers {}: {}", queueUrl, headers, toSend);
    messagingTemplate.convertAndSend(queueUrl, toSend, headers);
  }

  /**
   * Create a unique deduplication id for a particular object.
   *
   * @param objectType The object type.
   * @param id         The object Id.
   * @return The unique deduplication string.
   */
  public String getUniqueDeduplicationId(String objectType, String id) {
    return String.format(MESSAGE_GROUP_ID_FORMAT, objectType, id, Instant.now());
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
      Record theRecord = (Record) toSend;
      Pair<String, String> groupTableAndId = switch (theRecord.getTable()) {
        case "ConditionsOfJoining",
            "CurriculumMembership" -> Pair.of(PROGRAMME_MEMBERSHIP_TABLE,
            theRecord.getData().get("programmeMembershipUuid"));
        case "PlacementSite",
            "PlacementSpecialty" -> Pair.of("Placement", theRecord.getData().get("placementId"));
        case "PostSpecialty" -> Pair.of("Post", theRecord.getData().get("postId"));
        case PROGRAMME_MEMBERSHIP_TABLE ->
            Pair.of(PROGRAMME_MEMBERSHIP_TABLE, theRecord.getData().get("uuid"));
        case "Qualification" -> Pair.of("Person", theRecord.getData().get("personId"));
        default -> Pair.of(theRecord.getTable(), theRecord.getTisId());
      };
      return String.format(MESSAGE_GROUP_ID_FORMAT,
          theRecord.getSchema(), groupTableAndId.getFirst(), groupTableAndId.getSecond());
      //note this assumes the record and its group will always have the same schema
    } else {
      String table = toSendClass.getSimpleName();
      String id = "";
      try {
        Pair<String, Method> groupTableAndIdMethod = switch (table) {
          case "ConditionsOfJoining" -> Pair.of(PROGRAMME_MEMBERSHIP_TABLE,
              toSendClass.getDeclaredMethod("getProgrammeMembershipUuid"));
          case PROGRAMME_MEMBERSHIP_TABLE -> Pair.of(PROGRAMME_MEMBERSHIP_TABLE,
              toSendClass.getDeclaredMethod("getUuid"));
          case "PlacementSite" -> Pair.of("Placement",
              toSendClass.getDeclaredMethod("getPlacementId"));
          default -> Pair.of(table,
              toSendClass.getDeclaredMethod("getId")); //should not happen
        };

        id = groupTableAndIdMethod.getSecond().invoke(toSend).toString();
        return String.format(MESSAGE_GROUP_ID_FORMAT,
            DEFAULT_SCHEMA, groupTableAndIdMethod.getFirst(), id);
      } catch (Exception e) {
        //should not happen
        log.error("Expected id field missing: {}", toSend);
      }
      return String.format(MESSAGE_GROUP_ID_FORMAT, DEFAULT_SCHEMA, table, id);
    }
  }
}

/*
 * The MIT License (MIT)
 *
 * Copyright 2024 Crown Copyright (Health Education England)
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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import uk.nhs.hee.tis.trainee.sync.model.ConditionsOfJoining;
import uk.nhs.hee.tis.trainee.sync.model.CurriculumMembership;
import uk.nhs.hee.tis.trainee.sync.model.PlacementSite;
import uk.nhs.hee.tis.trainee.sync.model.PlacementSpecialty;
import uk.nhs.hee.tis.trainee.sync.model.Post;
import uk.nhs.hee.tis.trainee.sync.model.PostSpecialty;
import uk.nhs.hee.tis.trainee.sync.model.ProgrammeMembership;
import uk.nhs.hee.tis.trainee.sync.model.Record;

class FifoMessagingServiceTest {

  private static final String QUEUE = "the-queue";
  private static final String TIS_ID = "tis-id";
  private static final String TABLE = "table";
  private static final String SCHEMA = "schema";

  private FifoMessagingService service;
  private QueueMessagingTemplate messagingTemplate;

  @BeforeEach
  void setUp() {
    messagingTemplate = mock(QueueMessagingTemplate.class);
    service = new FifoMessagingService(messagingTemplate);
  }

  @Test
  void shouldConvertAndSendObjectToQueueWithMessageGroupIdHeader() {
    Record theRecord = new Record();
    theRecord.setTisId(TIS_ID);
    theRecord.setTable(TABLE);
    theRecord.setSchema(SCHEMA);

    service.sendMessageToFifoQueue(QUEUE, theRecord);

    ArgumentCaptor<Map<String, Object>> headersCaptor
        = ArgumentCaptor.forClass(Map.class);

    verify(messagingTemplate).convertAndSend(eq(QUEUE), eq(theRecord), headersCaptor.capture());

    Map<String, Object> headers = headersCaptor.getValue();
    assertThat("Message group id header missing.",
        headers.containsKey("message-group-id"), is(true));
  }

  @ParameterizedTest
  @ValueSource(strings = {"ConditionsOfJoining", "CurriculumMembership"})
  void shouldUseProgrammeMembershipForCojOrCmRecordMessageGroupIds(String table) {
    Record theRecord = new Record();
    theRecord.setTisId(TIS_ID);
    theRecord.setData(Map.of("programmeMembershipUuid", "UUID"));
    theRecord.setTable(table);
    theRecord.setSchema(SCHEMA);

    String messageGroupId = service.getMessageGroupId(theRecord);
    String expectedMessageGroupId
        = String.format("%s_%s_%s", SCHEMA, "ProgrammeMembership", "UUID");
    assertThat("Unexpected message group id.", messageGroupId, is(expectedMessageGroupId));
  }

  @ParameterizedTest
  @ValueSource(strings = {"PlacementSite", "PlacementSpecialty"})
  void shouldUsePlacementForPlacementSiteOrSpecialtyRecordMessageGroupIds(String table) {
    Record theRecord = new Record();
    theRecord.setTisId(TIS_ID);
    theRecord.setData(Map.of("placementId", "ID"));
    theRecord.setTable(table);
    theRecord.setSchema(SCHEMA);

    String messageGroupId = service.getMessageGroupId(theRecord);
    String expectedMessageGroupId = String.format("%s_%s_%s", SCHEMA, "Placement", "ID");
    assertThat("Unexpected message group id.", messageGroupId, is(expectedMessageGroupId));
  }

  @ParameterizedTest
  @ValueSource(strings = {"PostSpecialty"})
  void shouldUsePostForPostSpecialtyRecordMessageGroupIds(String table) {
    Record theRecord = new Record();
    theRecord.setTisId(TIS_ID);
    theRecord.setData(Map.of("postId", "ID"));
    theRecord.setTable(table);
    theRecord.setSchema(SCHEMA);

    String messageGroupId = service.getMessageGroupId(theRecord);
    String expectedMessageGroupId = String.format("%s_%s_%s", SCHEMA, "Post", "ID");
    assertThat("Unexpected message group id.", messageGroupId, is(expectedMessageGroupId));
  }

  @ParameterizedTest
  @ValueSource(strings = {"Qualification"})
  void shouldUsePersonForQualificationRecordMessageGroupIds(String table) {
    Record theRecord = new Record();
    theRecord.setTisId(TIS_ID);
    theRecord.setData(Map.of("personId", "ID"));
    theRecord.setTable(table);
    theRecord.setSchema(SCHEMA);

    String messageGroupId = service.getMessageGroupId(theRecord);
    String expectedMessageGroupId = String.format("%s_%s_%s", SCHEMA, "Person", "ID");
    assertThat("Unexpected message group id.", messageGroupId, is(expectedMessageGroupId));
  }

  @ParameterizedTest
  @ValueSource(strings = {"ProgrammeMembership"})
  void shouldUseProgrammeMembershipUuidForRecordMessageGroupIds(String table) {
    Record theRecord = new Record();
    theRecord.setTisId(TIS_ID);
    theRecord.setData(Map.of("uuid", "UUID"));
    theRecord.setTable(table);
    theRecord.setSchema(SCHEMA);

    String messageGroupId = service.getMessageGroupId(theRecord);
    String expectedMessageGroupId = String.format("%s_%s_%s", SCHEMA, table, "UUID");
    assertThat("Unexpected message group id.", messageGroupId, is(expectedMessageGroupId));
  }

  @Test
  void shouldUseTableAndIdForOtherRecordMessageGroupIds() {
    Record theRecord = new Record();
    theRecord.setTisId(TIS_ID);
    theRecord.setTable("someTable");
    theRecord.setSchema(SCHEMA);

    String messageGroupId = service.getMessageGroupId(theRecord);
    String expectedMessageGroupId = String.format("%s_%s_%s", SCHEMA, "someTable", TIS_ID);
    assertThat("Unexpected message group id.", messageGroupId, is(expectedMessageGroupId));
  }

  @Test
  void shouldUseTableAndIdForOtherMessageGroupIds() {
    Post post = new Post(); //for example
    post.setTisId(TIS_ID);
    post.setTable(TABLE);
    post.setSchema(SCHEMA);

    String messageGroupId = service.getMessageGroupId(post);
    String expectedMessageGroupId = String.format("%s_%s_%s", SCHEMA, TABLE, TIS_ID);
    assertThat("Unexpected message group id.", messageGroupId, is(expectedMessageGroupId));
  }

  @Test
  void shouldUseProgrammeMembershipForCurriculumMembershipMessageGroupIds() {
    CurriculumMembership curriculumMembership = new CurriculumMembership();
    curriculumMembership.setTisId(TIS_ID);
    curriculumMembership.setSchema(SCHEMA);
    curriculumMembership.setTable("CurriculumMembership");
    curriculumMembership.setData(Map.of("programmeMembershipUuid", "UUID"));

    String messageGroupId = service.getMessageGroupId(curriculumMembership);
    String expectedMessageGroupId
        = String.format("%s_%s_%s", SCHEMA, "ProgrammeMembership", "UUID");
    assertThat("Unexpected message group id.", messageGroupId, is(expectedMessageGroupId));
  }

  @Test
  void shouldUsePlacementForPlacementSpecialtyMessageGroupIds() {
    PlacementSpecialty placementSpecialty = new PlacementSpecialty();
    placementSpecialty.setTisId(TIS_ID);
    placementSpecialty.setSchema(SCHEMA);
    placementSpecialty.setTable("PlacementSpecialty");
    placementSpecialty.setData(Map.of("placementId", "ID"));

    String messageGroupId = service.getMessageGroupId(placementSpecialty);
    String expectedMessageGroupId = String.format("%s_%s_%s", SCHEMA, "Placement", "ID");
    assertThat("Unexpected message group id.", messageGroupId, is(expectedMessageGroupId));
  }

  @Test
  void shouldUsePostForPostSpecialtyMessageGroupIds() {
    PostSpecialty postSpecialty = new PostSpecialty();
    postSpecialty.setTisId(TIS_ID);
    postSpecialty.setSchema(SCHEMA);
    postSpecialty.setTable("PostSpecialty");
    postSpecialty.setData(Map.of("postId", "ID"));

    String messageGroupId = service.getMessageGroupId(postSpecialty);
    String expectedMessageGroupId = String.format("%s_%s_%s", SCHEMA, "Post", "ID");
    assertThat("Unexpected message group id.", messageGroupId, is(expectedMessageGroupId));
  }

  @Test
  void shouldUseProgrammeMembershipForConditionsOfJoiningMessageGroupIds() {
    ConditionsOfJoining conditionsOfJoining = new ConditionsOfJoining();
    conditionsOfJoining.setProgrammeMembershipUuid("UUID");

    String messageGroupId = service.getMessageGroupId(conditionsOfJoining);
    String expectedMessageGroupId = String.format("%s_%s_%s", "tcs", "ProgrammeMembership", "UUID");
    assertThat("Unexpected message group id.", messageGroupId, is(expectedMessageGroupId));
  }

  @Test
  void shouldUsePlacementForPlacementSiteMessageGroupIds() {
    PlacementSite placementSite = new PlacementSite();
    placementSite.setPlacementId(123L);
    placementSite.setId(234L);

    String messageGroupId = service.getMessageGroupId(placementSite);
    String expectedMessageGroupId = String.format("%s_%s_%s", "tcs", "Placement", "123");
    assertThat("Unexpected message group id.", messageGroupId, is(expectedMessageGroupId));
  }

  @Test
  void shouldUseUuidForProgrammeMembershipMessageGroupIds() {
    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    UUID uuid = UUID.randomUUID();
    programmeMembership.setUuid(uuid);

    String messageGroupId = service.getMessageGroupId(programmeMembership);
    String expectedMessageGroupId = String.format("%s_%s_%s", "tcs", "ProgrammeMembership", uuid);
    assertThat("Unexpected message group id.", messageGroupId, is(expectedMessageGroupId));
  }

  @Test
  void shouldUseDefaultIdForOtherClassesMessageGroupIds() {
    class OtherClass {
      public Long getId() {
        return 1L;
      }
    }

    OtherClass otherClass = new OtherClass();

    String messageGroupId = service.getMessageGroupId(otherClass);
    String expectedMessageGroupId = String.format("%s_%s_%s", "tcs", "OtherClass", "1");
    assertThat("Unexpected message group id.", messageGroupId, is(expectedMessageGroupId));
  }

  @Test
  void shouldFailGracefullyOnUnexpectedInput() {
    class UnexpectedClass {
      String anAttribute;

      public void setAnAttribute(String value) {
        this.anAttribute = value;
      }
    }

    UnexpectedClass unexpectedClass = new UnexpectedClass();
    unexpectedClass.setAnAttribute("a value");

    String messageGroupId = service.getMessageGroupId(unexpectedClass);
    String expectedMessageGroupId = String.format("%s_%s_%s", "tcs", "UnexpectedClass", "");
    assertThat("Unexpected message group id.", messageGroupId, is(expectedMessageGroupId));
  }

}

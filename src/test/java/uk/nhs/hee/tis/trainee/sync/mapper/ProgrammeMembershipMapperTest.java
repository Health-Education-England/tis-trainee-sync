/*
 * The MIT License (MIT)
 *
 * Copyright 2023 Crown Copyright (Health Education England)
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

package uk.nhs.hee.tis.trainee.sync.mapper;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.nhs.hee.tis.trainee.sync.model.ProgrammeMembership;
import uk.nhs.hee.tis.trainee.sync.model.Record;

class ProgrammeMembershipMapperTest {

  private static final String UUID_FIELD = "uuid";
  private static final UUID UUID_VALUE = UUID.randomUUID();
  private static final String PM_TYPE_FIELD = "programmeMembershipType";
  private static final String PM_TYPE_VALUE = "pmType1";
  private static final String PROGRAMME_START_DATE_FIELD = "programmeStartDate";
  private static final LocalDate PROGRAMME_START_DATE_VALUE = LocalDate.MIN;
  private static final String PROGRAMME_END_DATE_FIELD = "programmeEndDate";
  private static final LocalDate PROGRAMME_END_DATE_VALUE = LocalDate.MAX;
  private static final String PROGRAMME_ID_FIELD = "programmeId";
  private static final long PROGRAMME_ID_VALUE = 2;
  private static final String TRAINING_NUMBER_ID_FIELD = "trainingNumberId";
  private static final long TRAINING_NUMBER_ID_VALUE = 3;
  private static final String PERSON_ID_FIELD = "personId";
  private static final long PERSON_ID_VALUE = 4;
  private static final String ROTATION_FIELD = "rotation";
  private static final String ROTATION_VALUE = "rotation5";
  private static final String ROTATION_ID_FIELD = "rotationId";
  private static final long ROTATION_ID_VALUE = 6;
  private static final String TRAINING_PATHWAY_FIELD = "trainingPathway";
  private static final String TRAINING_PATHWAY_VALUE = "trainingPathway7";
  private static final String LEAVING_REASON_FIELD = "leavingReason";
  private static final String LEAVING_REASON_VALUE = "leavingReason8";
  private static final String LEAVING_DESTINATION_FIELD = "leavingDestination";
  private static final String LEAVING_DESTINATION_VALUE = "leavingDestination9";
  private static final String AMENDED_DATE_FIELD = "amendedDate";
  private static final Instant AMENDED_DATE_VALUE = Instant.EPOCH;

  private ProgrammeMembershipMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new ProgrammeMembershipMapperImpl();
  }

  @Test
  void shouldMapPopulatedRecordToProgrammeMembership() {
    Map<String, String> recordData = Map.ofEntries(
        Map.entry(UUID_FIELD, UUID_VALUE.toString()),
        Map.entry(PM_TYPE_FIELD, PM_TYPE_VALUE),
        Map.entry(PROGRAMME_START_DATE_FIELD, PROGRAMME_START_DATE_VALUE.toString()),
        Map.entry(PROGRAMME_END_DATE_FIELD, PROGRAMME_END_DATE_VALUE.toString()),
        Map.entry(PROGRAMME_ID_FIELD, String.valueOf(PROGRAMME_ID_VALUE)),
        Map.entry(TRAINING_NUMBER_ID_FIELD, String.valueOf(TRAINING_NUMBER_ID_VALUE)),
        Map.entry(PERSON_ID_FIELD, String.valueOf(PERSON_ID_VALUE)),
        Map.entry(ROTATION_FIELD, ROTATION_VALUE),
        Map.entry(ROTATION_ID_FIELD, String.valueOf(ROTATION_ID_VALUE)),
        Map.entry(TRAINING_PATHWAY_FIELD, TRAINING_PATHWAY_VALUE),
        Map.entry(LEAVING_REASON_FIELD, LEAVING_REASON_VALUE),
        Map.entry(LEAVING_DESTINATION_FIELD, LEAVING_DESTINATION_VALUE),
        Map.entry(AMENDED_DATE_FIELD, AMENDED_DATE_VALUE.toString())
    );

    ProgrammeMembership programmeMembership = mapper.toEntity(recordData);

    assertThat("Unexpected uuid.", programmeMembership.getUuid(), is(UUID_VALUE));
    assertThat("Unexpected PM type.", programmeMembership.getProgrammeMembershipType(),
        is(PM_TYPE_VALUE));
    assertThat("Unexpected programme start date.", programmeMembership.getProgrammeStartDate(),
        is(PROGRAMME_START_DATE_VALUE));
    assertThat("Unexpected programme end date.", programmeMembership.getProgrammeEndDate(),
        is(PROGRAMME_END_DATE_VALUE));
    assertThat("Unexpected programme ID.", programmeMembership.getProgrammeId(),
        is(PROGRAMME_ID_VALUE));
    assertThat("Unexpected training number ID.", programmeMembership.getTrainingNumberId(),
        is(TRAINING_NUMBER_ID_VALUE));
    assertThat("Unexpected person ID.", programmeMembership.getPersonId(), is(PERSON_ID_VALUE));
    assertThat("Unexpected rotation.", programmeMembership.getRotation(), is(ROTATION_VALUE));
    assertThat("Unexpected rotation ID.", programmeMembership.getRotationId(),
        is(ROTATION_ID_VALUE));
    assertThat("Unexpected training pathway.", programmeMembership.getTrainingPathway(),
        is(TRAINING_PATHWAY_VALUE));
    assertThat("Unexpected leaving reason.", programmeMembership.getLeavingReason(),
        is(LEAVING_REASON_VALUE));
    assertThat("Unexpected leaving destination.", programmeMembership.getLeavingDestination(),
        is(LEAVING_DESTINATION_VALUE));
    assertThat("Unexpected amended date.", programmeMembership.getAmendedDate(),
        is(AMENDED_DATE_VALUE));
  }

  @Test
  void shouldMapNullRecordToProgrammeMembership() {
    Map<String, String> recordData = new HashMap<>();
    recordData.put(UUID_FIELD, null);
    recordData.put(PM_TYPE_FIELD, null);
    recordData.put(PROGRAMME_START_DATE_FIELD, null);
    recordData.put(PROGRAMME_END_DATE_FIELD, null);
    recordData.put(PROGRAMME_ID_FIELD, null);
    recordData.put(TRAINING_NUMBER_ID_FIELD, null);
    recordData.put(PERSON_ID_FIELD, null);
    recordData.put(ROTATION_FIELD, null);
    recordData.put(ROTATION_ID_FIELD, null);
    recordData.put(TRAINING_PATHWAY_FIELD, null);
    recordData.put(LEAVING_REASON_FIELD, null);
    recordData.put(LEAVING_DESTINATION_FIELD, null);
    recordData.put(AMENDED_DATE_FIELD, null);

    ProgrammeMembership programmeMembership = mapper.toEntity(recordData);

    assertThat("Unexpected uuid.", programmeMembership.getUuid(), nullValue());
    assertThat("Unexpected PM type.", programmeMembership.getProgrammeMembershipType(),
        nullValue());
    assertThat("Unexpected programme start date.", programmeMembership.getProgrammeStartDate(),
        nullValue());
    assertThat("Unexpected programme end date.", programmeMembership.getProgrammeEndDate(),
        nullValue());
    assertThat("Unexpected programme ID.", programmeMembership.getProgrammeId(), nullValue());
    assertThat("Unexpected training number ID.", programmeMembership.getTrainingNumberId(),
        nullValue());
    assertThat("Unexpected person ID.", programmeMembership.getPersonId(), nullValue());
    assertThat("Unexpected rotation.", programmeMembership.getRotation(), nullValue());
    assertThat("Unexpected rotation ID.", programmeMembership.getRotationId(), nullValue());
    assertThat("Unexpected training pathway.", programmeMembership.getTrainingPathway(),
        nullValue());
    assertThat("Unexpected leaving reason.", programmeMembership.getLeavingReason(), nullValue());
    assertThat("Unexpected leaving destination.", programmeMembership.getLeavingDestination(),
        nullValue());
    assertThat("Unexpected amended date.", programmeMembership.getAmendedDate(), nullValue());
  }

  @Test
  void shouldMapPopulatedProgrammeMembershipToRecord() {
    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    programmeMembership.setUuid(UUID_VALUE);
    programmeMembership.setProgrammeMembershipType(PM_TYPE_VALUE);
    programmeMembership.setProgrammeStartDate(PROGRAMME_START_DATE_VALUE);
    programmeMembership.setProgrammeEndDate(PROGRAMME_END_DATE_VALUE);
    programmeMembership.setProgrammeId(PROGRAMME_ID_VALUE);
    programmeMembership.setTrainingNumberId(TRAINING_NUMBER_ID_VALUE);
    programmeMembership.setPersonId(PERSON_ID_VALUE);
    programmeMembership.setRotation(ROTATION_VALUE);
    programmeMembership.setRotationId(ROTATION_ID_VALUE);
    programmeMembership.setTrainingPathway(TRAINING_PATHWAY_VALUE);
    programmeMembership.setLeavingReason(LEAVING_REASON_VALUE);
    programmeMembership.setLeavingDestination(LEAVING_DESTINATION_VALUE);
    programmeMembership.setAmendedDate(AMENDED_DATE_VALUE);

    Record programmeMembershipRecord = mapper.toRecord(programmeMembership);

    assertThat("Unexpected TIS ID.", programmeMembershipRecord.getTisId(),
        is(UUID_VALUE.toString()));
    assertThat("Unexpected table.", programmeMembershipRecord.getTable(),
        is(ProgrammeMembership.ENTITY_NAME));
    assertThat("Unexpected schema.", programmeMembershipRecord.getSchema(),
        is(ProgrammeMembership.SCHEMA_NAME));

    Map<String, String> recordData = programmeMembershipRecord.getData();
    assertThat("Unexpected uuid.", recordData.get(UUID_FIELD), is(UUID_VALUE.toString()));
    assertThat("Unexpected PM type.", recordData.get(PM_TYPE_FIELD), is(PM_TYPE_VALUE));
    assertThat("Unexpected programme start date.", recordData.get(PROGRAMME_START_DATE_FIELD),
        is(PROGRAMME_START_DATE_VALUE.toString()));
    assertThat("Unexpected programme end date.", recordData.get(PROGRAMME_END_DATE_FIELD),
        is(PROGRAMME_END_DATE_VALUE.toString()));
    assertThat("Unexpected programme ID.", recordData.get(PROGRAMME_ID_FIELD),
        is(String.valueOf(PROGRAMME_ID_VALUE)));
    assertThat("Unexpected training number ID.", recordData.get(TRAINING_NUMBER_ID_FIELD),
        is(String.valueOf(TRAINING_NUMBER_ID_VALUE)));
    assertThat("Unexpected person ID.", recordData.get(PERSON_ID_FIELD),
        is(String.valueOf(PERSON_ID_VALUE)));
    assertThat("Unexpected rotation.", recordData.get(ROTATION_FIELD), is(ROTATION_VALUE));
    assertThat("Unexpected rotation ID.", recordData.get(ROTATION_ID_FIELD),
        is(String.valueOf(ROTATION_ID_VALUE)));
    assertThat("Unexpected training pathway.", recordData.get(TRAINING_PATHWAY_FIELD),
        is(TRAINING_PATHWAY_VALUE));
    assertThat("Unexpected leaving reason.", recordData.get(LEAVING_REASON_FIELD),
        is(LEAVING_REASON_VALUE));
    assertThat("Unexpected leaving destination.", recordData.get(LEAVING_DESTINATION_FIELD),
        is(LEAVING_DESTINATION_VALUE));
    assertThat("Unexpected amended date.", recordData.get(AMENDED_DATE_FIELD),
        is(AMENDED_DATE_VALUE.toString()));
    assertThat("Unexpected data count.", recordData.size(), is(13));
  }

  @Test
  void shouldMapNullProgrammeMembershipToRecord() {
    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    programmeMembership.setUuid(null);
    programmeMembership.setProgrammeMembershipType(null);
    programmeMembership.setProgrammeStartDate(null);
    programmeMembership.setProgrammeEndDate(null);
    programmeMembership.setProgrammeId(null);
    programmeMembership.setTrainingNumberId(null);
    programmeMembership.setPersonId(null);
    programmeMembership.setRotation(null);
    programmeMembership.setRotationId(null);
    programmeMembership.setTrainingPathway(null);
    programmeMembership.setLeavingReason(null);
    programmeMembership.setLeavingDestination(null);
    programmeMembership.setAmendedDate(null);

    Record programmeMembershipRecord = mapper.toRecord(programmeMembership);

    assertThat("Unexpected TIS ID.", programmeMembershipRecord.getTisId(), nullValue());

    Map<String, String> recordData = programmeMembershipRecord.getData();
    assertThat("Unexpected uuid.", recordData.get(UUID_FIELD), nullValue());
    assertThat("Unexpected PM type.", recordData.get(PM_TYPE_FIELD), nullValue());
    assertThat("Unexpected programme start date.", recordData.get(PROGRAMME_START_DATE_FIELD),
        nullValue());
    assertThat("Unexpected programme end date.", recordData.get(PROGRAMME_END_DATE_FIELD),
        nullValue());
    assertThat("Unexpected programme ID.", recordData.get(PROGRAMME_ID_FIELD), nullValue());
    assertThat("Unexpected training number ID.", recordData.get(TRAINING_NUMBER_ID_FIELD),
        nullValue());
    assertThat("Unexpected person ID.", recordData.get(PERSON_ID_FIELD), nullValue());
    assertThat("Unexpected rotation.", recordData.get(ROTATION_FIELD), nullValue());
    assertThat("Unexpected rotation ID.", recordData.get(ROTATION_ID_FIELD), nullValue());
    assertThat("Unexpected training pathway.", recordData.get(TRAINING_PATHWAY_FIELD), nullValue());
    assertThat("Unexpected leaving reason.", recordData.get(LEAVING_REASON_FIELD), nullValue());
    assertThat("Unexpected leaving destination.", recordData.get(LEAVING_DESTINATION_FIELD),
        nullValue());
    assertThat("Unexpected amended date.", recordData.get(AMENDED_DATE_FIELD), nullValue());
    assertThat("Unexpected data count.", recordData.size(), is(13));
  }
}

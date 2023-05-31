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
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.nhs.hee.tis.trainee.sync.model.ProgrammeMembership;
import uk.nhs.hee.tis.trainee.sync.model.Record;

class ProgrammeMembershipMapperTest {

  private static final UUID ID = UUID.randomUUID();

  private ProgrammeMembershipMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new ProgrammeMembershipMapperImpl();
  }

  @Test
  void shouldMapProgrammeMembershipToRecord() {
    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    programmeMembership.setUuid(ID);
    programmeMembership.setProgrammeMembershipType("pmType1");
    programmeMembership.setProgrammeStartDate(LocalDate.MIN);
    programmeMembership.setProgrammeEndDate(LocalDate.MAX);
    programmeMembership.setProgrammeId(2L);
    programmeMembership.setTrainingNumberId(3L);
    programmeMembership.setPersonId(4L);
    programmeMembership.setRotation("rotation5");
    programmeMembership.setRotationId(6L);
    programmeMembership.setTrainingPathway("trainingPathway7");
    programmeMembership.setLeavingReason("leavingReason8");
    programmeMembership.setLeavingDestination("leavingDestination9");
    programmeMembership.setAmendedDate(Instant.EPOCH);

    Record programmeMembershipRecord = mapper.toRecord(programmeMembership);

    assertThat("Unexpected TIS ID.", programmeMembershipRecord.getTisId(), is(ID.toString()));

    Map<String, String> recordData = programmeMembershipRecord.getData();
    assertThat("Unexpected uuid.", recordData.get("uuid"), is(ID.toString()));
    assertThat("Unexpected PM type.", recordData.get("programmeMembershipType"), is("pmType1"));
    assertThat("Unexpected programme start date.", recordData.get("programmeStartDate"),
        is(LocalDate.MIN.toString()));
    assertThat("Unexpected programme end date.", recordData.get("programmeEndDate"),
        is(LocalDate.MAX.toString()));
    assertThat("Unexpected programme ID.", recordData.get("programmeId"), is("2"));
    assertThat("Unexpected training number ID.", recordData.get("trainingNumberId"), is("3"));
    assertThat("Unexpected person ID.", recordData.get("personId"), is("4"));
    assertThat("Unexpected rotation.", recordData.get("rotation"), is("rotation5"));
    assertThat("Unexpected rotation ID.", recordData.get("rotationId"), is("6"));
    assertThat("Unexpected training pathway.", recordData.get("trainingPathway"),
        is("trainingPathway7"));
    assertThat("Unexpected leaving reason.", recordData.get("leavingReason"), is("leavingReason8"));
    assertThat("Unexpected leaving destination.", recordData.get("leavingDestination"),
        is("leavingDestination9"));
    assertThat("Unexpected amended date.", recordData.get("amendedDate"),
        is(Instant.EPOCH.toString()));
    assertThat("Unexpected data count.", recordData.size(), is(13));
  }
}

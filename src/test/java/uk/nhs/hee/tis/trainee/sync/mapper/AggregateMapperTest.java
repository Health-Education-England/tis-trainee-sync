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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.nhs.hee.tis.trainee.sync.dto.AggregateCurriculumMembershipDto;
import uk.nhs.hee.tis.trainee.sync.dto.AggregateProgrammeMembershipDto;
import uk.nhs.hee.tis.trainee.sync.model.Curriculum;
import uk.nhs.hee.tis.trainee.sync.model.CurriculumMembership;
import uk.nhs.hee.tis.trainee.sync.model.Programme;
import uk.nhs.hee.tis.trainee.sync.model.ProgrammeMembership;
import uk.nhs.hee.tis.trainee.sync.model.Record;

class AggregateMapperTest {

  private static final String TRAINEE_ID = String.valueOf(new Random().nextLong());

  private static final String CURRICULUM_ID = UUID.randomUUID().toString();
  private static final String CURRICULUM_NAME = "Dermatology";
  private static final String CURRICULUM_SUB_TYPE = "MEDICAL_CURRICULUM";

  private static final String CURRICULUM_MEMBERSHIP_ID = UUID.randomUUID().toString();
  private static final LocalDate CURRICULUM_MEMBERSHIP_START_DATE = LocalDate.now().minusYears(1L);
  private static final LocalDate CURRICULUM_MEMBERSHIP_END_DATE = LocalDate.now().plusYears(1L);

  private static final String PROGRAMME_ID = String.valueOf(new Random().nextLong());
  private static final String PROGRAMME_NAME = UUID.randomUUID().toString();
  private static final String PROGRAMME_NUMBER = UUID.randomUUID().toString();
  private static final String PROGRAMME_OWNER = UUID.randomUUID().toString();

  private static final UUID PROGRAMME_MEMBERSHIP_ID = UUID.randomUUID();
  private static final String PROGRAMME_MEMBERSHIP_TYPE = "SUBSTANTIVE";
  private static final LocalDate PROGRAMME_MEMBERSHIP_START_DATE = LocalDate.now().minusYears(2L);
  private static final LocalDate PROGRAMME_MEMBERSHIP_END_DATE = LocalDate.now().plusYears(2L);


  private AggregateMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new AggregateMapperImpl();
  }

  @Test
  void shouldAggregateCurriculumMembershipDto() {
    Curriculum curriculum = new Curriculum();
    curriculum.setTisId(CURRICULUM_ID);
    curriculum.setData(Map.of(
        "name", CURRICULUM_NAME,
        "curriculumSubType", CURRICULUM_SUB_TYPE
    ));

    CurriculumMembership curriculumMembership = new CurriculumMembership();
    curriculumMembership.setTisId(CURRICULUM_MEMBERSHIP_ID);
    curriculumMembership.setData(Map.of(
        "curriculumStartDate", CURRICULUM_MEMBERSHIP_START_DATE.toString(),
        "curriculumEndDate", CURRICULUM_MEMBERSHIP_END_DATE.toString()
    ));

    AggregateCurriculumMembershipDto aggregateCurriculum =
        mapper.toAggregateCurriculumMembershipDto(curriculum, curriculumMembership);

    assertThat("Unexpected curriculum ID.", aggregateCurriculum.getCurriculumTisId(),
        is(CURRICULUM_ID));
    assertThat("Unexpected curriculum name.", aggregateCurriculum.getCurriculumName(),
        is(CURRICULUM_NAME));
    assertThat("Unexpected curriculum sub type.", aggregateCurriculum.getCurriculumSubType(),
        is(CURRICULUM_SUB_TYPE));
    assertThat("Unexpected curriculum membership ID.",
        aggregateCurriculum.getCurriculumMembershipId(), is(CURRICULUM_MEMBERSHIP_ID));
    assertThat("Unexpected curriculum start date.", aggregateCurriculum.getCurriculumStartDate(),
        is(CURRICULUM_MEMBERSHIP_START_DATE));
    assertThat("Unexpected curriculum end date.", aggregateCurriculum.getCurriculumEndDate(),
        is(CURRICULUM_MEMBERSHIP_END_DATE));
  }

  @Test
  void shouldAggregateProgrammeMembershipDto() {
    Programme programme = new Programme();
    programme.setTisId(PROGRAMME_ID);
    programme.setData(Map.of(
        "programmeName", PROGRAMME_NAME,
        "programmeNumber", PROGRAMME_NUMBER,
        "owner", PROGRAMME_OWNER
    ));

    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    programmeMembership.setUuid(PROGRAMME_MEMBERSHIP_ID);
    programmeMembership.setPersonId(Long.parseLong(TRAINEE_ID));
    programmeMembership.setProgrammeMembershipType(PROGRAMME_MEMBERSHIP_TYPE);
    programmeMembership.setProgrammeStartDate(PROGRAMME_MEMBERSHIP_START_DATE);
    programmeMembership.setProgrammeEndDate(PROGRAMME_MEMBERSHIP_END_DATE);

    var aggregateCurriculumMembership = new AggregateCurriculumMembershipDto();
    aggregateCurriculumMembership.setCurriculumEndDate(CURRICULUM_MEMBERSHIP_END_DATE);
    List<AggregateCurriculumMembershipDto> curricula = List.of(aggregateCurriculumMembership);

    AggregateProgrammeMembershipDto aggregateProgrammeMembership =
        mapper.toAggregateProgrammeMembershipDto(programmeMembership, programme, curricula);

    assertThat("Unexpected TIS ID.", aggregateProgrammeMembership.getTisId(),
        is(PROGRAMME_MEMBERSHIP_ID.toString()));
    assertThat("Unexpected person ID.",
        aggregateProgrammeMembership.getPersonId(), is(TRAINEE_ID));
    assertThat("Unexpected programme ID.", aggregateProgrammeMembership.getProgrammeTisId(),
        is(PROGRAMME_ID));
    assertThat("Unexpected programme name.", aggregateProgrammeMembership.getProgrammeName(),
        is(PROGRAMME_NAME));
    assertThat("Unexpected programme number.", aggregateProgrammeMembership.getProgrammeNumber(),
        is(PROGRAMME_NUMBER));
    assertThat("Unexpected managing deanery.", aggregateProgrammeMembership.getManagingDeanery(),
        is(PROGRAMME_OWNER));
    assertThat("Unexpected programme membership type.",
        aggregateProgrammeMembership.getProgrammeMembershipType(), is(PROGRAMME_MEMBERSHIP_TYPE));
    assertThat("Unexpected start date.", aggregateProgrammeMembership.getStartDate(),
        is(PROGRAMME_MEMBERSHIP_START_DATE));
    assertThat("Unexpected end date.", aggregateProgrammeMembership.getEndDate(),
        is(PROGRAMME_MEMBERSHIP_END_DATE));
    assertThat("Unexpected programme completion date.",
        aggregateProgrammeMembership.getProgrammeCompletionDate(),
        is(CURRICULUM_MEMBERSHIP_END_DATE));
    assertThat("Unexpected curricula.",
        aggregateProgrammeMembership.getCurricula(), is(curricula));
  }

  @Test
  void shouldNotSetProgrammeCompletionDateWhenNoCurriculaEndDate() {
    Programme programme = new Programme();
    ProgrammeMembership programmeMembership = new ProgrammeMembership();

    var aggregateCurriculumMembership = new AggregateCurriculumMembershipDto();
    aggregateCurriculumMembership.setCurriculumEndDate(null);
    List<AggregateCurriculumMembershipDto> curricula = List.of(aggregateCurriculumMembership);

    AggregateProgrammeMembershipDto aggregateProgrammeMembership =
        mapper.toAggregateProgrammeMembershipDto(programmeMembership, programme, curricula);

    assertThat("Unexpected programme completion date.",
        aggregateProgrammeMembership.getProgrammeCompletionDate(), nullValue());
  }

  @Test
  void shouldNotSetProgrammeCompletionDateWhenNoCurricula() {
    Programme programme = new Programme();
    ProgrammeMembership programmeMembership = new ProgrammeMembership();

    AggregateProgrammeMembershipDto aggregateProgrammeMembership =
        mapper.toAggregateProgrammeMembershipDto(programmeMembership, programme, List.of());

    assertThat("Unexpected programme completion date.",
        aggregateProgrammeMembership.getProgrammeCompletionDate(), nullValue());
  }

  @Test
  void shouldSetFurthestProgrammeCompletionDateWhenMultipleCurricula() {
    Programme programme = new Programme();
    ProgrammeMembership programmeMembership = new ProgrammeMembership();

    var aggregateCurriculumMembership1 = new AggregateCurriculumMembershipDto();
    aggregateCurriculumMembership1.setCurriculumEndDate(CURRICULUM_MEMBERSHIP_END_DATE);
    var aggregateCurriculumMembership2 = new AggregateCurriculumMembershipDto();
    aggregateCurriculumMembership2.setCurriculumEndDate(
        CURRICULUM_MEMBERSHIP_END_DATE.minusDays(1));
    List<AggregateCurriculumMembershipDto> curricula = List.of(aggregateCurriculumMembership1,
        aggregateCurriculumMembership2);

    AggregateProgrammeMembershipDto aggregateProgrammeMembership =
        mapper.toAggregateProgrammeMembershipDto(programmeMembership, programme, curricula);

    assertThat("Unexpected programme completion date.",
        aggregateProgrammeMembership.getProgrammeCompletionDate(),
        is(CURRICULUM_MEMBERSHIP_END_DATE));
  }

  @Test
  void shouldMapAggregateProgrammeMembershipToRecord() throws JsonProcessingException {
    var curriculumMembership1 = new AggregateCurriculumMembershipDto();
    curriculumMembership1.setCurriculumTisId(CURRICULUM_ID);
    curriculumMembership1.setCurriculumName(CURRICULUM_NAME);
    curriculumMembership1.setCurriculumSubType(CURRICULUM_SUB_TYPE);
    curriculumMembership1.setCurriculumMembershipId(CURRICULUM_MEMBERSHIP_ID);
    curriculumMembership1.setCurriculumStartDate(CURRICULUM_MEMBERSHIP_START_DATE);
    curriculumMembership1.setCurriculumEndDate(CURRICULUM_MEMBERSHIP_END_DATE);

    var curriculumMembership2 = new AggregateCurriculumMembershipDto();
    curriculumMembership2.setCurriculumTisId("c2");
    curriculumMembership2.setCurriculumName("name2");
    curriculumMembership2.setCurriculumSubType("subType2");
    curriculumMembership2.setCurriculumMembershipId("cm2");
    curriculumMembership2.setCurriculumStartDate(LocalDate.now().minusYears(3L));
    curriculumMembership2.setCurriculumEndDate(LocalDate.now().plusYears(3L));

    AggregateProgrammeMembershipDto programmeMembership = new AggregateProgrammeMembershipDto();
    programmeMembership.setTisId(PROGRAMME_MEMBERSHIP_ID.toString());
    programmeMembership.setPersonId(TRAINEE_ID);
    programmeMembership.setProgrammeTisId(PROGRAMME_ID);
    programmeMembership.setProgrammeName(PROGRAMME_NAME);
    programmeMembership.setProgrammeNumber(PROGRAMME_NUMBER);
    programmeMembership.setManagingDeanery(PROGRAMME_OWNER);
    programmeMembership.setProgrammeMembershipType(PROGRAMME_MEMBERSHIP_TYPE);
    programmeMembership.setStartDate(PROGRAMME_MEMBERSHIP_START_DATE);
    programmeMembership.setEndDate(PROGRAMME_MEMBERSHIP_END_DATE);
    programmeMembership.setProgrammeCompletionDate(CURRICULUM_MEMBERSHIP_END_DATE);
    programmeMembership.setCurricula(List.of(curriculumMembership1, curriculumMembership2));

    Record record = mapper.toRecord(programmeMembership);

    assertThat("Unexpected TIS ID.", record.getTisId(), is(PROGRAMME_MEMBERSHIP_ID.toString()));

    Map<String, String> recordData = record.getData();
    assertThat("Unexpected record data count.", recordData.size(), is(11));
    assertThat("Unexpected TIS ID.", recordData.get("tisId"),
        is(PROGRAMME_MEMBERSHIP_ID.toString()));
    assertThat("Unexpected person ID.", recordData.get("personId"), is(TRAINEE_ID));
    assertThat("Unexpected programme ID.", recordData.get("programmeTisId"), is(PROGRAMME_ID));
    assertThat("Unexpected programme name.", recordData.get("programmeName"), is(PROGRAMME_NAME));
    assertThat("Unexpected programme number.", recordData.get("programmeNumber"),
        is(PROGRAMME_NUMBER));
    assertThat("Unexpected managing deanery.", recordData.get("managingDeanery"),
        is(PROGRAMME_OWNER));
    assertThat("Unexpected programme membership type.", recordData.get("programmeMembershipType"),
        is(PROGRAMME_MEMBERSHIP_TYPE));
    assertThat("Unexpected start date.", recordData.get("startDate"),
        is(PROGRAMME_MEMBERSHIP_START_DATE.toString()));
    assertThat("Unexpected end date.", recordData.get("endDate"),
        is(PROGRAMME_MEMBERSHIP_END_DATE.toString()));
    assertThat("Unexpected programme completion date.", recordData.get("programmeCompletionDate"),
        is(CURRICULUM_MEMBERSHIP_END_DATE.toString()));

    String curricula = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .writeValueAsString(List.of(curriculumMembership1, curriculumMembership2));
    assertThat("Unexpected curricula.", recordData.get("curricula"), is(curricula));
  }

  @Test
  void shouldNotHaveSideEffectsWhenMappingAggregateProgrammeMembershipToRecord() {
    var curriculumMembership = new AggregateCurriculumMembershipDto();
    curriculumMembership.setCurriculumTisId(CURRICULUM_ID);
    curriculumMembership.setCurriculumName(CURRICULUM_NAME);
    curriculumMembership.setCurriculumSubType(CURRICULUM_SUB_TYPE);
    curriculumMembership.setCurriculumMembershipId(CURRICULUM_MEMBERSHIP_ID);
    curriculumMembership.setCurriculumStartDate(CURRICULUM_MEMBERSHIP_START_DATE);
    curriculumMembership.setCurriculumEndDate(CURRICULUM_MEMBERSHIP_END_DATE);

    AggregateProgrammeMembershipDto programmeMembership = new AggregateProgrammeMembershipDto();
    programmeMembership.setTisId(PROGRAMME_MEMBERSHIP_ID.toString());
    programmeMembership.setPersonId(TRAINEE_ID);
    programmeMembership.setProgrammeTisId(PROGRAMME_ID);
    programmeMembership.setProgrammeName(PROGRAMME_NAME);
    programmeMembership.setProgrammeNumber(PROGRAMME_NUMBER);
    programmeMembership.setManagingDeanery(PROGRAMME_OWNER);
    programmeMembership.setProgrammeMembershipType(PROGRAMME_MEMBERSHIP_TYPE);
    programmeMembership.setStartDate(PROGRAMME_MEMBERSHIP_START_DATE);
    programmeMembership.setEndDate(PROGRAMME_MEMBERSHIP_END_DATE);
    programmeMembership.setProgrammeCompletionDate(CURRICULUM_MEMBERSHIP_END_DATE);
    programmeMembership.setCurricula(List.of(curriculumMembership));

    mapper.toRecord(programmeMembership);

    assertThat("Unexpected TIS ID.", programmeMembership.getTisId(),
        is(PROGRAMME_MEMBERSHIP_ID.toString()));
    assertThat("Unexpected person ID.", programmeMembership.getPersonId(), is(TRAINEE_ID));
    assertThat("Unexpected programme ID.", programmeMembership.getProgrammeTisId(),
        is(PROGRAMME_ID));
    assertThat("Unexpected programme name.", programmeMembership.getProgrammeName(),
        is(PROGRAMME_NAME));
    assertThat("Unexpected programme number.", programmeMembership.getProgrammeNumber(),
        is(PROGRAMME_NUMBER));
    assertThat("Unexpected managing deanery.", programmeMembership.getManagingDeanery(),
        is(PROGRAMME_OWNER));
    assertThat("Unexpected programme membership type.",
        programmeMembership.getProgrammeMembershipType(), is(PROGRAMME_MEMBERSHIP_TYPE));
    assertThat("Unexpected start date.", programmeMembership.getStartDate(),
        is(PROGRAMME_MEMBERSHIP_START_DATE));
    assertThat("Unexpected end date.", programmeMembership.getEndDate(),
        is(PROGRAMME_MEMBERSHIP_END_DATE));
    assertThat("Unexpected programme completion date.",
        programmeMembership.getProgrammeCompletionDate(), is(CURRICULUM_MEMBERSHIP_END_DATE));
    assertThat("Unexpected curricula.", programmeMembership.getCurricula(),
        is(List.of(curriculumMembership)));
  }
}

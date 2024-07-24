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

package uk.nhs.hee.tis.trainee.sync.mapper;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.util.AssertionErrors.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.lang.reflect.Field;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.util.ReflectionUtils;
import uk.nhs.hee.tis.trainee.sync.dto.HeeUserDto;
import uk.nhs.hee.tis.trainee.sync.dto.TraineeDetailsDto;
import uk.nhs.hee.tis.trainee.sync.mapper.util.TraineeDetailsUtil;
import uk.nhs.hee.tis.trainee.sync.model.Record;

class TraineeDetailsMapperTest {

  private static final String RO_FIRST_NAME = "RO first";
  private static final String RO_LAST_NAME = "RO last";
  private static final String RO_EMAIL = "RO email";
  private static final String RO_GMC = "RO GMC";
  private static final String RO_PHONE = "RO phone";

  private TraineeDetailsMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new TraineeDetailsMapperImpl();

    Field field = ReflectionUtils.findField(TraineeDetailsMapperImpl.class, "traineeDetailsUtil");
    field.setAccessible(true);
    ReflectionUtils.setField(field, mapper, new TraineeDetailsUtil());
  }

  @Test
  void shouldMapCurriculaToEmptySetWhenBadJson() {
    Record recrd = new Record();
    recrd.setData(Map.of("curricula", "bad JSON"));

    TraineeDetailsDto traineeDetails = mapper.toProgrammeMembershipDto(recrd);

    assertThat("Unexpected curricula size.", traineeDetails.getCurricula().size(), is(0));
  }

  @Test
  void shouldMapCurriculaToEmptySetWhenMissing() {
    Record recrd = new Record();
    recrd.setData(Map.of());

    TraineeDetailsDto traineeDetails = mapper.toProgrammeMembershipDto(recrd);

    assertThat("Unexpected curricula size.", traineeDetails.getCurricula().size(), is(0));
  }

  @Test
  void shouldMapConditionsOfJoiningToEmptyMapWhenBadJson() {
    Record recrd = new Record();
    recrd.setData(Map.of("conditionsOfJoining", "bad JSON"));

    TraineeDetailsDto traineeDetails = mapper.toProgrammeMembershipDto(recrd);

    assertTrue("Unexpected conditions of joining.",
        traineeDetails.getConditionsOfJoining().isEmpty());
  }

  @Test
  void shouldMapConditionsOfJoiningToEmptyMapWhenMissing() {
    Record recrd = new Record();
    recrd.setData(Map.of());

    TraineeDetailsDto traineeDetails = mapper.toProgrammeMembershipDto(recrd);

    assertTrue("Unexpected conditions of joining.",
        traineeDetails.getConditionsOfJoining().isEmpty());
  }

  @Test
  void shouldMapOtherSitesToEmptySetWhenBadJson() {
    Record recrd = new Record();
    recrd.setData(Map.of("otherSites", "bad JSON"));

    TraineeDetailsDto traineeDetails = mapper.toPlacementDto(recrd);

    assertThat("Unexpected other sites size.", traineeDetails.getOtherSites().size(), is(0));
  }

  @Test
  void shouldMapOtherSitesToEmptySetWhenMissing() {
    Record recrd = new Record();
    recrd.setData(Map.of());

    TraineeDetailsDto traineeDetails = mapper.toPlacementDto(recrd);

    assertThat("Unexpected other sites size.", traineeDetails.getOtherSites().size(), is(0));
  }

  @Test
  void shouldMapWholeTimeEquivalentToNullWhenMissing() {
    Record recrd = new Record();
    recrd.setData(Map.of());

    TraineeDetailsDto traineeDetails = mapper.toPlacementDto(recrd);

    assertThat("Unexpected WTE.", traineeDetails.getWholeTimeEquivalent(), nullValue());
  }

  @ParameterizedTest
  @ValueSource(strings = {"wholeTimeEquivalent", "placementWholeTimeEquivalent"})
  void shouldMapWholeTimeEquivalentForDifferentFieldNames(String fieldName) {
    Record recrd = new Record();
    recrd.setData(Map.of(fieldName, "0.5"));

    TraineeDetailsDto traineeDetails = mapper.toPlacementDto(recrd);

    assertThat("Unexpected WTE.", traineeDetails.getWholeTimeEquivalent(), is("0.5"));
  }

  @Test
  void shouldMapResponsibleOfficerToEmptySetWhenMissing() {
    Record recrd = new Record();
    recrd.setData(Map.of());

    TraineeDetailsDto traineeDetails = mapper.toAggregateProgrammeMembershipDto(recrd);

    assertThat("Unexpected responsible officer.", traineeDetails.getResponsibleOfficer().size(),
        is(0));
  }

  @Test
  void shouldMapResponsibleOfficerToEmptySetWhenBadJson() {
    Record recrd = new Record();
    recrd.setData(Map.of("responsibleOfficer", "bad JSON"));

    TraineeDetailsDto traineeDetails = mapper.toAggregateProgrammeMembershipDto(recrd);

    assertThat("Unexpected responsible officer.", traineeDetails.getResponsibleOfficer().size(),
        is(0));
  }

  @Test
  void shouldMapResponsibleOfficer() throws JsonProcessingException {
    HeeUserDto roDto = new HeeUserDto();
    roDto.setFirstName(RO_FIRST_NAME);
    roDto.setLastName(RO_LAST_NAME);
    roDto.setEmailAddress(RO_EMAIL);
    roDto.setGmcId(RO_GMC);
    roDto.setPhoneNumber(RO_PHONE);

    ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    Record recrd = new Record();
    recrd.setData(Map.of("responsibleOfficer", objectMapper.writeValueAsString(roDto)));

    TraineeDetailsDto traineeDetails = mapper.toAggregateProgrammeMembershipDto(recrd);

    Map<String, String> mappedRo = traineeDetails.getResponsibleOfficer();
    assertThat("Unexpected responsible officer email.", mappedRo.get("emailAddress"),
        is(RO_EMAIL));
    assertThat("Unexpected responsible officer first name.", mappedRo.get("firstName"),
        is(RO_FIRST_NAME));
    assertThat("Unexpected responsible officer email.", mappedRo.get("lastName"),
        is(RO_LAST_NAME));
    assertThat("Unexpected responsible officer email.", mappedRo.get("gmcId"),
        is(RO_GMC));
    assertThat("Unexpected responsible officer phone.", mappedRo.get("phoneNumber"),
        is(RO_PHONE));
  }

  @Test
  void shouldMapOtherSpecialtiesToEmptySetWhenBadJson() {
    Record recrd = new Record();
    recrd.setData(Map.of("otherSpecialties", "bad JSON"));

    TraineeDetailsDto traineeDetails = mapper.toPlacementDto(recrd);

    assertThat("Unexpected other specialties size.", traineeDetails.getOtherSpecialties().size(),
        is(0));
  }

  @Test
  void shouldMapOtherSpecialtiesToEmptySetWhenMissing() {
    Record recrd = new Record();
    recrd.setData(Map.of());

    TraineeDetailsDto traineeDetails = mapper.toPlacementDto(recrd);

    assertThat("Unexpected other specialties size.", traineeDetails.getOtherSpecialties().size(),
        is(0));
  }
}

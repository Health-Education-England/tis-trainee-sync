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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants.ComponentModel;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import uk.nhs.hee.tis.trainee.sync.dto.AggregateCurriculumMembershipDto;
import uk.nhs.hee.tis.trainee.sync.dto.AggregateProgrammeMembershipDto;
import uk.nhs.hee.tis.trainee.sync.model.ConditionsOfJoining;
import uk.nhs.hee.tis.trainee.sync.model.Curriculum;
import uk.nhs.hee.tis.trainee.sync.model.CurriculumMembership;
import uk.nhs.hee.tis.trainee.sync.model.Dbc;
import uk.nhs.hee.tis.trainee.sync.model.HeeUser;
import uk.nhs.hee.tis.trainee.sync.model.Programme;
import uk.nhs.hee.tis.trainee.sync.model.ProgrammeMembership;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.model.Specialty;

/**
 * A mapper for creating DTO aggregating from multiple data types.
 */
@Mapper(componentModel = ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE,
    uses = HeeUserMapper.class)
public interface AggregateMapper {

  /**
   * Create an aggregate curriculum DTO.
   *
   * @param curriculum           The curriculum to aggregate from.
   * @param curriculumMembership The curriculum membership to aggregate from.
   * @return The aggregated curriculum DTO.
   */
  @Mapping(target = "curriculumTisId", source = "curriculum.tisId")
  @Mapping(target = "curriculumName", source = "curriculum.data.name")
  @Mapping(target = "curriculumSubType", source = "curriculum.data.curriculumSubType")
  @Mapping(target = "curriculumSpecialty", source = "specialty.data.name")
  @Mapping(target = "curriculumSpecialtyCode", source = "specialty.data.specialtyCode")
  @Mapping(target = "curriculumSpecialtyBlockIndemnity", source = "specialty.data.blockIndemnity")
  @Mapping(target = "curriculumMembershipId", source = "curriculumMembership.tisId")
  @Mapping(target = "curriculumStartDate", source = "curriculumMembership.data.curriculumStartDate")
  @Mapping(target = "curriculumEndDate", source = "curriculumMembership.data.curriculumEndDate")
  AggregateCurriculumMembershipDto toAggregateCurriculumMembershipDto(Curriculum curriculum,
      CurriculumMembership curriculumMembership, Specialty specialty);

  /**
   * Create an aggregate programme membership DTO.
   *
   * @param programmeMembership The programme membership to aggregate from.
   * @param programme           The programme to aggregate from.
   * @param curricula           The curricula to aggregate from.
   * @return The aggregated programme membership DTO.
   */
  @Mapping(target = "tisId", source = "programmeMembership.uuid")
  @Mapping(target = "personId", source = "programmeMembership.personId")
  @Mapping(target = "programmeMembershipType",
      source = "programmeMembership.programmeMembershipType")
  @Mapping(target = "startDate", source = "programmeMembership.programmeStartDate")
  @Mapping(target = "endDate", source = "programmeMembership.programmeEndDate")
  @Mapping(target = "programmeTisId", source = "programme.tisId")
  @Mapping(target = "programmeName", source = "programme.data.programmeName")
  @Mapping(target = "programmeNumber", source = "programme.data.programmeNumber")
  @Mapping(target = "managingDeanery", source = "programme.data.owner")
  @Mapping(target = "designatedBody", source = "dbc.data.name")
  @Mapping(target = "designatedBodyCode", source = "dbc.data.dbc")
  @Mapping(target = "programmeCompletionDate", ignore = true)
  @Mapping(target = "trainingPathway", source = "programmeMembership.trainingPathway")
  @Mapping(target = "curricula", source = "curricula")
  @Mapping(target = "conditionsOfJoining", source = "conditionsOfJoining")
  @Mapping(target = "responsibleOfficer", source = "responsibleOfficer")
  AggregateProgrammeMembershipDto toAggregateProgrammeMembershipDto(
      ProgrammeMembership programmeMembership, Programme programme,
      List<AggregateCurriculumMembershipDto> curricula, ConditionsOfJoining conditionsOfJoining,
      Dbc dbc, HeeUser responsibleOfficer);

  /**
   * Convert a ProgrammeMembershipDto to a Record.
   *
   * @param aggregateProgrammeMembershipDto The ProgrammeMembershipDto to map.
   * @return The mapped Record.
   */
  default Record toRecord(AggregateProgrammeMembershipDto aggregateProgrammeMembershipDto) {
    ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // Remove the curricula as it must be mapped separately.
    var curricula = aggregateProgrammeMembershipDto.getCurricula();
    aggregateProgrammeMembershipDto.setCurricula(null);
    // Remove the conditionsOfJoining as it must be mapped separately
    var conditionsOfJoining = aggregateProgrammeMembershipDto.getConditionsOfJoining();
    aggregateProgrammeMembershipDto.setConditionsOfJoining(null);
    // Remove the responsibleOfficer as it must be mapped separately
    var responsibleOfficer = aggregateProgrammeMembershipDto.getResponsibleOfficer();
    aggregateProgrammeMembershipDto.setResponsibleOfficer(null);

    Map<String, String> recordData = objectMapper.convertValue(aggregateProgrammeMembershipDto,
        new TypeReference<>() {
        });

    try {
      // Restore the DTO to its original state and set the curricula and conditions of joining
      // record data.
      aggregateProgrammeMembershipDto.setCurricula(curricula);
      recordData.put("curricula", objectMapper.writeValueAsString(curricula));
      aggregateProgrammeMembershipDto.setConditionsOfJoining(conditionsOfJoining);
      recordData.put("conditionsOfJoining", objectMapper.writeValueAsString(conditionsOfJoining));
      aggregateProgrammeMembershipDto.setResponsibleOfficer(responsibleOfficer);
      recordData.put("responsibleOfficer", objectMapper.writeValueAsString(responsibleOfficer));
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }

    Record programmeMembershipRecord = new Record();
    programmeMembershipRecord.setData(recordData);
    programmeMembershipRecord.setTisId(aggregateProgrammeMembershipDto.getTisId());
    return programmeMembershipRecord;
  }

  /**
   * Parse a boolean from a string value.
   *
   * @param s The string to parse.
   * @return The parsed boolean.
   */
  default boolean parseBoolean(String s) {
    return Boolean.parseBoolean(s) || Objects.equals("1", s);
  }

  /**
   * Calculate the programme completion date.
   *
   * @param aggregateProgrammeMembership The aggregated programme membership to calculate the
   *                                     completion date for.
   */
  @AfterMapping
  default void calculateProgrammeCompletionDate(
      @MappingTarget AggregateProgrammeMembershipDto aggregateProgrammeMembership) {
    List<AggregateCurriculumMembershipDto> curricula = aggregateProgrammeMembership.getCurricula();

    LocalDate maxProgrammeCompletionDate = curricula.stream()
        .map(AggregateCurriculumMembershipDto::getCurriculumEndDate)
        .filter(Objects::nonNull)
        .max(LocalDate::compareTo)
        .orElse(null);

    aggregateProgrammeMembership.setProgrammeCompletionDate(maxProgrammeCompletionDate);
  }
}

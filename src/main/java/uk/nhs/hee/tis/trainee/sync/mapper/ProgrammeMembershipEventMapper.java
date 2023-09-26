/*
 * The MIT License (MIT)
 *
 *  Copyright 2023 Crown Copyright (Health Education England)
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
import uk.nhs.hee.tis.trainee.sync.dto.ProgrammeMembershipEventDto;
import uk.nhs.hee.tis.trainee.sync.model.ConditionsOfJoining;
import uk.nhs.hee.tis.trainee.sync.model.Curriculum;
import uk.nhs.hee.tis.trainee.sync.model.CurriculumMembership;
import uk.nhs.hee.tis.trainee.sync.model.Programme;
import uk.nhs.hee.tis.trainee.sync.model.ProgrammeMembership;
import uk.nhs.hee.tis.trainee.sync.model.Record;

/**
 * A mapper for creating programme membership event DTOs.
 */
@Mapper(componentModel = ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProgrammeMembershipEventMapper {

  /**
   * Create a programme membership event DTO.
   *
   * @param programmeMembership The programme membership to aggregate from.
   * @param programme           The programme to aggregate from.
   * @return The programme membership event DTO.
   */
  @Mapping(target = "tisId", source = "programmeMembership.uuid")
  @Mapping(target = "traineeTisId", source = "programmeMembership.personId")
  @Mapping(target = "managingDeanery", source = "programme.data.owner")
  @Mapping(target = "conditionsOfJoining", source = "conditionsOfJoining")
  ProgrammeMembershipEventDto toProgrammeMembershipEventDto(
      ProgrammeMembership programmeMembership, Programme programme,
      ConditionsOfJoining conditionsOfJoining);

  /**
   * Convert a ProgrammeMembershipEventDto to a Record.
   *
   * @param programmeMembershipEventDto The ProgrammeMembershipEventDto to map.
   * @return The mapped Record.
   */
  default Record toRecord(ProgrammeMembershipEventDto programmeMembershipEventDto) {
    ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // Remove the conditionsOfJoining as it must be mapped separately
    var conditionsOfJoining = programmeMembershipEventDto.getConditionsOfJoining();
    programmeMembershipEventDto.setConditionsOfJoining(null);

    Map<String, String> recordData = objectMapper.convertValue(programmeMembershipEventDto,
        new TypeReference<>() {
        });

    try {
      // Restore the DTO to its original state and set the conditions of joining record data.
      programmeMembershipEventDto.setConditionsOfJoining(conditionsOfJoining);
      recordData.put("conditionsOfJoining", objectMapper.writeValueAsString(conditionsOfJoining));
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }

    Record programmeMembershipEventRecord = new Record();
    programmeMembershipEventRecord.setData(recordData);
    programmeMembershipEventRecord.setTisId(programmeMembershipEventDto.getTisId());
    return programmeMembershipEventRecord;
  }
}

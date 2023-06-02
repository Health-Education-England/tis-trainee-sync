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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.mapstruct.BeforeMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants.ComponentModel;
import org.mapstruct.MappingTarget;
import uk.nhs.hee.tis.trainee.sync.dto.AggregateCurriculumDto;
import uk.nhs.hee.tis.trainee.sync.dto.AggregateProgrammeMembershipDto;
import uk.nhs.hee.tis.trainee.sync.model.Programme;
import uk.nhs.hee.tis.trainee.sync.model.ProgrammeMembership;
import uk.nhs.hee.tis.trainee.sync.model.Record;

/**
 * A mapper to convert between ProgrammeMembership data types.
 */
@Mapper(componentModel = ComponentModel.SPRING)
public interface ProgrammeMembershipMapper {

  @Mapping(target = "tisId", source = "uuid")
  @Mapping(target = "personId")
  @Mapping(target = "programmeTisId", source = "programmeId")
  @Mapping(target = "programmeMembershipType")
  @Mapping(target = "startDate", source = "programmeStartDate")
  @Mapping(target = "endDate", source = "programmeEndDate")
  AggregateProgrammeMembershipDto toDto(ProgrammeMembership programmeMembership);

  Set<AggregateProgrammeMembershipDto> toDtos(Set<ProgrammeMembership> programmeMemberships);

  @Mapping(target = "programmeTisId", source = "tisId")
  @Mapping(target = "programmeName", source = "data.programmeName")
  @Mapping(target = "programmeNumber", source = "data.programmeNumber")
  @Mapping(target = "managingDeanery", source = "data.owner")
  @Mapping(target = "tisId", ignore = true)
  @Mapping(target = "programmeMembershipType", ignore = true)
  @Mapping(target = "startDate", ignore = true)
  @Mapping(target = "endDate", ignore = true)
  @Mapping(target = "programmeCompletionDate", ignore = true)
  @Mapping(target = "curricula", ignore = true)
  // TODO: clean up unmapped fields ignore rules
  void populateProgrammeData(@MappingTarget AggregateProgrammeMembershipDto aggregateProgrammeMembershipDto, Programme programme);

  /**
   * Map a record data map to a ProgrammeMembership.
   *
   * @param recordData The map to convert.
   * @return The mapped ProgrammeMembership.
   */
  ProgrammeMembership toEntity(Map<String, String> recordData);

  /**
   * Convert a ProgrammeMembership to a Record.
   *
   * @param programmeMembership The ProgrammeMembership to map.
   * @return The mapped Record.
   */
  default Record toRecord(ProgrammeMembership programmeMembership) {
    Record programmeMembershipRecord = new Record();

    ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    Map<String, String> recordData = objectMapper.convertValue(programmeMembership,
        new TypeReference<>() {
        });

    programmeMembershipRecord.setData(recordData);

    UUID uuid = programmeMembership.getUuid();
    programmeMembershipRecord.setTisId(uuid == null ? null : uuid.toString());
    return programmeMembershipRecord;
  }

  /**
   * Convert a ProgrammeMembershipDto to a Record.
   *
   * @param aggregateProgrammeMembershipDto The ProgrammeMembershipDto to map.
   * @return The mapped Record.
   */
  default Record toRecord(AggregateProgrammeMembershipDto aggregateProgrammeMembershipDto) {
    Record programmeMembershipRecord = new Record();

    ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // TODO: hackity hack part 1
    List<AggregateCurriculumDto> curricula = aggregateProgrammeMembershipDto.getCurricula();
    aggregateProgrammeMembershipDto.setCurricula(null);

    Map<String, String> recordData = objectMapper.convertValue(aggregateProgrammeMembershipDto,
        new TypeReference<>() {
        });

    try {
      // TODO: hackity hack part 2
      aggregateProgrammeMembershipDto.setCurricula(curricula);
      recordData.put("curricula", objectMapper.writeValueAsString(curricula));
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }

    programmeMembershipRecord.setData(recordData);
    programmeMembershipRecord.setTisId(aggregateProgrammeMembershipDto.getTisId());
    return programmeMembershipRecord;
  }

  /**
   * Convert multiple ProgrammeMemberships to Records.
   *
   * @param programmeMemberships The ProgrammeMemberships to map.
   * @return The mapped Records.
   */
  Set<Record> toRecords(Set<ProgrammeMembership> programmeMemberships);

  @BeforeMapping
  default void stripNullMapValues(Map<String, String> recordData) {
    recordData.values().removeIf(Objects::isNull);
  }
}

/*
 * The MIT License (MIT)
 *
 *  Copyright 2023 Crown Copyright (Health Education England)
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 *  and associated documentation files (the "Software"), to deal in the Software without
 *  restriction, including without limitation the rights to use, copy, modify, merge, publish,
 *  distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all copies or
 *  substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 *  BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 *  DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package uk.nhs.hee.tis.trainee.sync.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants.ComponentModel;
import org.mapstruct.ReportingPolicy;
import uk.nhs.hee.tis.trainee.sync.dto.AggregateProgrammeMembershipDto;
import uk.nhs.hee.tis.trainee.sync.dto.ProgrammeMembershipEventDto;
import uk.nhs.hee.tis.trainee.sync.model.ConditionsOfJoining;
import uk.nhs.hee.tis.trainee.sync.model.Record;

/**
 * A mapper for creating programme membership event DTOs.
 */
@Mapper(componentModel = ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProgrammeMembershipEventMapper {

  /**
   * Create a programme membership event DTO.
   *
   * @param programmeMembership The aggregate programme membership to wrap.
   * @return The programme membership event DTO.
   */
  @Mapping(target = "programmeMembership", source = "programmeMembership")
  ProgrammeMembershipEventDto toProgrammeMembershipEventDto(
      AggregateProgrammeMembershipDto programmeMembership);

  /**
   * Convert a ProgrammeMembershipEventDto to a Record.
   *
   * @param programmeMembershipEventDto The ProgrammeMembershipEventDto to map.
   * @return The mapped Record.
   */
  default Record toRecord(ProgrammeMembershipEventDto programmeMembershipEventDto) {
    AggregateMapper aggregateMapper = new AggregateMapperImpl();
    return aggregateMapper.toRecord(programmeMembershipEventDto.getProgrammeMembership());
  }

  /**
   * Convert an AggregateProgrammeMembershipDto to a Record.
   *
   * @param aggregateProgrammeMembership The AggregateProgrammeMembershipDto to map.
   * @return The mapped Record.
   */
  default Record toRecord(AggregateProgrammeMembershipDto aggregateProgrammeMembership) {
    return toRecord(toProgrammeMembershipEventDto(aggregateProgrammeMembership));
  }
}

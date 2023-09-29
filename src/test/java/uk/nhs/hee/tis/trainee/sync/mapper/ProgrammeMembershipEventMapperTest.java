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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.nhs.hee.tis.trainee.sync.dto.AggregateProgrammeMembershipDto;
import uk.nhs.hee.tis.trainee.sync.dto.ProgrammeMembershipEventDto;
import uk.nhs.hee.tis.trainee.sync.model.Record;

class ProgrammeMembershipEventMapperTest {
  private static final String TIS_ID = "123";

  private ProgrammeMembershipEventMapper mapper;
  private AggregateMapper aggregateMapper;

  @BeforeEach
  void setUp() {
    mapper = new ProgrammeMembershipEventMapperImpl();
    aggregateMapper = mock(AggregateMapper.class);
  }

  @Test
  void shouldMapAggregateProgrammeMembershipToRecord() {
    AggregateProgrammeMembershipDto programmeMembership = new AggregateProgrammeMembershipDto();
    programmeMembership.setTisId(TIS_ID);

    Record recrd = new Record();
    recrd.setTisId(TIS_ID);
    when(aggregateMapper.toRecord(any())).thenReturn(recrd);

    Record returnedRecord = mapper.toRecord(programmeMembership);

    assertThat("Unexpected record Tis Id.", returnedRecord.getTisId(),
        is(TIS_ID));
  }

  @Test
  void shouldMapProgrammeMembershipEventDtoToRecord() {
    ProgrammeMembershipEventDto pmEventDto = new ProgrammeMembershipEventDto();
    AggregateProgrammeMembershipDto aggregatePmDto = new AggregateProgrammeMembershipDto();
    aggregatePmDto.setTisId(TIS_ID);
    pmEventDto.setProgrammeMembership(aggregatePmDto);

    Record recrd = new Record();
    recrd.setTisId(TIS_ID);
    when(aggregateMapper.toRecord(any())).thenReturn(recrd);

    Record returnedRecord = mapper.toRecord(pmEventDto);
    assertThat("Unexpected record Tis Id.", returnedRecord.getTisId(),
        is(TIS_ID));
  }
}

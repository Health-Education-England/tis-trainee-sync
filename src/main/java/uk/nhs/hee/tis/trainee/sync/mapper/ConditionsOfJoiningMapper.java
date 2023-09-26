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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.Map;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants.ComponentModel;
import uk.nhs.hee.tis.trainee.sync.model.ConditionsOfJoining;
import uk.nhs.hee.tis.trainee.sync.model.Record;

/**
 * A mapper to convert between ConditionsOfJoining data types.
 */
@Mapper(componentModel = ComponentModel.SPRING)
public interface ConditionsOfJoiningMapper {

  /**
   * Map a record data map to a ConditionsOfJoining.
   *
   * @param recordData The map to convert.
   * @return The mapped ConditionsOfJoining.
   */
  ConditionsOfJoining toEntity(Map<String, String> recordData);

  /**
   * Convert a Conditions of Joining to a Record.
   *
   * @param conditionsOfJoining The Conditions of Joining to map.
   * @return The mapped Record.
   */
  default Record toRecord(ConditionsOfJoining conditionsOfJoining) {
    ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    Map<String, String> recordData = objectMapper.convertValue(conditionsOfJoining,
        new TypeReference<>() {
        });

    Record cojRecord = new Record();
    cojRecord.setData(recordData);
    cojRecord.setTisId(conditionsOfJoining.getProgrammeMembershipUuid());
    return cojRecord;
  }
}

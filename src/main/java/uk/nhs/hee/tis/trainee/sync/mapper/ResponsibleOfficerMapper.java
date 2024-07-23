/*
 * The MIT License (MIT)
 *
 * Copyright 2024 Crown Copyright (Health Education England)
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
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.model.ResponsibleOfficer;

/**
 * A mapper to convert between ResponsibleOfficer data types.
 */
@Mapper(componentModel = ComponentModel.SPRING)
public interface ResponsibleOfficerMapper {

  /**
   * Map a record data map to a ResponsibleOfficer.
   *
   * @param recordData The map to convert.
   * @return The mapped ResponsibleOfficer.
   */
  ResponsibleOfficer toEntity(Map<String, String> recordData);

  /**
   * Convert a ResponsibleOfficer to a Record.
   *
   * @param responsibleOfficer The Responsible Officer to map.
   * @return The mapped Record.
   */
  default Record toRecord(ResponsibleOfficer responsibleOfficer) {
    ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    Map<String, String> recordData = objectMapper.convertValue(responsibleOfficer,
        new TypeReference<>() {
        });

    Record roRecord = new Record();
    roRecord.setData(recordData);
    roRecord.setTisId(responsibleOfficer.getProgrammeMembershipUuid());
    return roRecord;
  }
}

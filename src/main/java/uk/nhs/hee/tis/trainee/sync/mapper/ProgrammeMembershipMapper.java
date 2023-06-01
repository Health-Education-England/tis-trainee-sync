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
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.mapstruct.BeforeMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants.ComponentModel;
import uk.nhs.hee.tis.trainee.sync.model.ProgrammeMembership;
import uk.nhs.hee.tis.trainee.sync.model.Record;

/**
 * A mapper to convert between ProgrammeMembership data types.
 */
@Mapper(componentModel = ComponentModel.SPRING)
public interface ProgrammeMembershipMapper {

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

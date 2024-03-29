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

package uk.nhs.hee.tis.trainee.sync.mapper.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Map;
import org.mapstruct.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class RecordUtil {

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Operation {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface RecordType {

  }

  /**
   * Finds the model Operator enum value from the metadata.
   *
   * @param metadata the metadata map
   * @return the model Operator value
   */
  @Operation
  public uk.nhs.hee.tis.trainee.sync.model.Operation operation(Map<String, String> metadata) {
    String operationString = metadata.get("operation");
    var operation = uk.nhs.hee.tis.trainee.sync.model.Operation.fromString(operationString);

    if (operation == null) {
      throw new IllegalArgumentException(
          String.format("Unhandled record operation '%s'.", operationString));
    }

    return operation;
  }

  /**
   * Finds the RecordType enum value from the metadata.
   *
   * @param metadata the metadata map
   * @return the RecordType value
   */
  @RecordType
  public uk.nhs.hee.tis.trainee.sync.model.RecordType recordType(Map<String, String> metadata) {
    String recordType = metadata.get("record-type");
    return Arrays.stream(uk.nhs.hee.tis.trainee.sync.model.RecordType.values())
        .filter(e -> e.name().equalsIgnoreCase(recordType))
        .findAny()
        .orElseThrow(() -> new IllegalArgumentException(
            String.format("Unhandled record type '%s'.", recordType)));
  }
}

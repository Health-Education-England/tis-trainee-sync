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
import java.util.Map;
import org.mapstruct.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class ReferenceUtil {

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Label {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Status {

  }

  /**
   * Get the label value from the data.
   *
   * @param data The record data to retrieve the label from.
   * @return The label from the data record, or the name if no label is available.
   */
  @Label
  public String label(Map<String, String> data) {
    // Fall back to name if label is not found in the data map, to support types like College.
    return data.getOrDefault("label", data.get("name"));
  }

  /**
   * Get the status value from the data.
   *
   * @param data The record data to retrieve the status from.
   * @return The status from the data record, or null if the status is not a valid status.
   */
  @Status
  public uk.nhs.hee.tis.trainee.sync.dto.Status status(Map<String, String> data) {
    try {
      return uk.nhs.hee.tis.trainee.sync.dto.Status.valueOf(data.get("status"));
    } catch (NullPointerException e) {
      return null;
    }
  }
}

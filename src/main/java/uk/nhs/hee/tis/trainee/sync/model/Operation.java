/*
 * The MIT License (MIT)
 *
 * Copyright 2021 Crown Copyright (Health Education England)
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

package uk.nhs.hee.tis.trainee.sync.model;

public enum Operation {
  UPDATE("update"),
  INSERT("insert"),
  DELETE("delete"),
  LOAD("load"),
  DROP_TABLE("drop-table"),
  CREATE_TABLE("create-table");


  private final String key;

  Operation(String key) {
    this.key = key;
  }

  /**
   * Gets the Operation from a string representation.
   *
   * @param key The string to match to an Operation.
   * @return The matched Operation, or null if no match found.
   */
  public static Operation fromString(String key) {
    for (Operation operation : values()) {
      if (operation.key.equalsIgnoreCase(key)) {
        return operation;
      }
    }
    return null;
  }
}

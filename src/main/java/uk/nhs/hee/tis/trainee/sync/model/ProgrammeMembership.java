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

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
public class ProgrammeMembership {

  public static final String ENTITY_NAME = "ProgrammeMembership";

  @Id
  private UUID uuid;
  private String programmeMembershipType;
  private LocalDate programmeStartDate;
  private LocalDate programmeEndDate;
  private Long programmeId;
  private Long trainingNumberId;
  private Long personId;
  private String rotation;
  private Long rotationId;
  private String trainingPathway;
  private String leavingReason;
  /* legacy */ private String leavingDestination;
  private Instant amendedDate;
}

/*
 * The MIT License (MIT)
 *
 *  Copyright 2023 Crown Copyright (Health Education England)
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 *  associated documentation files (the "Software"), to deal in the Software without restriction,
 *  including without limitation the rights to use, copy, modify, merge, publish, distribute,
 *  sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all copies or
 *  substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 *  NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 *  DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package uk.nhs.hee.tis.trainee.sync.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.time.Instant;

/**
 * A Programme Membership event.
 *
 * @param traineeId           The trainee associated with the Programme membership.
 * @param managingDeanery     The programme owner.
 * @param conditionsOfJoining The Programme Membership's Conditions of Joining.
 */
public record ProgrammeMembershipEventDto(
    @JsonAlias("traineeTisId")
    String traineeId,
    String managingDeanery,
    ConditionsOfJoining conditionsOfJoining) {

  /**
   * A Programme Membership's Conditions of Joining details.
   *
   * @param syncedAt The timestamp for the signed Conditions of Joining being received.
   */
  public record ConditionsOfJoining(Instant syncedAt) {

  }
}

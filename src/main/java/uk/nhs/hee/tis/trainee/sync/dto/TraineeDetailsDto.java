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

package uk.nhs.hee.tis.trainee.sync.dto;

import java.time.LocalDate;
import lombok.Data;

@Data
public class TraineeDetailsDto {

  // Common field.
  private String tisId;

  // Person fields.
  private String traineeTisId;
  private String publicHealthNumber;

  // ContactDetails fields.
  private String title;
  private String forenames;
  private String knownAs;
  private String surname;
  private String maidenName;
  private String telephoneNumber;
  private String mobileNumber;
  private String email;
  private String address1;
  private String address2;
  private String address3;
  private String address4;
  private String postCode;

  // PersonalDetails fields.
  private String dateOfBirth;
  private String gender;

  // GdcDetails fields.
  private String gdcNumber;
  private String gdcStatus;

  // GmcDetails fields.
  private String gmcNumber;
  private String gmcStatus;

  // PersonOwner fields.
  private String personOwner;

  // Qualification fields.
  private String qualification;
  private LocalDate dateAttained;
  private String medicalSchool;
}

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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TraineeDetailsUtil {

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface PersonId {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Title {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Forenames {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface KnownAs {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Surname {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface MaidenName {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface TelephoneNumber {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface MobileNumber {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Email {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Address1 {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Address2 {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Address3 {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Address4 {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface PostCode {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface DateOfBirth {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Gender {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface PublicHealthNumber {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface GdcNumber {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface GdcStatus {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface GmcNumber {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface GmcStatus {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Owner {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Qualification {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface DateAttained {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface MedicalSchool {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface StartDate {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface EndDate {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Grade {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface PlacementType {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Status {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface TraineeId {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface ProgrammeMembershipType {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface ProgrammeName {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface ProgrammeNumber {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface ProgrammeTisId {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface ManagingDeanery {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Curricula {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface CurriculumEndDate {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface CurriculumName {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface CurriculumSubType {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface CurriculumStartDate {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface ProgrammeStartDate {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface ProgrammeEndDate {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface ProgrammeCompletionDate {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface EmployingBodyName {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface TrainingBodyName {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Site {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface SiteLocation {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Specialty {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface PlacementWholeTimeEquivalent {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface SpecialtyId {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface PlacementId {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface PlacementSpecialtyType {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface SpecialtyName {

  }

  @PersonId
  public String personId(Map<String, String> data) {
    return data.get("personId");
  }

  @Title
  public String title(Map<String, String> data) {
    return data.get("title");
  }

  @Forenames
  public String forenames(Map<String, String> data) {
    return data.get("forenames");
  }

  @KnownAs
  public String knownAs(Map<String, String> data) {
    return data.get("knownAs");
  }

  @Surname
  public String surname(Map<String, String> data) {
    return data.get("surname");
  }

  @MaidenName
  public String maidenName(Map<String, String> data) {
    return data.get("maidenName");
  }

  @TelephoneNumber
  public String telephoneNumber(Map<String, String> data) {
    return data.get("telephoneNumber");
  }

  @MobileNumber
  public String mobileNumber(Map<String, String> data) {
    return data.get("mobileNumber");
  }

  @Email
  public String email(Map<String, String> data) {
    return data.get("email");
  }

  @Address1
  public String address1(Map<String, String> data) {
    return data.get("address1");
  }

  @Address2
  public String address2(Map<String, String> data) {
    return data.get("address2");
  }

  @Address3
  public String address3(Map<String, String> data) {
    return data.get("address3");
  }

  @Address4
  public String address4(Map<String, String> data) {
    return data.get("address4");
  }

  @PostCode
  public String postCode(Map<String, String> data) {
    return data.get("postCode");
  }

  @DateOfBirth
  public String dateOfBirth(Map<String, String> data) {
    return data.get("dateOfBirth");
  }

  @Gender
  public String gender(Map<String, String> data) {
    return data.get("gender");
  }

  @PublicHealthNumber
  public String publicHealthNumber(Map<String, String> data) {
    return data.get("publicHealthNumber");
  }

  @GdcNumber
  public String gdcNumber(Map<String, String> data) {
    return data.get("gdcNumber");
  }

  @GdcStatus
  public String gdcStatus(Map<String, String> data) {
    return data.get("gdcStatus");
  }

  @GmcNumber
  public String gmcNumber(Map<String, String> data) {
    return data.get("gmcNumber");
  }

  @GmcStatus
  public String gmcStatus(Map<String, String> data) {
    return data.get("gmcStatus");
  }

  @Owner
  public String owner(Map<String, String> data) {
    return data.get("owner");
  }

  @Qualification
  public String qualification(Map<String, String> data) {
    return data.get("qualification");
  }

  @DateAttained
  public LocalDate dateAttained(Map<String, String> data) {
    String dateAttained = data.get("qualificationAttainedDate");
    return dateAttained == null ? null : LocalDate.parse(dateAttained);
  }

  @MedicalSchool
  public String medicalSchool(Map<String, String> data) {
    return data.get("medicalSchool");
  }

  @StartDate
  public LocalDate startDate(Map<String, String> data) {
    String startDate = data.get("dateFrom");
    return startDate == null ? null : LocalDate.parse(startDate);
  }

  @EndDate
  public LocalDate endDate(Map<String, String> data) {
    String endDate = data.get("dateTo");
    return endDate == null ? null : LocalDate.parse(endDate);
  }

  @Grade
  public String grade(Map<String, String> data) {
    return data.get("gradeAbbreviation");
  }

  @PlacementType
  public String placementType(Map<String, String> data) {
    return data.get("placementType");
  }

  @Status
  public String status(Map<String, String> data) {
    return data.get("status");
  }

  @TraineeId
  public String traineeId(Map<String, String> data) {
    return data.get("traineeId");
  }

  @ProgrammeMembershipType
  public String programmeMembershipType(Map<String, String> data) {
    return data.get("programmeMembershipType");
  }

  @ProgrammeName
  public String programmeName(Map<String, String> data) {
    return data.get("programmeName");
  }

  @ProgrammeNumber
  public String programmeNumber(Map<String, String> data) {
    return data.get("programmeNumber");
  }

  @ProgrammeTisId
  public String programmeTisId(Map<String, String> data) {
    return data.get("programmeId");
  }

  @ManagingDeanery
  public String managingDeanery(Map<String, String> data) {
    return data.get("managingDeanery");
  }

  /**
   * Gets the programme completion date from curriculum end date.
   *
   * @param data the data containing curriculumEndDate
   * @return the programme completion date
   */
  @ProgrammeCompletionDate
  public LocalDate programmeCompletionDate(Map<String, String> data) {
    // note that the consolidated programmeCompletionDate value in TISSS will be
    // max(programmeCompletionDate)
    String programmeCompletionDate = data.get("curriculumEndDate");
    return programmeCompletionDate == null ? null : LocalDate.parse(programmeCompletionDate);
  }

  /**
   * Gets the set of curricula from the data map String.
   *
   * @param data the data containing the curricula as a string
   * @return the curricula
   */
  @Curricula
  public Set<Map<String,String>> curricula(Map<String, String> data) {

    ObjectMapper mapper = new ObjectMapper();

    Set<Map<String, String>> curricula = new HashSet<>();
    if (data.get("curricula") != null) {
      try {
        curricula = mapper.readValue(data.get("curricula"), new TypeReference<>() {
        });
      } catch (JsonProcessingException e) {
        log.error("Badly formed curricula JSON in {}", data);
      }
    }

    return curricula;
  }

  @CurriculumEndDate
  public LocalDate curriculumEndDate(Map<String, String> data) {
    String curriculumEndDate = data.get("curriculumEndDate");
    return curriculumEndDate == null ? null : LocalDate.parse(curriculumEndDate);
  }

  @CurriculumName
  public String curriculumName(Map<String, String> data) {
    return data.get("name");
  }

  @CurriculumSubType
  public String curriculumSubType(Map<String, String> data) {
    return data.get("curriculumSubType");
  }

  @CurriculumStartDate
  public LocalDate curriculumStartDate(Map<String, String> data) {
    String curriculumStartDate = data.get("curriculumStartDate");
    return curriculumStartDate == null ? null : LocalDate.parse(curriculumStartDate);
  }

  @ProgrammeStartDate
  public LocalDate programmeStartDate(Map<String, String> data) {
    String programmeStartDate = data.get("programmeStartDate");
    return programmeStartDate == null ? null : LocalDate.parse(programmeStartDate);
  }

  @ProgrammeEndDate
  public LocalDate programmeEndDate(Map<String, String> data) {
    String programmeEndDate = data.get("programmeEndDate");
    return programmeEndDate == null ? null : LocalDate.parse(programmeEndDate);
  }

  @EmployingBodyName
  public String employingBodyName(Map<String, String> data) {
    return data.get("employingBodyName");
  }

  @TrainingBodyName
  public String trainingBodyName(Map<String, String> data) {
    return data.get("trainingBodyName");
  }

  @Site
  public String site(Map<String, String> data) {
    return data.get("site");
  }

  @SiteLocation
  public String siteLocation(Map<String, String> data) {
    return data.get("siteLocation");
  }

  @Specialty
  public String specialty(Map<String, String> data) {
    return data.get("specialty");
  }

  @PlacementWholeTimeEquivalent
  public String placementWholeTimeEquivalent(Map<String, String> data) {
    return data.get("placementWholeTimeEquivalent");
  }

  @SpecialtyId
  public String specialtyId(Map<String, String> data) {
    return data.get("specialtyId");
  }

  @PlacementId
  public String placementId(Map<String, String> data) {
    return data.get("placementId");
  }

  @PlacementSpecialtyType
  public String placementSpecialtyType(Map<String, String> data) {
    return data.get("placementSpecialtyType");
  }

  @SpecialtyName
  public String specialtyName(Map<String, String> data) {
    return data.get("name");
  }
}

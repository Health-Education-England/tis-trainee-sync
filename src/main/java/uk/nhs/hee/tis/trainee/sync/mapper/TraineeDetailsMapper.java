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

package uk.nhs.hee.tis.trainee.sync.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import uk.nhs.hee.tis.trainee.sync.dto.TraineeDetailsDto;
import uk.nhs.hee.tis.trainee.sync.mapper.util.TraineeDetailsUtil;
import uk.nhs.hee.tis.trainee.sync.mapper.util.TraineeDetailsUtil.ConditionsOfJoining;
import uk.nhs.hee.tis.trainee.sync.mapper.util.TraineeDetailsUtil.Curricula;
import uk.nhs.hee.tis.trainee.sync.mapper.util.TraineeDetailsUtil.OtherSites;
import uk.nhs.hee.tis.trainee.sync.mapper.util.TraineeDetailsUtil.OtherSpecialties;
import uk.nhs.hee.tis.trainee.sync.mapper.util.TraineeDetailsUtil.WholeTimeEquivalent;
import uk.nhs.hee.tis.trainee.sync.model.Record;

@Mapper(componentModel = "spring", uses = TraineeDetailsUtil.class,
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TraineeDetailsMapper {

  @Mapping(target = "tisId", ignore = true)
  @Mapping(target = "traineeTisId", source = "tisId")
  @Mapping(target = "publicHealthNumber", source = "data.publicHealthNumber")
  TraineeDetailsDto toBasicDetailsDto(Record recrd);

  @Mapping(target = "tisId", ignore = true)
  @Mapping(target = "traineeTisId", source = "tisId")
  @Mapping(target = "title", source = "data.title")
  @Mapping(target = "forenames", source = "data.forenames")
  @Mapping(target = "knownAs", source = "data.knownAs")
  @Mapping(target = "surname", source = "data.surname")
  @Mapping(target = "maidenName", source = "data.maidenName")
  @Mapping(target = "telephoneNumber", source = "data.telephoneNumber")
  @Mapping(target = "mobileNumber", source = "data.mobileNumber")
  @Mapping(target = "email", source = "data.email")
  @Mapping(target = "address1", source = "data.address1")
  @Mapping(target = "address2", source = "data.address2")
  @Mapping(target = "address3", source = "data.address3")
  @Mapping(target = "address4", source = "data.address4")
  @Mapping(target = "postCode", source = "data.postCode")
  TraineeDetailsDto toContactDetails(Record recrd);

  @Mapping(target = "tisId", ignore = true)
  @Mapping(target = "traineeTisId", source = "tisId")
  @Mapping(target = "gdcNumber", source = "data.gdcNumber")
  @Mapping(target = "gdcStatus", source = "data.gdcStatus")
  TraineeDetailsDto toGdcDetailsDto(Record recrd);

  @Mapping(target = "tisId", ignore = true)
  @Mapping(target = "traineeTisId", source = "tisId")
  @Mapping(target = "gmcNumber", source = "data.gmcNumber")
  @Mapping(target = "gmcStatus", source = "data.gmcStatus")
  TraineeDetailsDto toGmcDetailsDto(Record recrd);

  @Mapping(target = "tisId", ignore = true)
  @Mapping(target = "traineeTisId", source = "tisId")
  @Mapping(target = "personOwner", source = "data.owner")
  TraineeDetailsDto toPersonOwnerDto(Record recrd);

  @Mapping(target = "tisId", ignore = true)
  @Mapping(target = "traineeTisId", source = "tisId")
  @Mapping(target = "dateOfBirth", source = "data.dateOfBirth")
  @Mapping(target = "gender", source = "data.gender")
  TraineeDetailsDto toPersonalInfoDto(Record recrd);

  @Mapping(target = "traineeTisId", source = "data.personId")
  @Mapping(target = "qualification", source = "data.qualification")
  @Mapping(target = "dateAttained", source = "data.qualificationAttainedDate")
  @Mapping(target = "medicalSchool", source = "data.medicalSchool")
  TraineeDetailsDto toQualificationDto(Record recrd);

  @Mapping(target = "traineeTisId", source = "data.traineeId")
  @Mapping(target = "startDate", source = "data.dateFrom")
  @Mapping(target = "endDate", source = "data.dateTo")
  @Mapping(target = "grade", source = "data.gradeAbbreviation")
  @Mapping(target = "placementType", source = "data.placementType")
  @Mapping(target = "status", source = "data.status")
  @Mapping(target = "employingBody", source = "data.employingBodyName")
  @Mapping(target = "trainingBody", source = "data.trainingBodyName")
  @Mapping(target = "site", source = "data.site")
  @Mapping(target = "siteLocation", source = "data.siteLocation")
  @Mapping(target = "siteKnownAs", source = "data.siteKnownAs")
  @Mapping(target = "otherSites", source = "data", qualifiedBy = OtherSites.class)
  @Mapping(target = "specialty", source = "data.specialty")
  @Mapping(target = "subSpecialty", source = "data.subSpecialty")
  @Mapping(target = "postAllowsSubspecialty", source = "data.postAllowsSubspecialty")
  @Mapping(target = "otherSpecialties", source = "data", qualifiedBy = OtherSpecialties.class)
  @Mapping(target = "wholeTimeEquivalent", source = "data", qualifiedBy = WholeTimeEquivalent.class)
  TraineeDetailsDto toPlacementDto(Record recrd);

  @Mapping(target = "traineeTisId", source = "data.personId")
  @Mapping(target = "startDate", source = "data.programmeStartDate")
  @Mapping(target = "endDate", source = "data.programmeEndDate")
  @Mapping(target = "programmeMembershipType", source = "data.programmeMembershipType")
  @Mapping(target = "programmeName", source = "data.programmeName")
  @Mapping(target = "programmeNumber", source = "data.programmeNumber")
  @Mapping(target = "programmeTisId", source = "data.programmeId")
  @Mapping(target = "managingDeanery", source = "data.managingDeanery")
  @Mapping(target = "designatedBody", source = "data.designatedBody")
  @Mapping(target = "programmeCompletionDate", source = "data.curriculumEndDate")
  @Mapping(target = "curricula", source = "data", qualifiedBy = Curricula.class)
  @Mapping(target = "conditionsOfJoining", source = "data", qualifiedBy = ConditionsOfJoining.class)
  TraineeDetailsDto toProgrammeMembershipDto(Record recrd);

  @Mapping(target = "tisId")
  @Mapping(target = "traineeTisId", source = "data.personId")
  @Mapping(target = "startDate", source = "data.startDate")
  @Mapping(target = "endDate", source = "data.endDate")
  @Mapping(target = "programmeMembershipType", source = "data.programmeMembershipType")
  @Mapping(target = "programmeName", source = "data.programmeName")
  @Mapping(target = "programmeNumber", source = "data.programmeNumber")
  @Mapping(target = "programmeTisId", source = "data.programmeTisId")
  @Mapping(target = "managingDeanery", source = "data.managingDeanery")
  @Mapping(target = "designatedBody", source = "data.designatedBody")
  @Mapping(target = "programmeCompletionDate", source = "data.programmeCompletionDate")
  @Mapping(target = "trainingPathway", source = "data.trainingPathway")
  @Mapping(target = "curricula", source = "data", qualifiedBy = Curricula.class)
  @Mapping(target = "conditionsOfJoining", source = "data", qualifiedBy = ConditionsOfJoining.class)
  TraineeDetailsDto toAggregateProgrammeMembershipDto(Record recrd);

  @Mapping(target = "curriculumName", source = "data.name")
  @Mapping(target = "curriculumSubType", source = "data.curriculumSubType")
  @Mapping(target = "curriculumStartDate", source = "data.curriculumStartDate")
  @Mapping(target = "curriculumEndDate", source = "data.curriculumEndDate")
  TraineeDetailsDto toCurriculumDto(Record recrd);
}

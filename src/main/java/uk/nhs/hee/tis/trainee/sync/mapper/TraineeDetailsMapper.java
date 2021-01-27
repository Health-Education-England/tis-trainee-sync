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
import uk.nhs.hee.tis.trainee.sync.mapper.util.TraineeDetailsUtil.Address1;
import uk.nhs.hee.tis.trainee.sync.mapper.util.TraineeDetailsUtil.Address2;
import uk.nhs.hee.tis.trainee.sync.mapper.util.TraineeDetailsUtil.Address3;
import uk.nhs.hee.tis.trainee.sync.mapper.util.TraineeDetailsUtil.Address4;
import uk.nhs.hee.tis.trainee.sync.mapper.util.TraineeDetailsUtil.DateAttained;
import uk.nhs.hee.tis.trainee.sync.mapper.util.TraineeDetailsUtil.DateOfBirth;
import uk.nhs.hee.tis.trainee.sync.mapper.util.TraineeDetailsUtil.Email;
import uk.nhs.hee.tis.trainee.sync.mapper.util.TraineeDetailsUtil.EmployingBodyName;
import uk.nhs.hee.tis.trainee.sync.mapper.util.TraineeDetailsUtil.EndDate;
import uk.nhs.hee.tis.trainee.sync.mapper.util.TraineeDetailsUtil.Forenames;
import uk.nhs.hee.tis.trainee.sync.mapper.util.TraineeDetailsUtil.GdcNumber;
import uk.nhs.hee.tis.trainee.sync.mapper.util.TraineeDetailsUtil.GdcStatus;
import uk.nhs.hee.tis.trainee.sync.mapper.util.TraineeDetailsUtil.Gender;
import uk.nhs.hee.tis.trainee.sync.mapper.util.TraineeDetailsUtil.GmcNumber;
import uk.nhs.hee.tis.trainee.sync.mapper.util.TraineeDetailsUtil.GmcStatus;
import uk.nhs.hee.tis.trainee.sync.mapper.util.TraineeDetailsUtil.Grade;
import uk.nhs.hee.tis.trainee.sync.mapper.util.TraineeDetailsUtil.KnownAs;
import uk.nhs.hee.tis.trainee.sync.mapper.util.TraineeDetailsUtil.MaidenName;
import uk.nhs.hee.tis.trainee.sync.mapper.util.TraineeDetailsUtil.MedicalSchool;
import uk.nhs.hee.tis.trainee.sync.mapper.util.TraineeDetailsUtil.MobileNumber;
import uk.nhs.hee.tis.trainee.sync.mapper.util.TraineeDetailsUtil.Owner;
import uk.nhs.hee.tis.trainee.sync.mapper.util.TraineeDetailsUtil.PersonId;
import uk.nhs.hee.tis.trainee.sync.mapper.util.TraineeDetailsUtil.PlacementType;
import uk.nhs.hee.tis.trainee.sync.mapper.util.TraineeDetailsUtil.PostCode;
import uk.nhs.hee.tis.trainee.sync.mapper.util.TraineeDetailsUtil.ProgrammeMembershipType;
import uk.nhs.hee.tis.trainee.sync.mapper.util.TraineeDetailsUtil.PublicHealthNumber;
import uk.nhs.hee.tis.trainee.sync.mapper.util.TraineeDetailsUtil.Qualification;
import uk.nhs.hee.tis.trainee.sync.mapper.util.TraineeDetailsUtil.StartDate;
import uk.nhs.hee.tis.trainee.sync.mapper.util.TraineeDetailsUtil.Status;
import uk.nhs.hee.tis.trainee.sync.mapper.util.TraineeDetailsUtil.Surname;
import uk.nhs.hee.tis.trainee.sync.mapper.util.TraineeDetailsUtil.TelephoneNumber;
import uk.nhs.hee.tis.trainee.sync.mapper.util.TraineeDetailsUtil.Title;
import uk.nhs.hee.tis.trainee.sync.mapper.util.TraineeDetailsUtil.TraineeId;
import uk.nhs.hee.tis.trainee.sync.mapper.util.TraineeDetailsUtil.TrainingBodyName;
import uk.nhs.hee.tis.trainee.sync.model.Record;

@Mapper(componentModel = "spring", uses = TraineeDetailsUtil.class,
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TraineeDetailsMapper {

  @Mapping(target = "tisId", ignore = true)
  @Mapping(target = "traineeTisId", source = "tisId")
  @Mapping(target = "publicHealthNumber", source = "data", qualifiedBy = PublicHealthNumber.class)
  TraineeDetailsDto toBasicDetailsDto(Record record);

  @Mapping(target = "tisId", ignore = true)
  @Mapping(target = "traineeTisId", source = "tisId")
  @Mapping(target = "title", source = "data", qualifiedBy = Title.class)
  @Mapping(target = "forenames", source = "data", qualifiedBy = Forenames.class)
  @Mapping(target = "knownAs", source = "data", qualifiedBy = KnownAs.class)
  @Mapping(target = "surname", source = "data", qualifiedBy = Surname.class)
  @Mapping(target = "maidenName", source = "data", qualifiedBy = MaidenName.class)
  @Mapping(target = "telephoneNumber", source = "data", qualifiedBy = TelephoneNumber.class)
  @Mapping(target = "mobileNumber", source = "data", qualifiedBy = MobileNumber.class)
  @Mapping(target = "email", source = "data", qualifiedBy = Email.class)
  @Mapping(target = "address1", source = "data", qualifiedBy = Address1.class)
  @Mapping(target = "address2", source = "data", qualifiedBy = Address2.class)
  @Mapping(target = "address3", source = "data", qualifiedBy = Address3.class)
  @Mapping(target = "address4", source = "data", qualifiedBy = Address4.class)
  @Mapping(target = "postCode", source = "data", qualifiedBy = PostCode.class)
  TraineeDetailsDto toContactDetails(Record record);

  @Mapping(target = "tisId", ignore = true)
  @Mapping(target = "traineeTisId", source = "tisId")
  @Mapping(target = "gdcNumber", source = "data", qualifiedBy = GdcNumber.class)
  @Mapping(target = "gdcStatus", source = "data", qualifiedBy = GdcStatus.class)
  TraineeDetailsDto toGdcDetailsDto(Record record);

  @Mapping(target = "tisId", ignore = true)
  @Mapping(target = "traineeTisId", source = "tisId")
  @Mapping(target = "gmcNumber", source = "data", qualifiedBy = GmcNumber.class)
  @Mapping(target = "gmcStatus", source = "data", qualifiedBy = GmcStatus.class)
  TraineeDetailsDto toGmcDetailsDto(Record record);

  @Mapping(target = "tisId", ignore = true)
  @Mapping(target = "traineeTisId", source = "tisId")
  @Mapping(target = "personOwner", source = "data", qualifiedBy = Owner.class)
  TraineeDetailsDto toPersonOwnerDto(Record record);

  @Mapping(target = "tisId", ignore = true)
  @Mapping(target = "traineeTisId", source = "tisId")
  @Mapping(target = "dateOfBirth", source = "data", qualifiedBy = DateOfBirth.class)
  @Mapping(target = "gender", source = "data", qualifiedBy = Gender.class)
  TraineeDetailsDto toPersonalInfoDto(Record record);

  @Mapping(target = "traineeTisId", source = "data", qualifiedBy = PersonId.class)
  @Mapping(target = "qualification", source = "data", qualifiedBy = Qualification.class)
  @Mapping(target = "dateAttained", source = "data", qualifiedBy = DateAttained.class)
  @Mapping(target = "medicalSchool", source = "data", qualifiedBy = MedicalSchool.class)
  TraineeDetailsDto toQualificationDto(Record record);

  @Mapping(target = "traineeTisId", source = "data", qualifiedBy = TraineeId.class)
  @Mapping(target = "startDate", source = "data", qualifiedBy = StartDate.class)
  @Mapping(target = "endDate", source = "data", qualifiedBy = EndDate.class)
  @Mapping(target = "grade", source = "data", qualifiedBy = Grade.class)
  @Mapping(target = "placementType", source = "data", qualifiedBy = PlacementType.class)
  @Mapping(target = "status", source = "data", qualifiedBy = Status.class)
  @Mapping(target = "employingBody", source = "data", qualifiedBy = EmployingBodyName.class)
  @Mapping(target = "trainingBody", source = "data", qualifiedBy = TrainingBodyName.class)
  TraineeDetailsDto toPlacementDto(Record record);

  @Mapping(target = "traineeTisId", source = "data", qualifiedBy = PersonId.class)
  @Mapping(target = "startDate", source = "data", qualifiedBy = StartDate.class)
  @Mapping(target = "endDate", source = "data", qualifiedBy = EndDate.class)
  @Mapping(target = "programmeMembershipType", source = "data", qualifiedBy = ProgrammeMembershipType.class)
  TraineeDetailsDto toProgrammeMembershipDto(Record record);
}

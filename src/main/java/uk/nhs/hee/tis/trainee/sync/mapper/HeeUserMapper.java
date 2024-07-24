/*
 * The MIT License (MIT)
 *
 * Copyright 2024 Crown Copyright (Health Education England)
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
import org.mapstruct.MappingConstants.ComponentModel;
import uk.nhs.hee.tis.trainee.sync.dto.HeeUserDto;
import uk.nhs.hee.tis.trainee.sync.model.HeeUser;

/**
 * A mapper to convert between HEE User data types.
 */
@Mapper(componentModel = ComponentModel.SPRING)
public interface HeeUserMapper {

  /**
   * Map a HeeUser to a DTO.
   *
   * @param heeUser The HEE User to convert.
   * @return The mapped HEE user DTO.
   */
  @Mapping(target = "firstName", source = "data.firstName")
  @Mapping(target = "lastName", source = "data.lastName")
  @Mapping(target = "gmcId", source = "data.gmcId")
  @Mapping(target = "emailAddress", source = "data.emailAddress")
  @Mapping(target = "phoneNumber", source = "data.phoneNumber")
  HeeUserDto toDto(HeeUser heeUser);

}

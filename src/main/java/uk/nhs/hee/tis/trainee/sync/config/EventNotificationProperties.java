/*
 * The MIT License (MIT)
 *
 * Copyright 2023 Crown Copyright (Health Education England)
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

package uk.nhs.hee.tis.trainee.sync.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * A representation of the event notification endpoint properties.
 *
 * @param deletePlacementEvent           The delete placement event ARN
 * @param deleteProgrammeMembershipEvent The delete programme membership event ARN
 * @param updateContactDetails           The update contact details event ARN
 * @param updateGdcDetails               The update GDC details event ARN
 * @param updateGmcDetails               The update GMC details event ARN
 * @param updatePerson                   The update person event ARN
 * @param updatePersonalInfo             The update personal info event ARN
 * @param updatePersonOwner              The update person owner event ARN
 * @param updatePlacementEvent           The update placement event ARN
 * @param updateProgrammeMembershipEvent The update programme membership event ARN
 */
@ConfigurationProperties(prefix = "application.aws.sns")
public record EventNotificationProperties(
    String deletePlacementEvent,
    String deleteProgrammeMembershipEvent,
    String updateContactDetails,
    String updateGdcDetails,
    String updateGmcDetails,
    String updatePerson,
    String updatePersonOwner,
    String updatePersonalInfo,
    String updatePlacementEvent,
    String updateProgrammeMembershipEvent) {

}

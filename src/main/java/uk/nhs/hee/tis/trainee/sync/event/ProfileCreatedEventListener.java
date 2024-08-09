/*
 * The MIT License (MIT)
 *
 * Copyright 2022 Crown Copyright (Health Education England)
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

package uk.nhs.hee.tis.trainee.sync.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.awspring.cloud.sqs.annotation.SqsListener;
import jakarta.validation.Valid;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import uk.nhs.hee.tis.trainee.sync.model.Person;
import uk.nhs.hee.tis.trainee.sync.service.DataRequestService;

/**
 * A listener for {@link ProfileCreatedEvent}s.
 */
@Slf4j
@Component
@Validated
public class ProfileCreatedEventListener {

  private final DataRequestService requestService;

  ProfileCreatedEventListener(DataRequestService requestService) {
    this.requestService = requestService;
  }

  /**
   * Requests associated Person data when a profile is created.
   *
   * @param event The incoming event.
   * @throws JsonProcessingException If the event's id could not be processed.
   */
  @SqsListener("${application.aws.sqs.profile-created}")
  void getEvent(@Valid ProfileCreatedEvent event) throws JsonProcessingException {
    String id = event.getTraineeTisId();
    log.info("Profile created event received for trainee ID '{}'.", id);

    requestService.sendRequest(Person.ENTITY_NAME, Collections.singletonMap("id", id));
  }
}

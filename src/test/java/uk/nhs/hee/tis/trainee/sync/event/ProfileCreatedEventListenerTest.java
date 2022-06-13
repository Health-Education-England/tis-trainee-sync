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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Collections;
import javax.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import uk.nhs.hee.tis.trainee.sync.service.DataRequestService;

@ContextConfiguration(classes = ValidationAutoConfiguration.class)
@SpringBootTest(classes = ProfileCreatedEventListener.class)
class ProfileCreatedEventListenerTest {

  @Autowired
  private ProfileCreatedEventListener listener;
  @MockBean
  private DataRequestService requestService;

  @Test
  void shouldRequestPersonWhenEventHasId() throws JsonProcessingException {
    ProfileCreatedEvent event = new ProfileCreatedEvent();
    event.setTraineeTisId("10");

    listener.getEvent(event);

    verify(requestService).sendRequest("Person", Collections.singletonMap("id", "10"));
  }

  @Test
  void shouldNotRequestPersonWhenEventHasId() {
    ProfileCreatedEvent event = new ProfileCreatedEvent();

    assertThrows(ConstraintViolationException.class, () -> listener.getEvent(event));

    verifyNoInteractions(requestService);
  }
}

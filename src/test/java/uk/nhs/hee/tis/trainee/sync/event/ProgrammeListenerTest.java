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

package uk.nhs.hee.tis.trainee.sync.event;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import uk.nhs.hee.tis.trainee.sync.model.Programme;
import uk.nhs.hee.tis.trainee.sync.service.ProgrammeSyncService;

@ExtendWith(OutputCaptureExtension.class)
class ProgrammeListenerTest {

  private ProgrammeListener listener;

  private ProgrammeSyncService service;

  @BeforeEach
  void setUp() {
    service = mock(ProgrammeSyncService.class);
    listener = new ProgrammeListener(service);
  }

  @Test
  void shouldOnlyLogRecordWhenDataAndMetadataNotNull(CapturedOutput output) {
    Programme programme = new Programme();
    programme.setData(Collections.emptyMap());
    programme.setMetadata(Collections.emptyMap());

    listener.getProgramme(programme);

    verifyNoInteractions(service);

    assertThat("Expected log not found.", output.getOut(), containsString("Received programme"));
  }
}

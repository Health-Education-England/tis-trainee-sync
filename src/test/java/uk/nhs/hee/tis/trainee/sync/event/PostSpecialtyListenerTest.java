/*
 * The MIT License (MIT)
 *
 *  Copyright 2023 Crown Copyright (Health Education England)
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 *  and associated documentation files (the "Software"), to deal in the Software without
 *  restriction, including without limitation the rights to use, copy, modify, merge, publish,
 *  distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all copies or
 *  substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 *  BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 *  DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package uk.nhs.hee.tis.trainee.sync.event;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.nhs.hee.tis.trainee.sync.model.PostSpecialty;
import uk.nhs.hee.tis.trainee.sync.service.PostSpecialtySyncService;

class PostSpecialtyListenerTest {

  private PostSpecialtyListener listener;

  private PostSpecialtySyncService service;

  @BeforeEach
  void setUp() {
    service = mock(PostSpecialtySyncService.class);
    listener = new PostSpecialtyListener(service);
  }

  @Test
  void shouldProcessRecordWhenDataAndMetadataNotNull() {
    PostSpecialty postSpecialty = new PostSpecialty();
    postSpecialty.setData(Collections.emptyMap());
    postSpecialty.setMetadata(Collections.emptyMap());

    listener.getPostSpecialty(postSpecialty);

    verify(service).syncPostSpecialty(postSpecialty);
  }
}
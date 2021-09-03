/*
 * The MIT License (MIT)
 *
 * Copyright 2021 Crown Copyright (Health Education England)
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Collections;
import javax.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import uk.nhs.hee.tis.trainee.sync.model.Post;
import uk.nhs.hee.tis.trainee.sync.service.PostSyncService;

class PostListenerTest {

  private PostListener listener;

  private PostSyncService service;

  @BeforeEach
  void setUp() {
    service = mock(PostSyncService.class);
    listener = new PostListener(service);
  }

  @Disabled("Unable to get the validation working during tests.")
  @Test
  void shouldNotProcessRecordWhenDataNull() {
    Post post = new Post();
    post.setMetadata(Collections.emptyMap());

    assertThrows(ConstraintViolationException.class, () -> listener.getPost(post));

    verifyNoInteractions(service);
  }

  @Disabled("Unable to get the validation working during tests.")
  @Test
  void shouldNotProcessRecordWhenMetadataNull() {
    Post post = new Post();
    post.setData(Collections.emptyMap());

    assertThrows(ConstraintViolationException.class, () -> listener.getPost(post));

    verifyNoInteractions(service);
  }

  @Disabled("Unable to get the validation working during tests.")
  @Test
  void shouldNotProcessRecordWhenDataAndMetadataNull() {
    Post post = new Post();

    assertThrows(ConstraintViolationException.class, () -> listener.getPost(post));

    verifyNoInteractions(service);
  }

  @Test
  void shouldProcessRecordWhenDataAndMetadataNotNull() {
    Post post = new Post();
    post.setData(Collections.emptyMap());
    post.setMetadata(Collections.emptyMap());

    listener.getPost(post);

    verify(service).syncPost(post);
  }
}

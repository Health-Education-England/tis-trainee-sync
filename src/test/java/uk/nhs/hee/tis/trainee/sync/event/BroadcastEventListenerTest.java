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

package uk.nhs.hee.tis.trainee.sync.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.nhs.hee.tis.trainee.sync.facade.ProgrammeMembershipEnricherFacade;
import uk.nhs.hee.tis.trainee.sync.model.BroadcastRouting;
import uk.nhs.hee.tis.trainee.sync.model.ProgrammeMembership;

class BroadcastEventListenerTest {

  private BroadcastEventListener listener;
  private ProgrammeMembershipEnricherFacade programmeMembershipEnricherFacade;

  @BeforeEach
  void setUp() {
    programmeMembershipEnricherFacade = mock(ProgrammeMembershipEnricherFacade.class);
    listener = new BroadcastEventListener(programmeMembershipEnricherFacade);
  }

  @Test
  void shouldBroadcastCoJ() {
    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    BroadcastEvent event = new BroadcastEvent(programmeMembership, BroadcastRouting.COJ);

    listener.onApplicationEvent(event);

    verify(programmeMembershipEnricherFacade).broadcastCoj(any());
  }

  @Test
  void shouldNotBroadcastIgnoredEvents() {
    Object object = new Object();
    BroadcastEvent event = new BroadcastEvent(object, BroadcastRouting.IGNORED);

    listener.onApplicationEvent(event);

    verifyNoInteractions(programmeMembershipEnricherFacade);
  }
}

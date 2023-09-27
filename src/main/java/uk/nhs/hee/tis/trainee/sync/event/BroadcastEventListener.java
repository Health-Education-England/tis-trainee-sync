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

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.trainee.sync.facade.ProgrammeMembershipEnricherFacade;
import uk.nhs.hee.tis.trainee.sync.model.BroadcastRouting;
import uk.nhs.hee.tis.trainee.sync.model.ProgrammeMembership;

/**
 * An event listener for custom Broadcast events.
 */
@Component
@Slf4j
public class BroadcastEventListener implements ApplicationListener<BroadcastEvent> {

  private final ProgrammeMembershipEnricherFacade programmeMembershipEnricher;

  BroadcastEventListener(ProgrammeMembershipEnricherFacade programmeMembershipEnricher) {
    this.programmeMembershipEnricher = programmeMembershipEnricher;
  }

  /**
   * Respond to a BroadcastEvent-type application event.
   *
   * @param event the event to respond to
   */
  @Override
  public void onApplicationEvent(BroadcastEvent event) {
    log.info("Received broadcast event - " + event.getRoutingType());
    if (event.getRoutingType().equals(BroadcastRouting.COJ)) {
      ProgrammeMembership programmeMembership = (ProgrammeMembership) event.getSource();
      programmeMembershipEnricher.broadcastCoj(programmeMembership);
    } else {
      log.info("Event message not supported");
    }
  }
}

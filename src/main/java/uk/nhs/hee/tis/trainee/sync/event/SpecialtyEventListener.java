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

import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.PlacementSpecialty;
import uk.nhs.hee.tis.trainee.sync.model.PostSpecialty;
import uk.nhs.hee.tis.trainee.sync.model.Specialty;
import uk.nhs.hee.tis.trainee.sync.service.FifoMessagingService;
import uk.nhs.hee.tis.trainee.sync.service.PlacementSpecialtySyncService;
import uk.nhs.hee.tis.trainee.sync.service.PostSpecialtySyncService;

@Component
public class SpecialtyEventListener extends AbstractMongoEventListener<Specialty> {

  private final PlacementSpecialtySyncService placementSpecialtyService;
  private final PostSpecialtySyncService postSpecialtyService;

  private final FifoMessagingService fifoMessagingService;

  private final String placementSpecialtyQueueUrl;
  private final String postSpecialtyQueueUrl;

  SpecialtyEventListener(PlacementSpecialtySyncService placementSpecialtyService,
      PostSpecialtySyncService postSpecialtyService,
      FifoMessagingService fifoMessagingService,
      @Value("${application.aws.sqs.placement-specialty}") String placementSpecialtyQueueUrl,
      @Value("${application.aws.sqs.post-specialty}") String postSpecialtyQueueUrl
  ) {
    this.placementSpecialtyService = placementSpecialtyService;
    this.postSpecialtyService = postSpecialtyService;
    this.fifoMessagingService = fifoMessagingService;
    this.placementSpecialtyQueueUrl = placementSpecialtyQueueUrl;
    this.postSpecialtyQueueUrl = postSpecialtyQueueUrl;
  }

  @Override
  public void onAfterSave(AfterSaveEvent<Specialty> event) {
    super.onAfterSave(event);

    Specialty specialty = event.getSource();
    sendPlacementSpecialtyMessages(specialty.getTisId(), Operation.LOAD);
    sendPostSubSpecialtyMessages(specialty.getTisId(), Operation.LOAD);
  }

  @Override
  public void onAfterDelete(AfterDeleteEvent<Specialty> event) {
    super.onAfterDelete(event);

    String specialtyId = event.getSource().getString("_id");
    sendPlacementSpecialtyMessages(specialtyId, Operation.DELETE);
    sendPostSubSpecialtyMessages(specialtyId, Operation.DELETE);
  }

  /**
   * Send messages for all associated placement specialties.
   *
   * @param specialtyId The ID of the specialty to get associated placement specialties for.
   * @param operation   The operation to set on the message, e.g. DELETE.
   */
  private void sendPlacementSpecialtyMessages(String specialtyId, Operation operation) {
    Set<PlacementSpecialty> placementSpecialties = placementSpecialtyService
        .findBySpecialtyId(specialtyId);

    for (PlacementSpecialty placementSpecialty : placementSpecialties) {
      // Default each placement specialty's operation.
      placementSpecialty.setOperation(operation);
      fifoMessagingService.sendMessageToFifoQueue(placementSpecialtyQueueUrl, placementSpecialty);
    }
  }

  /**
   * Send messages for all associated post sub-specialties.
   *
   * @param specialtyId The ID of the specialty to get associated post sub-specialties for.
   * @param operation   The operation to set on the message, e.g. DELETE.
   */
  private void sendPostSubSpecialtyMessages(String specialtyId, Operation operation) {
    Set<PostSpecialty> postSpecialties = postSpecialtyService
        .findBySpecialtyId(specialtyId);

    for (PostSpecialty postSpecialty : postSpecialties) {
      // Default each post specialty's operation.
      postSpecialty.setOperation(operation);
      fifoMessagingService.sendMessageToFifoQueue(postSpecialtyQueueUrl, postSpecialty);
    }
  }
}

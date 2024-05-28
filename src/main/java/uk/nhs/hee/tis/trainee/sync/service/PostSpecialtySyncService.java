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

package uk.nhs.hee.tis.trainee.sync.service;

import static uk.nhs.hee.tis.trainee.sync.model.Operation.DELETE;

import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.trainee.sync.model.PostSpecialty;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.repository.PostSpecialtyRepository;

@Slf4j
@Service("tcs-PostSpecialty")
public class PostSpecialtySyncService implements SyncService {

  private static final String REQUIRED_SPECIALITY_TYPE = "SUB_SPECIALTY";

  private final PostSpecialtyRepository repository;

  private final FifoMessagingService fifoMessagingService;
  private final String queueUrl;

  PostSpecialtySyncService(PostSpecialtyRepository repository,
      FifoMessagingService fifoMessagingService,
      @Value("${application.aws.sqs.post-specialty}") String queueUrl) {
    this.repository = repository;
    this.fifoMessagingService = fifoMessagingService;
    this.queueUrl = queueUrl;
  }

  @Override
  public void syncRecord(Record postSpecialty) {
    if (!(postSpecialty instanceof PostSpecialty)) {
      String message = String.format("Invalid record type '%s'.", postSpecialty.getClass());
      throw new IllegalArgumentException(message);
    }

    // Send incoming post specialty records to the post specialty queue to be processed.
    fifoMessagingService.sendMessageToFifoQueue(queueUrl, postSpecialty);
  }

  /**
   * Synchronize the given post specialty.
   *
   * @param postSpecialty The post specialty to synchronize.
   */
  public void syncPostSpecialty(PostSpecialty postSpecialty) {
    if (postSpecialty.getOperation().equals(DELETE)) {
      repository.deleteById(postSpecialty.getTisId());
    } else {
      if (Objects.equals(
          postSpecialty.getData().get("postSpecialtyType"), REQUIRED_SPECIALITY_TYPE)) {
        repository.save(postSpecialty);
      }
    }
  }

  public Optional<PostSpecialty> findById(String id) {
    return repository.findById(id);
  }

  public Set<PostSpecialty> findByPostId(String postId) {
    return repository.findSubSpecialtiesByPostId(postId);
  }

  public Set<PostSpecialty> findBySpecialtyId(String specialtyId) {
    return repository.findSubSpecialtiesBySpecialtyId(specialtyId);
  }
}

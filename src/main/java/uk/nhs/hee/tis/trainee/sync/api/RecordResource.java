/*
 * The MIT License (MIT)
 *
 * Copyright 2020 Crown Copyright (Health Education England)
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

package uk.nhs.hee.tis.trainee.sync.api;

import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.nhs.hee.tis.trainee.sync.dto.RecordDto;
import uk.nhs.hee.tis.trainee.sync.service.RecordService;

@Slf4j
@RestController
@RequestMapping("/api")
public class RecordResource {

  private final RecordService recordService;

  public RecordResource(RecordService recordService) {
    this.recordService = recordService;
  }

  /**
   * POST  /record : Process the given CDC record.
   *
   * @param recordDto The record DTO to process.
   * @return the DTO.
   */
  @PostMapping("/record")
  public ResponseEntity<RecordDto> processRecord(@Valid @RequestBody RecordDto recordDto,
      BindingResult bindingResult) {
    log.info("REST request to process Record : {}", recordDto);

    if (bindingResult.hasErrors()) {
      log.warn("Invalid record received, skipping processing.");
      bindingResult.getAllErrors().forEach(error -> log.debug(error.toString()));
    } else {
      recordService.processRecord(recordDto);
    }

    // TODO: Return something more sensible for failures.
    return ResponseEntity.ok(recordDto);
  }
}

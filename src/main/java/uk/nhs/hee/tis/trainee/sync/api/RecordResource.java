package uk.nhs.hee.tis.trainee.sync.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.nhs.hee.tis.trainee.sync.dto.RecordDto;
import uk.nhs.hee.tis.trainee.sync.mapper.RecordMapper;
import uk.nhs.hee.tis.trainee.sync.model.Record;

@RestController
@RequestMapping("/api")
public class RecordResource {

  private static final Logger LOG = LoggerFactory.getLogger(RecordResource.class);

  private RecordMapper recordMapper;

  public RecordResource(RecordMapper recordMapper) {
    this.recordMapper = recordMapper;
  }

  @PostMapping("/record")
  public ResponseEntity<RecordDto> receiveRecord(@RequestBody RecordDto recordDto) {
    Record record = recordMapper.toEntity(recordDto);
    LOG.info("REST request to receive Record : {}", record);
    return ResponseEntity.ok(recordDto);
  }
}

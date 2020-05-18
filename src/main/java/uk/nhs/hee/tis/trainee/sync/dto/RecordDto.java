package uk.nhs.hee.tis.trainee.sync.dto;

import java.util.Map;
import lombok.Data;

@Data
public class RecordDto {

  private Map<String, String> data;
  private Map<String, String> metadata;
}

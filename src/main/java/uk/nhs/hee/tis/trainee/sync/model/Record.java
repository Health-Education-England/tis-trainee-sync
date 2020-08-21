package uk.nhs.hee.tis.trainee.sync.model;

import java.util.Map;
import lombok.Data;

@Data
public class Record {

  private Map<String, String> data;
  private Map<String, String> metadata;

  // TODO: Change operation to enum of UPDATE/INSERT/DELETE.
  private String operation;
  private String schema;
  private String table;
}

package uk.nhs.hee.tis.trainee.sync.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.nhs.hee.tis.trainee.sync.dto.RecordDto;
import uk.nhs.hee.tis.trainee.sync.mapper.util.MetadataUtil;
import uk.nhs.hee.tis.trainee.sync.mapper.util.MetadataUtil.Operation;
import uk.nhs.hee.tis.trainee.sync.mapper.util.MetadataUtil.Schema;
import uk.nhs.hee.tis.trainee.sync.mapper.util.MetadataUtil.Table;
import uk.nhs.hee.tis.trainee.sync.model.Record;

@Mapper(componentModel = "spring", uses = MetadataUtil.class)
public interface RecordMapper {

  @Mapping(target = "operation", source = "metadata", qualifiedBy = Operation.class)
  @Mapping(target = "schema", source = "metadata", qualifiedBy = Schema.class)
  @Mapping(target = "table", source = "metadata", qualifiedBy = Table.class)
  Record toEntity(RecordDto recordDto);
}

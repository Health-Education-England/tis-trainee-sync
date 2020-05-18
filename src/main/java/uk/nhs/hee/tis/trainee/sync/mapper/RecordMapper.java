package uk.nhs.hee.tis.trainee.sync.mapper;

import org.mapstruct.Mapper;
import uk.nhs.hee.tis.trainee.sync.dto.RecordDto;
import uk.nhs.hee.tis.trainee.sync.model.Record;

@Mapper(componentModel = "spring")
public interface RecordMapper {

  RecordDto toDto(Record record);

  Record toEntity(RecordDto recordDto);
}

package uk.nhs.hee.tis.trainee.sync.migration;

import io.mongock.api.annotations.BeforeExecution;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackBeforeExecution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexField;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.index.IndexOperations;

import java.util.List;

@Slf4j
@ChangeUnit(id = "CopyProgrammeMembershipToCurriculumMembership", order = "1")
public class CopyProgrammeMembershipToCurriculumMembership {
  private static final String SOURCE_COLLECTION = "programmeMembership";
  private static final String DEST_COLLECTION = "curriculumMembership";

  private final MongoTemplate mongoTemplate;

  public CopyProgrammeMembershipToCurriculumMembership(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  /**
   * Set up CurriculumMembership table with correct indexes
   */
  @BeforeExecution
  public void createCurriculumMembershipTable() {
    IndexOperations indexOperationsSource;
    IndexOperations indexOperationsDest;
    List<IndexInfo> idxInfoList;

    if (!mongoTemplate.collectionExists(DEST_COLLECTION)) {
      mongoTemplate.createCollection(DEST_COLLECTION);
    }

    indexOperationsSource = mongoTemplate.indexOps(SOURCE_COLLECTION);
    indexOperationsDest = mongoTemplate.indexOps(DEST_COLLECTION);
    idxInfoList = indexOperationsSource.getIndexInfo();
    idxInfoList.forEach(idxInfo -> {
      List<IndexField> idxFields = idxInfo.getIndexFields();
      if (idxFields.size() < 2) {
        Index index = new Index(idxFields.get(0).getKey(), Sort.Direction.ASC);
        indexOperationsDest.ensureIndex(index);
      } else {
        //compound index
        Document keys = new Document();
        idxFields.forEach(idxField -> {
          Index index = new Index(idxField.getKey(), Sort.Direction.ASC);
          keys.append(idxField.getKey(), index);
        });
        CompoundIndexDefinition compoundIndexDefinition = new CompoundIndexDefinition(keys);
        indexOperationsDest.ensureIndex(compoundIndexDefinition);
      }
    });
  }

  /**
   * Copy all ProgrammeMembership records to CurriculumMembership
   */
  @Execution
  public void migrate() {
    log.info("here");


    //mongoTemplate.remove(lessThanPilotEndDateQuery, FormRPartA.class);
    //mongoTemplate.remove(lessThanPilotEndDateQuery, FormRPartB.class);
  }

  /**
   * Do not attempt rollback, allow the curriculumMembership table creation to persist
   */
  @RollbackBeforeExecution
  public void rollbackTableCreate() {
    log.warn("Rollback requested but not available for 'createCurriculumMembership' migration.");
  }

  /**
   * Do not attempt rollback, any successfully copied records should be left as-is
   * TODO: hmmm
   */
  @RollbackExecution
  public void rollback() {
    log.warn("Rollback requested but not available for 'copyProgrammeMembership' migration.");
  }

}

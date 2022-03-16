package uk.nhs.hee.tis.trainee.sync.migration;

import com.mongodb.MongoException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.WriteModel;
import io.mongock.api.annotations.BeforeExecution;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackBeforeExecution;
import io.mongock.api.annotations.RollbackExecution;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexField;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.index.IndexOperations;
import uk.nhs.hee.tis.trainee.sync.model.CurriculumMembership;

@Slf4j
@ChangeUnit(id = "copyProgrammeMembershipToCurriculumMembership", order = "1")
public class CopyProgrammeMembershipToCurriculumMembership {
  private static final String SOURCE_COLLECTION = "programmeMembership";
  private static final String DEST_COLLECTION = "curriculumMembership";
  private static final String DEST_CLASS_NAME = CurriculumMembership.class.getName();

  private final MongoTemplate mongoTemplate;

  public CopyProgrammeMembershipToCurriculumMembership(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  /**
   * Set up CurriculumMembership table with correct indexes.
   */
  @BeforeExecution
  public void createCurriculumMembershipTable() {
    IndexOperations indexOperationsSource;
    IndexOperations indexOperationsDest;
    AtomicInteger compoundIndexCount = new AtomicInteger(0);

    if (!mongoTemplate.collectionExists(DEST_COLLECTION)) {
      mongoTemplate.createCollection(DEST_COLLECTION);
    }

    indexOperationsSource = mongoTemplate.indexOps(SOURCE_COLLECTION);
    indexOperationsDest = mongoTemplate.indexOps(DEST_COLLECTION);
    List<IndexInfo> idxInfoList = indexOperationsSource.getIndexInfo();
    idxInfoList.forEach(idxInfo -> {
      List<IndexField> idxFields = idxInfo.getIndexFields();
      if (idxFields.size() < 2) {
        Index index = new Index(idxFields.get(0).getKey(), Sort.Direction.ASC);
        indexOperationsDest.ensureIndex(index);
      } else {
        //compound index
        Document keys = new Document();
        idxFields.forEach(idxField -> keys.append(idxField.getKey(), 1));
        CompoundIndexDefinition compoundIndexDefinition = new CompoundIndexDefinition(keys);
        compoundIndexDefinition.named("compoundIdx_" + compoundIndexCount.incrementAndGet());
        indexOperationsDest.ensureIndex(compoundIndexDefinition);
      }
    });
  }

  /**
   * Copy all ProgrammeMembership records to CurriculumMembership.
   */
  @Execution
  public void copyProgrammeMembership() {
    MongoCollection<Document> sourceCollection = mongoTemplate.getCollection(SOURCE_COLLECTION);
    MongoCollection<Document> destCollection = mongoTemplate.getCollection(DEST_COLLECTION);

    try {
      ReplaceOptions replaceOptions = new ReplaceOptions();
      replaceOptions.upsert(true);
      List<WriteModel<Document>> writes = new ArrayList<>();
      FindIterable<Document> cursor;
      cursor = sourceCollection.find();
      if (cursor.first() != null) {
        cursor.forEach(d -> {
          d.put("_class", DEST_CLASS_NAME);
          WriteModel<Document> wmd = new ReplaceOneModel<>(
              new Document("_id", d.get("_id")),
              d,
              replaceOptions);
          writes.add(wmd);
        });
        destCollection.bulkWrite(writes);
      }
    } catch (MongoException me) {
      log.error("Mongo error: " + me);
    }
  }

  /**
   * Do not attempt rollback, allow the curriculumMembership table to persist.
   */
  @RollbackBeforeExecution
  public void rollbackTableCreate() {
    log.warn("Rollback requested but not available for 'createCurriculumMembership' migration.");
  }

  /**
   * Do not attempt rollback, retain any existing CurriculumMembership records.
   */
  @RollbackExecution
  public void rollback() {
    log.warn("Rollback requested but not available for 'copyProgrammeMembership' migration.");
  }
}

package uk.nhs.hee.tis.trainee.sync.migration;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.WriteModel;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.DefaultIndexOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexField;
import org.springframework.data.mongodb.core.index.IndexInfo;
import uk.nhs.hee.tis.trainee.sync.model.CurriculumMembership;

class CopyProgrammeMembershipToCurriculumMembershipTest {
  private static final String SOURCE_COLLECTION = "programmeMembership";
  private static final String DEST_COLLECTION = "curriculumMembership";
  private static final String DEST_CLASS_NAME = CurriculumMembership.class.getName();

  private CopyProgrammeMembershipToCurriculumMembership migration;

  private MongoTemplate template;
  private DefaultIndexOperations indexOperationsSource;
  private DefaultIndexOperations indexOperationsDest;
  private MongoCollection<Document> sourceCollection;
  private MongoCollection<Document> destCollection;

  @BeforeEach
  void setUp() {
    template = mock(MongoTemplate.class);
    indexOperationsSource = mock(DefaultIndexOperations.class);
    indexOperationsDest = mock(DefaultIndexOperations.class);
    sourceCollection = mock(MongoCollection.class);
    destCollection = mock(MongoCollection.class);
    migration = new CopyProgrammeMembershipToCurriculumMembership(template);
  }

  @Test
  void shouldCreateCurriculumMembershipTable() {
    IndexField idxFieldSimple = IndexField.create("simpleIndexField", Sort.Direction.ASC);
    IndexField idxFieldCompound1 = IndexField.create("compoundIndexField1", Sort.Direction.ASC);
    IndexField idxFieldCompound2 = IndexField.create("compoundIndexField2", Sort.Direction.ASC);
    IndexInfo idxInfoSimple = new IndexInfo(List.of(idxFieldSimple),
        "test", false, false, "en");
    IndexInfo idxInfoCompound = new IndexInfo(List.of(idxFieldCompound1, idxFieldCompound2),
        "test2", false, false, "en");

    //given
    when(template.collectionExists(DEST_COLLECTION)).thenReturn(false);
    when(template.indexOps(SOURCE_COLLECTION)).thenReturn(indexOperationsSource);
    when(template.indexOps(DEST_COLLECTION)).thenReturn(indexOperationsDest);
    when(indexOperationsSource.getIndexInfo()).thenReturn(List.of(idxInfoSimple, idxInfoCompound));

    //when
    migration.createCurriculumMembershipTable();
    ArgumentCaptor<Index> indexCaptor = ArgumentCaptor.forClass(Index.class);

    //then
    verify(template).createCollection(DEST_COLLECTION);
    verify(indexOperationsDest, times(2)).ensureIndex(indexCaptor.capture());
    List<Index> indexList = indexCaptor.getAllValues();
    assertThat("Unexpected simple index field sort order",
        indexList.get(0).getIndexKeys().get("simpleIndexField"),
        is(1));
    assertThat("Unexpected compound index field sort order",
        indexList.get(1).getIndexKeys().get("compoundIndexField2"),
        is(1));
  }

  @Test
  void shouldCopyProgrammeMembershipTable() {
    Document doc = new Document();
    doc.put("_id", "1");
    doc.put("_class", "old.class");

    FindIterable iterable = mock(FindIterable.class);
    MongoCursor cursor = mock(MongoCursor.class);

    //given
    when(template.getCollection(SOURCE_COLLECTION)).thenReturn(sourceCollection);
    when(template.getCollection(DEST_COLLECTION)).thenReturn(destCollection);

    when(sourceCollection.find()).thenReturn(iterable);
    doCallRealMethod().when(iterable).forEach(any(Consumer.class));
    //to allow the iterator to catch forEach()
    when(iterable.iterator()).thenReturn(cursor);
    when(cursor.hasNext())
        .thenReturn(true)
        .thenReturn(false);
    when(cursor.next())
        .thenReturn(doc);

    //when
    migration.copyProgrammeMembership();
    ArgumentCaptor<List<WriteModel<Document>>> arrayListCaptor =
        ArgumentCaptor.forClass(ArrayList.class);

    //then
    verify(destCollection).bulkWrite(arrayListCaptor.capture());
    List<WriteModel<Document>> listWmd = arrayListCaptor.getValue();
    ReplaceOneModel<Document> wmd = (ReplaceOneModel<Document>) listWmd.get(0);
    assertThat("Unexpected _id value",
        wmd.getFilter().toBsonDocument().get("_id").asString().getValue(),
        is("1"));
    assertThat("Unexpected _class value",
        wmd.getReplacement().get("_class"),
        is(DEST_CLASS_NAME));
  }

  @Test
  void shouldNotAttemptRollbackTableCreate() {
    migration.rollbackTableCreate();
    verifyNoInteractions(template);
  }

  @Test
  void shouldNotAttemptRollback() {
    migration.rollback();
    verifyNoInteractions(template);
  }
}


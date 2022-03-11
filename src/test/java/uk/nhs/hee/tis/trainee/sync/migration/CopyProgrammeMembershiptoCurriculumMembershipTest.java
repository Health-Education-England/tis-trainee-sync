package uk.nhs.hee.tis.trainee.sync.migration;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.time.LocalDate;
import java.util.List;

import static java.time.Month.SEPTEMBER;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class CopyProgrammeMembershiptoCurriculumMembershipTest {

  private CopyProgrammeMembershipToCurriculumMembership migration;

  private MongoTemplate template;

  @BeforeEach
  void setUp() {
    template = mock(MongoTemplate.class);
    migration = new CopyProgrammeMembershipToCurriculumMembership(template);
  }

  @Test
  void shouldCopyTable() {
    migration.migrate();

//    ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
//    verify(template).remove(queryCaptor.capture(), eq(formClass));
//
//    Query query = queryCaptor.getValue();
//    Document queryObject = query.getQueryObject();
//    LocalDate queryDate = queryObject.getEmbedded(List.of("lastModifiedDate", "$lt"),
//        LocalDate.class);
//    assertThat("Unexpected query date.", queryDate, is(LocalDate.of(2021, SEPTEMBER, 1)));
  }

  @Test
  void shouldNotAttemptRollback() {
    migration.rollback();
    verifyNoInteractions(template);
  }
}

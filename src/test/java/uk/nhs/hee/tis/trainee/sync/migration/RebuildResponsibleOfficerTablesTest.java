/*
 * The MIT License (MIT)
 *
 * Copyright 2024 Crown Copyright (Health Education England)
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

package uk.nhs.hee.tis.trainee.sync.migration;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.mongodb.MongoException;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.data.mongodb.core.index.IndexOperations;
import uk.nhs.hee.tis.trainee.sync.model.HeeUser;
import uk.nhs.hee.tis.trainee.sync.model.UserDesignatedBody;
import uk.nhs.hee.tis.trainee.sync.model.UserRole;

class RebuildResponsibleOfficerTablesTest {
  private static final String HEE_USER_COLLECTION = "heeUser";
  private static final String USER_ROLE_COLLECTION = "userRole";
  private static final String USER_DB_COLLECTION = "userDesignatedBody";

  private RebuildResponsibleOfficerTables migration;
  private MongoTemplate template;

  @BeforeEach
  void setUp() {
    template = mock(MongoTemplate.class);
    migration = new RebuildResponsibleOfficerTables(template);
  }

  @Test
  void shouldRecreateResponsibleOfficerTables() {
    IndexOperations heeUserIndexOperationMock = mock(IndexOperations.class);
    when(template.indexOps(HeeUser.class)).thenReturn(heeUserIndexOperationMock);
    IndexOperations userRoleIndexOperationMock = mock(IndexOperations.class);
    when(template.indexOps(UserRole.class)).thenReturn(userRoleIndexOperationMock);
    IndexOperations userDbIndexOperationMock = mock(IndexOperations.class);
    when(template.indexOps(UserDesignatedBody.class)).thenReturn(userDbIndexOperationMock);

    migration.migrate();

    verify(template).dropCollection(HEE_USER_COLLECTION);
    verify(template).dropCollection(USER_ROLE_COLLECTION);
    verify(template).dropCollection(USER_DB_COLLECTION);

    verify(template).createCollection(HEE_USER_COLLECTION);
    verify(template).createCollection(USER_ROLE_COLLECTION);
    verify(template).createCollection(USER_DB_COLLECTION);
  }

  @Test
  void shouldSetIndexesInResponsibleOfficerTables() {
    IndexOperations heeUserIndexOperationMock = mock(IndexOperations.class);
    when(template.indexOps(HeeUser.class)).thenReturn(heeUserIndexOperationMock);
    IndexOperations userRoleIndexOperationMock = mock(IndexOperations.class);
    when(template.indexOps(UserRole.class)).thenReturn(userRoleIndexOperationMock);
    IndexOperations userDbIndexOperationMock = mock(IndexOperations.class);
    when(template.indexOps(UserDesignatedBody.class)).thenReturn(userDbIndexOperationMock);

    migration.migrate();

    ArgumentCaptor<IndexDefinition> heeUserIndexCaptor
        = ArgumentCaptor.forClass(IndexDefinition.class);
    verify(heeUserIndexOperationMock, atLeastOnce()).ensureIndex(heeUserIndexCaptor.capture());

    List<IndexDefinition> heeUserIndexes = heeUserIndexCaptor.getAllValues();
    assertThat("Unexpected number of indexes.", heeUserIndexes.size(), is(1));

    List<String> heeUserIndexKeys = heeUserIndexes.stream()
        .flatMap(i -> i.getIndexKeys().keySet().stream())
        .toList();
    assertThat("Unexpected index.", heeUserIndexKeys, hasItems("data.name"));


    ArgumentCaptor<IndexDefinition> userDbIndexCaptor
        = ArgumentCaptor.forClass(IndexDefinition.class);
    verify(userDbIndexOperationMock, atLeastOnce()).ensureIndex(userDbIndexCaptor.capture());

    List<IndexDefinition> userDbIndexes = userDbIndexCaptor.getAllValues();
    assertThat("Unexpected number of indexes.", userDbIndexes.size(), is(3));

    List<String> userDbIndexKeys = userDbIndexes.stream()
        .flatMap(i -> i.getIndexKeys().keySet().stream())
        .toList();
    assertThat("Unexpected index.", userDbIndexKeys,
        hasItems("data.userName", "data.designatedBodyCode"));
    List<String> userDbIndexCompositeKeys = userDbIndexes.stream()
        .filter(i -> i.getIndexKeys().size() > 1)
        .flatMap(i -> i.getIndexKeys().keySet().stream())
        .toList();
    assertThat("Unexpected compound index.", userDbIndexCompositeKeys,
        hasItems("data.userName", "data.designatedBodyCode"));


    ArgumentCaptor<IndexDefinition> userRoleIndexCaptor
        = ArgumentCaptor.forClass(IndexDefinition.class);
    verify(userRoleIndexOperationMock, atLeastOnce()).ensureIndex(userRoleIndexCaptor.capture());

    List<IndexDefinition> userRoleIndexes = userRoleIndexCaptor.getAllValues();
    assertThat("Unexpected number of indexes.", userRoleIndexes.size(), is(3));

    List<String> userRoleIndexKeys = userRoleIndexes.stream()
        .flatMap(i -> i.getIndexKeys().keySet().stream())
        .toList();
    assertThat("Unexpected index.", userRoleIndexKeys,
        hasItems("data.userName", "data.roleName"));
    List<String> userRoleIndexCompositeKeys = userRoleIndexes.stream()
        .filter(i -> i.getIndexKeys().size() > 1)
        .flatMap(i -> i.getIndexKeys().keySet().stream())
        .toList();
    assertThat("Unexpected compound index.", userRoleIndexCompositeKeys,
        hasItems("data.userName", "data.roleName"));
  }

  @Test
  void shouldCatchMongoExceptionNotThrowIt() {
    doThrow(new MongoException("exception")).when(template).dropCollection(anyString());
    Assertions.assertDoesNotThrow(() -> migration.migrate());
  }

  @Test
  void shouldNotAttemptRollback() {
    migration.rollback();
    verifyNoInteractions(template);
  }
}

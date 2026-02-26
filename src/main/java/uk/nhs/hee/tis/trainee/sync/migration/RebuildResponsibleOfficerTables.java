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

import com.mongodb.MongoException;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import uk.nhs.hee.tis.trainee.sync.model.HeeUser;
import uk.nhs.hee.tis.trainee.sync.model.UserDesignatedBody;
import uk.nhs.hee.tis.trainee.sync.model.UserRole;

/**
 * Recreate ResponsibleOfficer-related collections.
 */
@Slf4j
@ChangeUnit(id = "rebuildResponsibleOfficerTables", order = "1")
public class RebuildResponsibleOfficerTables {

  private static final String HEE_USER_COLLECTION = "heeUser";
  private static final String USER_ROLE_COLLECTION = "userRole";
  private static final String USER_DB_COLLECTION = "userDesignatedBody";

  private final MongoTemplate mongoTemplate;

  public RebuildResponsibleOfficerTables(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  /**
   * Recreate HeeUser, UserRole and UserDesignatedBody collections.
   */
  @Execution
  public void migrate() {
    try {
      mongoTemplate.dropCollection(HEE_USER_COLLECTION);
      log.info("Dropped {} collection.", HEE_USER_COLLECTION);
      mongoTemplate.dropCollection(USER_ROLE_COLLECTION);
      log.info("Dropped {} collection.", USER_ROLE_COLLECTION);
      mongoTemplate.dropCollection(USER_DB_COLLECTION);
      log.info("Dropped {} collection.", USER_DB_COLLECTION);

      mongoTemplate.createCollection(HEE_USER_COLLECTION);
      log.info("Recreated {} collection.", HEE_USER_COLLECTION);
      mongoTemplate.createCollection(USER_ROLE_COLLECTION);
      log.info("Recreated {} collection.", USER_ROLE_COLLECTION);
      mongoTemplate.createCollection(USER_DB_COLLECTION);
      log.info("Recreated {} collection.", USER_DB_COLLECTION);

      // HeeUser
      IndexOperations heeUserIndexOps = mongoTemplate.indexOps(HeeUser.class);
      heeUserIndexOps.createIndex(new Index().on("data.name", Direction.ASC));

      // UserDesignatedBody
      IndexOperations userDbIndexOps = mongoTemplate.indexOps(UserDesignatedBody.class);
      userDbIndexOps.createIndex(new Index().on("data.userName", Direction.ASC));
      userDbIndexOps.createIndex(new Index().on("data.designatedBodyCode", Direction.ASC));
      Document udbKeys = new Document();
      udbKeys.put("data.userName", 1);
      udbKeys.put("data.designatedBodyCode", 1);
      Index udbCompoundIndex = new CompoundIndexDefinition(udbKeys).unique()
          .named("userDesignatedBodyCompoundIndex");
      userDbIndexOps.createIndex(udbCompoundIndex);

      // UserRole
      IndexOperations userRoleIndexOps = mongoTemplate.indexOps(UserRole.class);
      userRoleIndexOps.createIndex(new Index().on("data.userName", Direction.ASC));
      userRoleIndexOps.createIndex(new Index().on("data.roleName", Direction.ASC));
      Document urKeys = new Document();
      urKeys.put("data.userName", 1);
      urKeys.put("data.roleName", 1);
      Index urCompoundIndex = new CompoundIndexDefinition(urKeys).unique()
          .named("userRoleCompoundIndex");
      userRoleIndexOps.createIndex(urCompoundIndex);

    } catch (MongoException me) {
      log.error("Unable to recreate collections due to an error: {} ", me.toString());
    }
  }

  /**
   * Do not attempt rollback, the collection should be left as-is.
   */
  @RollbackExecution
  public void rollback() {
    log.warn("Rollback requested but not available "
        + "for 'rebuildResponsibleOfficerTables' migration.");
  }
}

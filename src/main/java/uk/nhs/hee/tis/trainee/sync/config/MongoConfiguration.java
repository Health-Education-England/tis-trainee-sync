/*
 * The MIT License (MIT)
 *
 * Copyright 2021 Crown Copyright (Health Education England)
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

package uk.nhs.hee.tis.trainee.sync.config;

import javax.annotation.PostConstruct;
import org.bson.Document;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import uk.nhs.hee.tis.trainee.sync.model.Placement;
import uk.nhs.hee.tis.trainee.sync.model.Post;
import uk.nhs.hee.tis.trainee.sync.model.ProgrammeMembership;

@Configuration
public class MongoConfiguration {

  private final MongoTemplate template;

  MongoConfiguration(MongoTemplate template) {
    this.template = template;
  }

  /**
   * Add custom indexes to the Mongo collections.
   */
  @PostConstruct
  public void initIndexes() {
    IndexOperations postIndexOps = template.indexOps(Post.class);
    postIndexOps.ensureIndex(new Index().on("data.employingBodyId", Direction.ASC));
    postIndexOps.ensureIndex(new Index().on("data.trainingBodyId", Direction.ASC));

    IndexOperations placementIndexOps = template.indexOps(Placement.class);
    placementIndexOps.ensureIndex(new Index().on("data.postId", Direction.ASC));
    placementIndexOps.ensureIndex(new Index().on("data.siteId", Direction.ASC));

    IndexOperations programmeMembershipIndexOps = template.indexOps(ProgrammeMembership.class);
    programmeMembershipIndexOps.ensureIndex(new Index().on("programmeId", Direction.ASC));
    programmeMembershipIndexOps.ensureIndex(new Index().on("personId", Direction.ASC));
    Document keys = new Document();
    keys.put("programmeId", 1);
    keys.put("personId", 1);
    keys.put("programmeMembershipType", 1);
    keys.put("programmeStartDate", 1);
    keys.put("programmeEndDate", 1);
    Index programmeMembershipCompoundIndex = new CompoundIndexDefinition(keys)
        .named("programmeMembershipCompoundIndex");
    programmeMembershipIndexOps.ensureIndex(programmeMembershipCompoundIndex);
  }
}

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

import jakarta.annotation.PostConstruct;
import org.bson.Document;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import uk.nhs.hee.tis.trainee.sync.model.CurriculumMembership;
import uk.nhs.hee.tis.trainee.sync.model.Dbc;
import uk.nhs.hee.tis.trainee.sync.model.HeeUser;
import uk.nhs.hee.tis.trainee.sync.model.LocalOffice;
import uk.nhs.hee.tis.trainee.sync.model.Placement;
import uk.nhs.hee.tis.trainee.sync.model.PlacementSite;
import uk.nhs.hee.tis.trainee.sync.model.PlacementSpecialty;
import uk.nhs.hee.tis.trainee.sync.model.Post;
import uk.nhs.hee.tis.trainee.sync.model.PostSpecialty;
import uk.nhs.hee.tis.trainee.sync.model.Programme;
import uk.nhs.hee.tis.trainee.sync.model.ProgrammeMembership;
import uk.nhs.hee.tis.trainee.sync.model.UserDesignatedBody;
import uk.nhs.hee.tis.trainee.sync.model.UserRole;

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
    // DBC
    IndexOperations dbcIndexOps = template.indexOps(Dbc.class);
    dbcIndexOps.createIndex(new Index().on("data.dbc", Direction.ASC));

    // CurriculumMembership
    IndexOperations cmIndexOps = template.indexOps(CurriculumMembership.class);
    cmIndexOps.createIndex(new Index().on("data.programmeId", Direction.ASC));
    cmIndexOps.createIndex(new Index().on("data.programmeMembershipUuid", Direction.ASC));
    cmIndexOps.createIndex(new Index().on("data.curriculumId", Direction.ASC));
    cmIndexOps.createIndex(new Index().on("data.personId", Direction.ASC));
    Document cmKeys = new Document();
    cmKeys.put("data.personId", 1);
    cmKeys.put("data.programmeId", 1);
    cmKeys.put("data.programmeMembershipType", 1);
    cmKeys.put("data.programmeStartDate", 1);
    cmKeys.put("data.programmeEndDate", 1);
    Index curriculumMembershipCompoundIndex = new CompoundIndexDefinition(cmKeys)
        .named("curriculumMembershipCompoundIndex");
    cmIndexOps.createIndex(curriculumMembershipCompoundIndex);

    // LocalOffice
    IndexOperations localOfficeIndexOps = template.indexOps(LocalOffice.class);
    localOfficeIndexOps.createIndex(new Index().on("data.abbreviation", Direction.ASC));

    // HeeUser
    IndexOperations heeUserIndexOps = template.indexOps(HeeUser.class);
    heeUserIndexOps.createIndex(new Index().on("data.name", Direction.ASC));

    // Placement
    IndexOperations placementIndexOps = template.indexOps(Placement.class);
    placementIndexOps.createIndex(new Index().on("data.postId", Direction.ASC));
    placementIndexOps.createIndex(new Index().on("data.siteId", Direction.ASC));
    placementIndexOps.createIndex(new Index().on("data.gradeId", Direction.ASC));

    // PlacementSite
    IndexOperations placementSiteIndexOps = template.indexOps(PlacementSite.class);
    Document placementSiteKeys1 = new Document();
    placementSiteKeys1.put("siteId", 1);
    placementSiteKeys1.put("placementSiteType", 1);
    Index placementSiteCompoundIndex1 = new CompoundIndexDefinition(placementSiteKeys1)
        .named("placementSiteCompoundIndex1");
    placementSiteIndexOps.createIndex(placementSiteCompoundIndex1);

    Document placementSiteKeys2 = new Document();
    placementSiteKeys2.put("placementId", 1);
    placementSiteKeys2.put("placementSiteType", 1);
    Index placementSiteCompoundIndex2 = new CompoundIndexDefinition(placementSiteKeys2)
        .named("placementSiteCompoundIndex2");
    placementSiteIndexOps.createIndex(placementSiteCompoundIndex2);

    // PlacementSpecialty
    IndexOperations placementSpecialtyIndexOps = template.indexOps(PlacementSpecialty.class);
    Document placementSpecialtyKeys1 = new Document();
    placementSpecialtyKeys1.put("data.placementId", 1);
    placementSpecialtyKeys1.put("data.placementSpecialtyType", 1);
    Index placementSpecialtyCompoundIndex1 = new CompoundIndexDefinition(placementSpecialtyKeys1)
        .named("placementSpecialtyCompoundIndex1");
    placementSpecialtyIndexOps.createIndex(placementSpecialtyCompoundIndex1);

    Document placementSpecialtyKeys2 = new Document();
    placementSpecialtyKeys2.put("data.specialtyId", 1);
    placementSpecialtyKeys2.put("data.placementSpecialtyType", 1);
    Index placementSpecialtyCompoundIndex2 = new CompoundIndexDefinition(placementSpecialtyKeys2)
        .named("placementSpecialtyCompoundIndex2");
    placementSpecialtyIndexOps.createIndex(placementSpecialtyCompoundIndex2);

    Document placementSpecialtyKeys3 = new Document();
    placementSpecialtyKeys3.put("data.placementId", 1);
    placementSpecialtyKeys3.put("data.specialtyId", 1);
    Index placementSpecialtyCompoundIndex3 = new CompoundIndexDefinition(placementSpecialtyKeys3)
        .named("placementSpecialtyCompoundIndex3")
        .unique();
    placementSpecialtyIndexOps.createIndex(placementSpecialtyCompoundIndex3);

    // Post
    IndexOperations postIndexOps = template.indexOps(Post.class);
    postIndexOps.createIndex(new Index().on("data.employingBodyId", Direction.ASC));
    postIndexOps.createIndex(new Index().on("data.trainingBodyId", Direction.ASC));

    // PostSpecialty
    IndexOperations postSpecialtyIndexOps = template.indexOps(PostSpecialty.class);
    Document postSpecialtyKeys1 = new Document();
    postSpecialtyKeys1.put("data.postId", 1);
    postSpecialtyKeys1.put("data.postSpecialtyType", 1);
    Index postSpecialtyCompoundIndex1 = new CompoundIndexDefinition(postSpecialtyKeys1)
        .named("postSpecialtyCompoundIndex1");
    postSpecialtyIndexOps.createIndex(postSpecialtyCompoundIndex1);

    Document postSpecialtyKeys2 = new Document();
    postSpecialtyKeys2.put("data.specialtyId", 1);
    postSpecialtyKeys2.put("data.postSpecialtyType", 1);
    Index postSpecialtyCompoundIndex2 = new CompoundIndexDefinition(postSpecialtyKeys2)
        .named("postSpecialtyCompoundIndex2");
    postSpecialtyIndexOps.createIndex(postSpecialtyCompoundIndex2);

    // Programme
    IndexOperations programmeIndexOps = template.indexOps(Programme.class);
    programmeIndexOps.createIndex(new Index().on("data.owner", Direction.ASC));

    // ProgrammeMembership
    IndexOperations programmeMembershipIndexOps = template.indexOps(ProgrammeMembership.class);
    programmeMembershipIndexOps.createIndex(new Index().on("programmeId", Direction.ASC));
    programmeMembershipIndexOps.createIndex(new Index().on("personId", Direction.ASC));
    Document pmKeys = new Document();
    pmKeys.put("programmeId", 1);
    pmKeys.put("personId", 1);
    pmKeys.put("programmeMembershipType", 1);
    pmKeys.put("programmeStartDate", 1);
    pmKeys.put("programmeEndDate", 1);
    Index programmeMembershipCompoundIndex = new CompoundIndexDefinition(pmKeys)
        .named("programmeMembershipCompoundIndex");
    programmeMembershipIndexOps.createIndex(programmeMembershipCompoundIndex);

    // UserDesignatedBody
    IndexOperations userDbIndexOps = template.indexOps(UserDesignatedBody.class);
    userDbIndexOps.createIndex(new Index().on("data.userName", Direction.ASC));
    userDbIndexOps.createIndex(new Index().on("data.designatedBodyCode", Direction.ASC));

    // UserRole
    IndexOperations userRoleIndexOps = template.indexOps(UserRole.class);
    userRoleIndexOps.createIndex(new Index().on("data.userName", Direction.ASC));
    userRoleIndexOps.createIndex(new Index().on("data.roleName", Direction.ASC));
  }
}

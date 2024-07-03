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

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.data.mongodb.core.index.IndexOperations;
import uk.nhs.hee.tis.trainee.sync.model.CurriculumMembership;
import uk.nhs.hee.tis.trainee.sync.model.Placement;
import uk.nhs.hee.tis.trainee.sync.model.PlacementSite;
import uk.nhs.hee.tis.trainee.sync.model.PlacementSpecialty;
import uk.nhs.hee.tis.trainee.sync.model.Post;
import uk.nhs.hee.tis.trainee.sync.model.PostSpecialty;
import uk.nhs.hee.tis.trainee.sync.model.Programme;
import uk.nhs.hee.tis.trainee.sync.model.ProgrammeMembership;
import uk.nhs.hee.tis.trainee.sync.model.Record;

class MongoConfigurationTest {

  private MongoConfiguration configuration;

  private MongoTemplate template;

  @BeforeEach
  void setUp() {
    template = mock(MongoTemplate.class);
    configuration = new MongoConfiguration(template);

    IndexOperations indexOperations = mock(IndexOperations.class);
    when(template.indexOps(ArgumentMatchers.<Class<Record>>any())).thenReturn(indexOperations);
  }

  @Test
  void shouldInitIndexesForCurriculumMembershipCollection() {
    IndexOperations indexOperations = mock(IndexOperations.class);
    when(template.indexOps(CurriculumMembership.class)).thenReturn(indexOperations);

    configuration.initIndexes();

    ArgumentCaptor<IndexDefinition> indexCaptor = ArgumentCaptor.forClass(IndexDefinition.class);
    verify(indexOperations, atLeastOnce()).ensureIndex(indexCaptor.capture());

    List<IndexDefinition> indexes = indexCaptor.getAllValues();
    assertThat("Unexpected number of indexes.", indexes.size(), is(5));

    List<String> indexKeys = indexes.stream()
        .flatMap(i -> i.getIndexKeys().keySet().stream())
        .collect(Collectors.toList());
    assertThat("Unexpected index.", indexKeys,
        hasItems("data.personId", "data.programmeId", "data.programmeMembershipType",
            "data.programmeStartDate", "data.programmeEndDate", "data.programmeMembershipUuid",
            "data.curriculumId"));
  }

  @Test
  void shouldInitIndexesForPlacementCollection() {
    IndexOperations indexOperations = mock(IndexOperations.class);
    when(template.indexOps(Placement.class)).thenReturn(indexOperations);

    configuration.initIndexes();

    ArgumentCaptor<IndexDefinition> indexCaptor = ArgumentCaptor.forClass(IndexDefinition.class);
    verify(indexOperations, atLeastOnce()).ensureIndex(indexCaptor.capture());

    List<IndexDefinition> indexes = indexCaptor.getAllValues();
    assertThat("Unexpected number of indexes.", indexes.size(), is(3));

    List<String> indexKeys = indexes.stream()
        .flatMap(i -> i.getIndexKeys().keySet().stream())
        .collect(Collectors.toList());
    assertThat("Unexpected index.", indexKeys,
        hasItems("data.postId", "data.siteId", "data.gradeId"));
  }

  @Test
  void shouldInitIndexesForPlacementSiteCollection() {
    IndexOperations indexOperations = mock(IndexOperations.class);
    when(template.indexOps(PlacementSite.class)).thenReturn(indexOperations);

    configuration.initIndexes();

    ArgumentCaptor<IndexDefinition> indexCaptor = ArgumentCaptor.forClass(IndexDefinition.class);
    verify(indexOperations, atLeastOnce()).ensureIndex(indexCaptor.capture());

    List<IndexDefinition> indexes = indexCaptor.getAllValues();
    assertThat("Unexpected number of indexes.", indexes.size(), is(2));

    List<String> indexKeys = indexes.stream()
        .flatMap(i -> i.getIndexKeys().keySet().stream())
        .collect(Collectors.toList());
    assertThat("Unexpected index.", indexKeys,
        hasItems("siteId", "placementId", "placementSiteType"));
  }

  @Test
  void shouldInitIndexesForPlacementSpecialtyCollection() {
    IndexOperations indexOperations = mock(IndexOperations.class);
    when(template.indexOps(PlacementSpecialty.class)).thenReturn(indexOperations);

    configuration.initIndexes();

    ArgumentCaptor<IndexDefinition> indexCaptor = ArgumentCaptor.forClass(IndexDefinition.class);
    verify(indexOperations, atLeastOnce()).ensureIndex(indexCaptor.capture());

    List<IndexDefinition> indexes = indexCaptor.getAllValues();
    assertThat("Unexpected number of indexes.", indexes.size(), is(3));

    List<String> indexKeys = indexes.stream()
        .flatMap(i -> i.getIndexKeys().keySet().stream())
        .collect(Collectors.toList());
    assertThat("Unexpected index.", indexKeys,
        hasItems("data.placementId", "data.specialtyId", "data.placementSpecialtyType"));
  }

  @Test
  void shouldInitIndexesForPostCollection() {
    IndexOperations indexOperations = mock(IndexOperations.class);
    when(template.indexOps(Post.class)).thenReturn(indexOperations);

    configuration.initIndexes();

    ArgumentCaptor<IndexDefinition> indexCaptor = ArgumentCaptor.forClass(IndexDefinition.class);
    verify(indexOperations, atLeastOnce()).ensureIndex(indexCaptor.capture());

    List<IndexDefinition> indexes = indexCaptor.getAllValues();
    assertThat("Unexpected number of indexes.", indexes.size(), is(2));

    List<String> indexKeys = indexes.stream()
        .flatMap(i -> i.getIndexKeys().keySet().stream())
        .collect(Collectors.toList());
    assertThat("Unexpected index.", indexKeys,
        hasItems("data.employingBodyId", "data.trainingBodyId"));
  }

  @Test
  void shouldInitIndexesForPostSpecialtyCollection() {
    IndexOperations indexOperations = mock(IndexOperations.class);
    when(template.indexOps(PostSpecialty.class)).thenReturn(indexOperations);

    configuration.initIndexes();

    ArgumentCaptor<IndexDefinition> indexCaptor = ArgumentCaptor.forClass(IndexDefinition.class);
    verify(indexOperations, atLeastOnce()).ensureIndex(indexCaptor.capture());

    List<IndexDefinition> indexes = indexCaptor.getAllValues();
    assertThat("Unexpected number of indexes.", indexes.size(), is(2));

    List<String> indexKeys = indexes.stream()
        .flatMap(i -> i.getIndexKeys().keySet().stream())
        .collect(Collectors.toList());
    assertThat("Unexpected index.", indexKeys,
        hasItems("data.postId", "data.specialtyId", "data.postSpecialtyType"));
  }

  @Test
  void shouldInitIndexesForProgrammeCollection() {
    IndexOperations indexOperations = mock(IndexOperations.class);
    when(template.indexOps(Programme.class)).thenReturn(indexOperations);

    configuration.initIndexes();

    ArgumentCaptor<IndexDefinition> indexCaptor = ArgumentCaptor.forClass(IndexDefinition.class);
    verify(indexOperations, atLeastOnce()).ensureIndex(indexCaptor.capture());

    List<IndexDefinition> indexes = indexCaptor.getAllValues();
    assertThat("Unexpected number of indexes.", indexes.size(), is(1));

    List<String> indexKeys = indexes.stream()
        .flatMap(i -> i.getIndexKeys().keySet().stream())
        .toList();
    assertThat("Unexpected index.", indexKeys, hasItems("data.owner"));
  }

  @Test
  void shouldInitIndexesForProgrammeMembershipCollection() {
    IndexOperations indexOperations = mock(IndexOperations.class);
    when(template.indexOps(ProgrammeMembership.class)).thenReturn(indexOperations);

    configuration.initIndexes();

    ArgumentCaptor<IndexDefinition> indexCaptor = ArgumentCaptor.forClass(IndexDefinition.class);
    verify(indexOperations, atLeastOnce()).ensureIndex(indexCaptor.capture());

    List<IndexDefinition> indexes = indexCaptor.getAllValues();
    assertThat("Unexpected number of indexes.", indexes.size(), is(3));

    List<String> indexKeys = indexes.stream()
        .flatMap(i -> i.getIndexKeys().keySet().stream())
        .collect(Collectors.toList());
    assertThat("Unexpected index.", indexKeys,
        hasItems("programmeId", "personId", "programmeMembershipType", "programmeStartDate",
            "programmeEndDate"));
  }
}

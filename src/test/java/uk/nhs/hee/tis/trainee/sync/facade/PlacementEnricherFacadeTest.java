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

package uk.nhs.hee.tis.trainee.sync.facade;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.trainee.sync.model.Grade;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.Placement;
import uk.nhs.hee.tis.trainee.sync.model.PlacementSite;
import uk.nhs.hee.tis.trainee.sync.model.PlacementSpecialty;
import uk.nhs.hee.tis.trainee.sync.model.Post;
import uk.nhs.hee.tis.trainee.sync.model.PostSpecialty;
import uk.nhs.hee.tis.trainee.sync.model.Site;
import uk.nhs.hee.tis.trainee.sync.model.Specialty;
import uk.nhs.hee.tis.trainee.sync.model.Trust;
import uk.nhs.hee.tis.trainee.sync.service.GradeSyncService;
import uk.nhs.hee.tis.trainee.sync.service.PlacementSiteSyncService;
import uk.nhs.hee.tis.trainee.sync.service.PlacementSpecialtySyncService;
import uk.nhs.hee.tis.trainee.sync.service.PlacementSyncService;
import uk.nhs.hee.tis.trainee.sync.service.PostSpecialtySyncService;
import uk.nhs.hee.tis.trainee.sync.service.PostSyncService;
import uk.nhs.hee.tis.trainee.sync.service.SiteSyncService;
import uk.nhs.hee.tis.trainee.sync.service.SpecialtySyncService;
import uk.nhs.hee.tis.trainee.sync.service.TcsSyncService;
import uk.nhs.hee.tis.trainee.sync.service.TrustSyncService;

@ExtendWith(MockitoExtension.class)
class PlacementEnricherFacadeTest {

  private static final Random RANDOM = new Random();

  private static final String PLACEMENT_1_ID = String.valueOf(RANDOM.nextLong());

  private static final String PLACEMENT_SITE_1_ID = String.valueOf(RANDOM.nextLong());
  private static final String POST_1_ID = "post1";
  private static final String TRUST_1_ID = "trust1";
  private static final String TRUST_1_NAME = "Trust One";
  private static final String TRUST_2_ID = "trust2";
  private static final String TRUST_2_NAME = "Trust Two";
  private static final String SITE_1_ID = String.valueOf(RANDOM.nextLong());
  private static final String SITE_1_NAME = "Site One";
  private static final String SITE_1_LOCATION = "Site One Location";
  private static final String SITE_1_KNOWN_AS = "Site One Known As";
  private static final String SITE_2_ID = String.valueOf(RANDOM.nextLong());
  private static final String SITE_2_NAME = "Site Two";
  private static final String SITE_2_LOCATION = "Site Two Location";
  private static final String SITE_2_KNOWN_AS = "Site Two Known As";
  private static final String GRADE_1_ID = "grade1";
  private static final String GRADE_1_ABBR = "Grade One";
  private static final String SPECIALTY_1_ID = "specialty1";
  private static final String SPECIALTY_1_NAME = "Specialty One";

  private static final String DATA_PLACEMENT_SPECIALTY_PLACEMENT_ID = "placementId";
  private static final String DATA_PLACEMENT_SPECIALTY_SPECIALTY_ID = "specialtyId";
  private static final String DATA_POST_ID = "postId";
  private static final String DATA_POST_ALLOWS_SUBSPECIALTY = "postAllowsSubspecialty";
  private static final String DATA_POST_SPECIALTY_TYPE = "postSpecialtyType";
  private static final String DATA_EMPLOYING_BODY_ID = "employingBodyId";
  private static final String DATA_EMPLOYING_BODY_NAME = "employingBodyName";
  private static final String DATA_TRAINING_BODY_ID = "trainingBodyId";
  private static final String DATA_TRAINING_BODY_NAME = "trainingBodyName";
  private static final String DATA_TRUST_NAME = "trustKnownAs";
  private static final String PLACEMENT_DATA_SITE_ID = "siteId";
  private static final String PLACEMENT_DATA_SITE_NAME = "site";
  private static final String PLACEMENT_DATA_SITE_LOCATION = "siteLocation";
  private static final String PLACEMENT_DATA_SITE_KNOWN_AS = "siteKnownAs";
  private static final String PLACEMENT_DATA_OTHER_SITE_NAME = "otherSites";
  private static final String PLACEMENT_DATA_GRADE_ID = "gradeId";
  private static final String PLACEMENT_DATA_GRADE_ABBREVIATION = "gradeAbbreviation";
  private static final String DATA_SITE_ID = "id";
  private static final String DATA_SITE_NAME = "siteName";
  private static final String DATA_SITE_LOCATION = "address";
  private static final String DATA_SITE_KNOWN_AS = "siteKnownAs";
  private static final String DATA_GRADE_ID = "id";
  private static final String DATA_GRADE_ABBREVIATION = "abbreviation";
  private static final String DATA_SPECIALTY_ID = "id";
  private static final String DATA_SPECIALTY_NAME = "name";
  private static final String PLACEMENT_DATA_SPECIALTY_NAME = "specialty";

  @InjectMocks
  private PlacementEnricherFacade enricher;

  @Mock
  private PlacementSyncService placementService;

  @Mock
  private PlacementSiteSyncService placementSiteService;

  @Mock
  private PostSyncService postService;

  @Mock
  private TrustSyncService trustService;

  @Mock
  private SiteSyncService siteService;

  @Mock
  private GradeSyncService gradeService;

  @Mock
  private SpecialtySyncService specialtyService;

  @Mock
  private PlacementSpecialtySyncService placementSpecialtyService;

  @Mock
  private PostSpecialtySyncService postSpecialtyService;

  @Mock
  private TcsSyncService tcsSyncService;

  @Spy
  private final ObjectMapper objectMapper = new ObjectMapper();

  @InjectMocks
  @Spy
  private PlacementEnricherFacade enricherSpy;

  @Test
  void shouldEnrichPlacementWhenPostAndSameTrustsExist() {
    Placement placement = new Placement();
    placement.setTisId(PLACEMENT_1_ID);
    placement.setData(new HashMap<>(Map.of(DATA_POST_ID, POST_1_ID)));

    Post post = new Post();
    post.setTisId(POST_1_ID);
    post.setData(Map.of(
        DATA_EMPLOYING_BODY_ID, TRUST_1_ID,
        DATA_TRAINING_BODY_ID, TRUST_1_ID
    ));

    Trust trust1 = new Trust();
    trust1.setTisId(TRUST_1_ID);
    trust1.setData(Map.of(DATA_TRUST_NAME, TRUST_1_NAME));

    when(postService.findById(POST_1_ID)).thenReturn(Optional.of(post));
    when(trustService.findById(TRUST_1_ID)).thenReturn(Optional.of(trust1));

    enricher.enrich(placement);

    verify(placementService, never()).request(anyString());
    verify(postService, never()).request(anyString());
    verify(trustService, never()).request(anyString());

    verify(tcsSyncService).syncRecord(placement);
    verifyNoMoreInteractions(tcsSyncService);

    Map<String, String> placementData = placement.getData();
    assertThat("Unexpected employing body name.", placementData.get(DATA_EMPLOYING_BODY_NAME),
        is(TRUST_1_NAME));
    assertThat("Unexpected training body name.", placementData.get(DATA_TRAINING_BODY_NAME),
        is(TRUST_1_NAME));
  }

  @Test
  void shouldEnrichPlacementWhenPostAndDifferentTrustsExist() {
    Placement placement = new Placement();
    placement.setTisId(PLACEMENT_1_ID);
    placement.setData(new HashMap<>(Map.of(DATA_POST_ID, POST_1_ID)));

    Post post = new Post();
    post.setTisId(POST_1_ID);
    post.setData(Map.of(
        DATA_EMPLOYING_BODY_ID, TRUST_1_ID,
        DATA_TRAINING_BODY_ID, TRUST_2_ID
    ));

    Trust trust1 = new Trust();
    trust1.setTisId(TRUST_1_ID);
    trust1.setData(Map.of(DATA_TRUST_NAME, TRUST_1_NAME));

    Trust trust2 = new Trust();
    trust2.setTisId(TRUST_2_ID);
    trust2.setData(Map.of(DATA_TRUST_NAME, TRUST_2_NAME));

    when(postService.findById(POST_1_ID)).thenReturn(Optional.of(post));
    when(trustService.findById(TRUST_1_ID)).thenReturn(Optional.of(trust1));
    when(trustService.findById(TRUST_2_ID)).thenReturn(Optional.of(trust2));

    enricher.enrich(placement);

    verify(placementService, never()).request(anyString());
    verify(postService, never()).request(anyString());
    verify(trustService, never()).request(anyString());

    verify(tcsSyncService).syncRecord(placement);
    verifyNoMoreInteractions(tcsSyncService);

    Map<String, String> placementData = placement.getData();
    assertThat("Unexpected employing body name.", placementData.get(DATA_EMPLOYING_BODY_NAME),
        is(TRUST_1_NAME));
    assertThat("Unexpected training body name.", placementData.get(DATA_TRAINING_BODY_NAME),
        is(TRUST_2_NAME));
  }

  @Test
  void shouldEnrichPlacementWhenPostExistsAndNoEmployingBody() {
    Placement placement = new Placement();
    placement.setTisId(PLACEMENT_1_ID);
    placement.setData(new HashMap<>(Map.of(DATA_POST_ID, POST_1_ID)));

    Post post = new Post();
    post.setTisId(POST_1_ID);
    post.setData(Map.of(
        DATA_TRAINING_BODY_ID, TRUST_1_ID
    ));

    Trust trust1 = new Trust();
    trust1.setTisId(TRUST_1_ID);
    trust1.setData(Map.of(DATA_TRUST_NAME, TRUST_1_NAME));

    when(postService.findById(POST_1_ID)).thenReturn(Optional.of(post));
    when(trustService.findById(TRUST_1_ID)).thenReturn(Optional.of(trust1));

    enricher.enrich(placement);

    verify(placementService, never()).request(anyString());
    verify(postService, never()).request(anyString());
    verify(trustService, never()).request(anyString());

    verify(tcsSyncService).syncRecord(placement);
    verifyNoMoreInteractions(tcsSyncService);

    Map<String, String> placementData = placement.getData();
    assertThat("Unexpected employing body name.", placementData.get(DATA_EMPLOYING_BODY_NAME),
        nullValue());
    assertThat("Unexpected training body name.", placementData.get(DATA_TRAINING_BODY_NAME),
        is(TRUST_1_NAME));
  }

  @Test
  void shouldEnrichPlacementWhenPostExistsAndNoTrainingBody() {
    Placement placement = new Placement();
    placement.setTisId(PLACEMENT_1_ID);
    placement.setData(new HashMap<>(Map.of(DATA_POST_ID, POST_1_ID)));

    Post post = new Post();
    post.setTisId(POST_1_ID);
    post.setData(Map.of(
        DATA_EMPLOYING_BODY_ID, TRUST_1_ID
    ));

    Trust trust1 = new Trust();
    trust1.setTisId(TRUST_1_ID);
    trust1.setData(Map.of(DATA_TRUST_NAME, TRUST_1_NAME));

    when(postService.findById(POST_1_ID)).thenReturn(Optional.of(post));
    when(trustService.findById(TRUST_1_ID)).thenReturn(Optional.of(trust1));

    enricher.enrich(placement);

    verify(placementService, never()).request(anyString());
    verify(postService, never()).request(anyString());
    verify(trustService, never()).request(anyString());

    verify(tcsSyncService).syncRecord(placement);
    verifyNoMoreInteractions(tcsSyncService);

    Map<String, String> placementData = placement.getData();
    assertThat("Unexpected employing body name.", placementData.get(DATA_EMPLOYING_BODY_NAME),
        is(TRUST_1_NAME));
    assertThat("Unexpected training body name.", placementData.get(DATA_TRAINING_BODY_NAME),
        nullValue());
  }

  @Test
  void shouldEnrichPlacementWhenPostExistsAndNoEmployingAndTrainingBody() {
    Placement placement = new Placement();
    placement.setTisId(PLACEMENT_1_ID);
    placement.setData(new HashMap<>(Map.of(DATA_POST_ID, POST_1_ID)));

    Post post = new Post();
    post.setTisId(POST_1_ID);

    when(postService.findById(POST_1_ID)).thenReturn(Optional.of(post));

    enricher.enrich(placement);

    verify(placementService, never()).request(anyString());
    verify(postService, never()).request(anyString());
    verifyNoMoreInteractions(trustService);

    verify(tcsSyncService).syncRecord(placement);
    verifyNoMoreInteractions(tcsSyncService);

    Map<String, String> placementData = placement.getData();
    assertThat("Unexpected employing body name.", placementData.get(DATA_EMPLOYING_BODY_NAME),
        nullValue());
    assertThat("Unexpected training body name.", placementData.get(DATA_TRAINING_BODY_NAME),
        nullValue());
  }

  @Test
  void shouldNotEnrichPlacementWhenPostAndOnlyEmployingBodyTrustExists() {
    Placement placement = new Placement();
    placement.setTisId(PLACEMENT_1_ID);
    placement.setData(new HashMap<>(Map.of(DATA_POST_ID, POST_1_ID)));

    Post post = new Post();
    post.setTisId(POST_1_ID);
    post.setData(Map.of(
        DATA_EMPLOYING_BODY_ID, TRUST_1_ID,
        DATA_TRAINING_BODY_ID, TRUST_2_ID
    ));

    Trust trust1 = new Trust();
    trust1.setTisId(TRUST_1_ID);
    trust1.setData(Map.of(DATA_TRUST_NAME, TRUST_1_NAME));

    when(postService.findById(POST_1_ID)).thenReturn(Optional.of(post));
    when(trustService.findById(TRUST_1_ID)).thenReturn(Optional.of(trust1));
    when(trustService.findById(TRUST_2_ID)).thenReturn(Optional.empty());

    enricher.enrich(placement);

    verify(placementService, never()).request(anyString());
    verify(postService, never()).request(anyString());
    verify(trustService, never()).request(TRUST_1_ID);
    verify(trustService).request(TRUST_2_ID);

    verifyNoInteractions(tcsSyncService);

    Map<String, String> placementData = placement.getData();
    assertThat("Unexpected employing body name.", placementData.get(DATA_EMPLOYING_BODY_NAME),
        nullValue());
    assertThat("Unexpected training body name.", placementData.get(DATA_TRAINING_BODY_NAME),
        nullValue());
  }

  @Test
  void shouldNotEnrichPlacementWhenPostAndOnlyTrainingBodyTrustExists() {
    Placement placement = new Placement();
    placement.setTisId(PLACEMENT_1_ID);
    placement.setData(new HashMap<>(Map.of(DATA_POST_ID, POST_1_ID)));

    Post post = new Post();
    post.setTisId(POST_1_ID);
    post.setData(Map.of(
        DATA_EMPLOYING_BODY_ID, TRUST_1_ID,
        DATA_TRAINING_BODY_ID, TRUST_2_ID
    ));

    Trust trust2 = new Trust();
    trust2.setTisId(TRUST_2_ID);
    trust2.setData(Map.of(DATA_TRUST_NAME, TRUST_2_NAME));

    when(postService.findById(POST_1_ID)).thenReturn(Optional.of(post));
    when(trustService.findById(TRUST_1_ID)).thenReturn(Optional.empty());
    when(trustService.findById(TRUST_2_ID)).thenReturn(Optional.of(trust2));

    enricher.enrich(placement);

    verify(placementService, never()).request(anyString());
    verify(postService, never()).request(anyString());
    verify(trustService).request(TRUST_1_ID);
    verify(trustService, never()).request(TRUST_2_ID);

    verifyNoInteractions(tcsSyncService);

    Map<String, String> placementData = placement.getData();
    assertThat("Unexpected employing body name.", placementData.get(DATA_EMPLOYING_BODY_NAME),
        nullValue());
    assertThat("Unexpected training body name.", placementData.get(DATA_TRAINING_BODY_NAME),
        nullValue());
  }

  @Test
  void shouldNotEnrichPlacementWhenPostExistsAndTrustsNotExist() {
    Placement placement = new Placement();
    placement.setTisId(PLACEMENT_1_ID);
    placement.setData(new HashMap<>(Map.of(DATA_POST_ID, POST_1_ID)));

    Post post = new Post();
    post.setTisId(POST_1_ID);
    post.setData(Map.of(
        DATA_EMPLOYING_BODY_ID, TRUST_1_ID,
        DATA_TRAINING_BODY_ID, TRUST_2_ID
    ));

    Trust trust1 = new Trust();
    trust1.setTisId(TRUST_1_ID);
    trust1.setData(Map.of(DATA_TRUST_NAME, TRUST_1_NAME));

    Trust trust2 = new Trust();
    trust2.setTisId(TRUST_2_ID);
    trust2.setData(Map.of(DATA_TRUST_NAME, TRUST_2_NAME));

    when(postService.findById(POST_1_ID)).thenReturn(Optional.of(post));
    when(trustService.findById(TRUST_1_ID)).thenReturn(Optional.empty());
    when(trustService.findById(TRUST_2_ID)).thenReturn(Optional.empty());

    enricher.enrich(placement);

    verify(placementService, never()).request(anyString());
    verify(postService, never()).request(anyString());
    verify(trustService).request(TRUST_1_ID);
    verify(trustService).request(TRUST_2_ID);

    verifyNoInteractions(tcsSyncService);

    Map<String, String> placementData = placement.getData();
    assertThat("Unexpected employing body name.", placementData.get(DATA_EMPLOYING_BODY_NAME),
        nullValue());
    assertThat("Unexpected training body name.", placementData.get(DATA_TRAINING_BODY_NAME),
        nullValue());
  }

  @Test
  void shouldNotEnrichPlacementWhenPostNotExists() {
    Placement placement = new Placement();
    placement.setTisId(PLACEMENT_1_ID);
    placement.setData(new HashMap<>(Map.of(DATA_POST_ID, POST_1_ID)));

    Post post = new Post();
    post.setTisId(POST_1_ID);
    post.setData(Map.of(
        DATA_EMPLOYING_BODY_ID, TRUST_1_ID,
        DATA_TRAINING_BODY_ID, TRUST_2_ID
    ));

    when(postService.findById(POST_1_ID)).thenReturn(Optional.empty());

    enricher.enrich(placement);

    verify(placementService, never()).request(anyString());
    verify(postService).request(POST_1_ID);
    verifyNoInteractions(trustService);

    verifyNoInteractions(tcsSyncService);

    Map<String, String> placementData = placement.getData();
    assertThat("Unexpected employing body name.", placementData.get(DATA_EMPLOYING_BODY_NAME),
        nullValue());
    assertThat("Unexpected training body name.", placementData.get(DATA_TRAINING_BODY_NAME),
        nullValue());
  }

  @Test
  void shouldNotEnrichPlacementWhenSiteNotExists() {
    Placement placement = new Placement();
    placement.setTisId(PLACEMENT_1_ID);
    placement.setData(new HashMap<>(Map.of(PLACEMENT_DATA_SITE_ID, SITE_1_ID)));

    Site site = new Site();
    site.setTisId(SITE_1_ID);
    site.setData(Map.of(
        DATA_SITE_ID, SITE_1_ID,
        DATA_SITE_NAME, SITE_1_NAME,
        DATA_SITE_LOCATION, SITE_1_LOCATION,
        DATA_SITE_KNOWN_AS, SITE_1_KNOWN_AS
    ));

    when(siteService.findById(SITE_1_ID)).thenReturn(Optional.empty());

    enricher.enrich(placement);

    verify(placementService, never()).request(anyString());
    verify(siteService).request(SITE_1_ID);
    verifyNoInteractions(trustService);

    verifyNoInteractions(tcsSyncService);

    Map<String, String> placementData = placement.getData();
    assertThat("Unexpected site name.", placementData.get(PLACEMENT_DATA_SITE_NAME),
        nullValue());
    assertThat("Unexpected site location.", placementData.get(PLACEMENT_DATA_SITE_LOCATION),
        nullValue());
    assertThat("Unexpected site known as.", placementData.get(PLACEMENT_DATA_SITE_KNOWN_AS),
        nullValue());
  }

  @Test
  void shouldStillEnrichPlacementWhenSiteExistsButHasNoSiteLocation() {
    Placement placement = new Placement();
    placement.setTisId(PLACEMENT_1_ID);
    placement.setData(new HashMap<>(Map.of(PLACEMENT_DATA_SITE_ID, SITE_1_ID)));

    Site site = new Site();
    site.setTisId(SITE_1_ID);
    site.setData(Map.of(
        DATA_SITE_ID, SITE_1_ID,
        DATA_SITE_NAME, SITE_1_NAME
    ));

    when(siteService.findById(SITE_1_ID)).thenReturn(Optional.of(site));

    enricher.enrich(placement);

    verify(placementService, never()).request(anyString());

    verify(tcsSyncService).syncRecord(placement);
    verifyNoMoreInteractions(tcsSyncService);

    Map<String, String> placementData = placement.getData();
    assertThat("Unexpected site name.", placementData.get(PLACEMENT_DATA_SITE_NAME),
        is(SITE_1_NAME));
  }

  @Test
  void shouldEnrichPlacementWhenSiteExists() {
    Placement placement = new Placement();
    placement.setTisId(PLACEMENT_1_ID);
    placement.setData(new HashMap<>(Map.of(PLACEMENT_DATA_SITE_ID, SITE_1_ID)));

    Site site = new Site();
    site.setTisId(SITE_1_ID);
    site.setData(Map.of(
        DATA_SITE_ID, SITE_1_ID,
        DATA_SITE_NAME, SITE_1_NAME,
        DATA_SITE_LOCATION, SITE_1_LOCATION,
        DATA_SITE_KNOWN_AS, SITE_1_KNOWN_AS
    ));

    when(siteService.findById(SITE_1_ID)).thenReturn(Optional.of(site));

    enricher.enrich(placement);

    verify(placementService, never()).request(anyString());

    verify(tcsSyncService).syncRecord(placement);
    verifyNoMoreInteractions(tcsSyncService);

    Map<String, String> placementData = placement.getData();
    assertThat("Unexpected site name.", placementData.get(PLACEMENT_DATA_SITE_NAME),
        is(SITE_1_NAME));
    assertThat("Unexpected site location.", placementData.get(PLACEMENT_DATA_SITE_LOCATION),
        is(SITE_1_LOCATION));
    assertThat("Unexpected site known as.", placementData.get(PLACEMENT_DATA_SITE_KNOWN_AS),
        is(SITE_1_KNOWN_AS));
  }

  @Test
  void shouldNotEnrichPlacementWhenSiteExistsWithNoSiteNameNorSiteLocationNorSiteKnownAs() {
    Placement placement = new Placement();
    placement.setTisId(PLACEMENT_1_ID);
    placement.setData(new HashMap<>(Map.of(PLACEMENT_DATA_SITE_ID, SITE_1_ID)));

    Site site = new Site();
    site.setTisId(SITE_1_ID);
    site.setData(Map.of(
        DATA_SITE_ID, SITE_1_ID
    ));

    when(siteService.findById(SITE_1_ID)).thenReturn(Optional.of(site));

    enricher.enrich(placement);

    verify(placementService, never()).request(anyString());
    verifyNoInteractions(trustService);

    verifyNoInteractions(tcsSyncService);

    Map<String, String> placementData = placement.getData();
    assertThat("Unexpected site name.", placementData.get(PLACEMENT_DATA_SITE_NAME),
        nullValue());
    assertThat("Unexpected site location.", placementData.get(PLACEMENT_DATA_SITE_LOCATION),
        nullValue());
    assertThat("Unexpected site known as.", placementData.get(PLACEMENT_DATA_SITE_KNOWN_AS),
        nullValue());
  }

  @Test
  void shouldStillEnrichPlacementWhenNoOtherSites() throws JsonProcessingException {
    Placement placement = new Placement();
    placement.setTisId(PLACEMENT_1_ID);

    when(placementSiteService.findOtherSitesByPlacementId(
        Long.parseLong(PLACEMENT_1_ID))).thenReturn(Set.of());

    enricher.enrich(placement);

    verify(placementService, never()).request(anyString());
    verifyNoInteractions(siteService);

    verify(tcsSyncService).syncRecord(placement);
    verifyNoMoreInteractions(tcsSyncService);

    Map<String, String> placementData = placement.getData();
    Set<Map<String, String>> otherSites = objectMapper.readValue(
        placementData.get(PLACEMENT_DATA_OTHER_SITE_NAME), new TypeReference<>() {
        });
    assertThat("Unexpected other sites count.", otherSites.size(), is(0));
  }

  @Test
  void shouldStillEnrichPlacementWhenOtherSiteMissingSiteId() throws JsonProcessingException {
    Placement placement = new Placement();
    placement.setTisId(PLACEMENT_1_ID);

    PlacementSite placementSite = new PlacementSite();
    placementSite.setId(Long.parseLong(PLACEMENT_SITE_1_ID));
    placementSite.setPlacementId(Long.parseLong(PLACEMENT_1_ID));
    when(placementSiteService.findOtherSitesByPlacementId(
        Long.parseLong(PLACEMENT_1_ID))).thenReturn(Set.of(placementSite));

    enricher.enrich(placement);

    verify(placementService, never()).request(anyString());
    verifyNoInteractions(siteService);

    verify(tcsSyncService).syncRecord(placement);
    verifyNoMoreInteractions(tcsSyncService);

    Map<String, String> placementData = placement.getData();
    Set<Map<String, String>> otherSites = objectMapper.readValue(
        placementData.get(PLACEMENT_DATA_OTHER_SITE_NAME), new TypeReference<>() {
        });
    assertThat("Unexpected other sites count.", otherSites.size(), is(0));
  }

  @Test
  void shouldNotEnrichPlacementWhenOtherSiteNotExists() {
    Placement placement = new Placement();
    placement.setTisId(PLACEMENT_1_ID);

    PlacementSite placementSite = new PlacementSite();
    placementSite.setId(Long.parseLong(PLACEMENT_SITE_1_ID));
    placementSite.setPlacementId(Long.parseLong(PLACEMENT_1_ID));
    placementSite.setSiteId(Long.parseLong(SITE_1_ID));

    when(placementSiteService.findOtherSitesByPlacementId(
        Long.parseLong(PLACEMENT_1_ID))).thenReturn(Set.of(placementSite));
    when(siteService.findById(SITE_1_ID)).thenReturn(Optional.empty());

    enricher.enrich(placement);

    verify(placementService, never()).request(anyString());
    verify(siteService).request(SITE_1_ID);

    verifyNoInteractions(tcsSyncService);

    Map<String, String> placementData = placement.getData();
    assertThat("Unexpected other sites.", placementData.get(PLACEMENT_DATA_OTHER_SITE_NAME),
        nullValue());
  }

  @Test
  void shouldStillEnrichPlacementWhenOtherSiteExistsButHasNoSiteLocation()
      throws JsonProcessingException {
    Placement placement = new Placement();
    placement.setTisId(PLACEMENT_1_ID);

    PlacementSite placementSite = new PlacementSite();
    placementSite.setId(Long.parseLong(PLACEMENT_SITE_1_ID));
    placementSite.setPlacementId(Long.parseLong(PLACEMENT_1_ID));
    placementSite.setSiteId(Long.parseLong(SITE_1_ID));
    when(placementSiteService.findOtherSitesByPlacementId(
        Long.parseLong(PLACEMENT_1_ID))).thenReturn(Set.of(placementSite));

    Site site = new Site();
    site.setTisId(SITE_1_ID);
    site.setData(Map.of(
        DATA_SITE_ID, SITE_1_ID,
        DATA_SITE_NAME, SITE_1_NAME,
        DATA_SITE_KNOWN_AS, SITE_1_KNOWN_AS
    ));
    when(siteService.findById(SITE_1_ID)).thenReturn(Optional.of(site));

    enricher.enrich(placement);

    verify(placementService, never()).request(anyString());

    verify(tcsSyncService).syncRecord(placement);
    verifyNoMoreInteractions(tcsSyncService);

    Map<String, String> placementData = placement.getData();
    Set<Map<String, String>> otherSites = objectMapper.readValue(
        placementData.get(PLACEMENT_DATA_OTHER_SITE_NAME), new TypeReference<>() {
        });
    assertThat("Unexpected other sites count.", otherSites.size(), is(1));

    Map<String, String> otherSite = otherSites.iterator().next();
    assertThat("Unexpected site name.", otherSite.get(PLACEMENT_DATA_SITE_NAME), is(SITE_1_NAME));
    assertThat("Unexpected site location.", otherSite.get(PLACEMENT_DATA_SITE_LOCATION),
        nullValue());
    assertThat("Unexpected site known as.", otherSite.get(PLACEMENT_DATA_SITE_KNOWN_AS),
        is(SITE_1_KNOWN_AS));
  }

  @Test
  void shouldStillEnrichPlacementWhenOtherSiteExistsButHasNoSiteKnownAs()
      throws JsonProcessingException {
    Placement placement = new Placement();
    placement.setTisId(PLACEMENT_1_ID);

    PlacementSite placementSite = new PlacementSite();
    placementSite.setId(Long.parseLong(PLACEMENT_SITE_1_ID));
    placementSite.setPlacementId(Long.parseLong(PLACEMENT_1_ID));
    placementSite.setSiteId(Long.parseLong(SITE_1_ID));
    when(placementSiteService.findOtherSitesByPlacementId(
        Long.parseLong(PLACEMENT_1_ID))).thenReturn(Set.of(placementSite));

    Site site = new Site();
    site.setTisId(SITE_1_ID);
    site.setData(Map.of(
        DATA_SITE_ID, SITE_1_ID,
        DATA_SITE_NAME, SITE_1_NAME,
        DATA_SITE_LOCATION, SITE_1_LOCATION
    ));
    when(siteService.findById(SITE_1_ID)).thenReturn(Optional.of(site));

    enricher.enrich(placement);

    verify(placementService, never()).request(anyString());

    verify(tcsSyncService).syncRecord(placement);
    verifyNoMoreInteractions(tcsSyncService);

    Map<String, String> placementData = placement.getData();
    Set<Map<String, String>> otherSites = objectMapper.readValue(
        placementData.get(PLACEMENT_DATA_OTHER_SITE_NAME), new TypeReference<>() {
        });
    assertThat("Unexpected other sites count.", otherSites.size(), is(1));

    Map<String, String> otherSite = otherSites.iterator().next();
    assertThat("Unexpected site name.", otherSite.get(PLACEMENT_DATA_SITE_NAME), is(SITE_1_NAME));
    assertThat("Unexpected site location.", otherSite.get(PLACEMENT_DATA_SITE_LOCATION),
        is(SITE_1_LOCATION));
    assertThat("Unexpected site known as.", otherSite.get(PLACEMENT_DATA_SITE_KNOWN_AS),
        nullValue());
  }

  @Test
  void shouldThrowExceptionWhenOtherSiteNotReadable() throws JsonProcessingException {
    Placement placement = new Placement();
    placement.setTisId(PLACEMENT_1_ID);

    PlacementSite placementSite = new PlacementSite();
    placementSite.setId(Long.parseLong(PLACEMENT_SITE_1_ID));
    placementSite.setPlacementId(Long.parseLong(PLACEMENT_1_ID));
    placementSite.setSiteId(Long.parseLong(SITE_1_ID));
    when(placementSiteService.findOtherSitesByPlacementId(
        Long.parseLong(PLACEMENT_1_ID))).thenReturn(Set.of(placementSite));

    Site site = new Site();
    site.setTisId(SITE_1_ID);
    site.setData(Map.of(
        DATA_SITE_ID, SITE_1_ID,
        DATA_SITE_NAME, SITE_1_NAME,
        DATA_SITE_LOCATION, SITE_1_LOCATION,
        DATA_SITE_KNOWN_AS, SITE_1_KNOWN_AS
    ));
    when(siteService.findById(SITE_1_ID)).thenReturn(Optional.of(site));

    when(objectMapper.writeValueAsString(any())).thenThrow(
        new JsonGenerationException("Unit test exception.", null, null));

    assertThrows(RuntimeException.class, () -> enricher.enrich(placement));
  }

  @Test
  void shouldEnrichPlacementWhenOtherSiteExists() throws JsonProcessingException {
    Placement placement = new Placement();
    placement.setTisId(PLACEMENT_1_ID);

    PlacementSite placementSite = new PlacementSite();
    placementSite.setId(Long.parseLong(PLACEMENT_SITE_1_ID));
    placementSite.setPlacementId(Long.parseLong(PLACEMENT_1_ID));
    placementSite.setSiteId(Long.parseLong(SITE_1_ID));
    when(placementSiteService.findOtherSitesByPlacementId(
        Long.parseLong(PLACEMENT_1_ID))).thenReturn(Set.of(placementSite));

    Site site = new Site();
    site.setTisId(SITE_1_ID);
    site.setData(Map.of(
        DATA_SITE_ID, SITE_1_ID,
        DATA_SITE_NAME, SITE_1_NAME,
        DATA_SITE_LOCATION, SITE_1_LOCATION,
        DATA_SITE_KNOWN_AS, SITE_1_KNOWN_AS
    ));
    when(siteService.findById(SITE_1_ID)).thenReturn(Optional.of(site));

    enricher.enrich(placement);

    verify(placementService, never()).request(anyString());

    verify(tcsSyncService).syncRecord(placement);
    verifyNoMoreInteractions(tcsSyncService);

    Map<String, String> placementData = placement.getData();
    Set<Map<String, String>> otherSites = objectMapper.readValue(
        placementData.get(PLACEMENT_DATA_OTHER_SITE_NAME), new TypeReference<>() {
        });
    assertThat("Unexpected other sites count.", otherSites.size(), is(1));

    Map<String, String> otherSite = otherSites.iterator().next();
    assertThat("Unexpected site name.", otherSite.get(PLACEMENT_DATA_SITE_NAME), is(SITE_1_NAME));
    assertThat("Unexpected site location.", otherSite.get(PLACEMENT_DATA_SITE_LOCATION),
        is(SITE_1_LOCATION));
    assertThat("Unexpected site known as.", otherSite.get(PLACEMENT_DATA_SITE_KNOWN_AS),
        is(SITE_1_KNOWN_AS));
  }

  @Test
  void shouldEnrichPlacementWhenMultipleOtherSitesExist() throws JsonProcessingException {
    Placement placement = new Placement();
    placement.setTisId(PLACEMENT_1_ID);

    PlacementSite placementSite1 = new PlacementSite();
    placementSite1.setId(Long.parseLong(PLACEMENT_SITE_1_ID));
    placementSite1.setPlacementId(Long.parseLong(PLACEMENT_1_ID));
    placementSite1.setSiteId(Long.parseLong(SITE_1_ID));

    PlacementSite placementSite2 = new PlacementSite();
    placementSite2.setId(1L);
    placementSite2.setPlacementId(Long.parseLong(PLACEMENT_1_ID));
    placementSite2.setSiteId(Long.parseLong(SITE_2_ID));

    when(placementSiteService.findOtherSitesByPlacementId(
        Long.parseLong(PLACEMENT_1_ID))).thenReturn(Set.of(placementSite1, placementSite2));

    Site site1 = new Site();
    site1.setTisId(SITE_1_ID);
    site1.setData(Map.of(
        DATA_SITE_ID, SITE_1_ID,
        DATA_SITE_NAME, SITE_1_NAME,
        DATA_SITE_LOCATION, SITE_1_LOCATION,
        DATA_SITE_KNOWN_AS, SITE_1_KNOWN_AS
    ));
    when(siteService.findById(SITE_1_ID)).thenReturn(Optional.of(site1));

    Site site2 = new Site();
    site2.setTisId(SITE_2_ID);
    site2.setData(Map.of(
        DATA_SITE_ID, SITE_2_ID,
        DATA_SITE_NAME, SITE_2_NAME,
        DATA_SITE_LOCATION, SITE_2_LOCATION,
        DATA_SITE_KNOWN_AS, SITE_2_KNOWN_AS
    ));
    when(siteService.findById(SITE_2_ID)).thenReturn(Optional.of(site2));

    enricher.enrich(placement);

    verify(placementService, never()).request(anyString());

    verify(tcsSyncService).syncRecord(placement);
    verifyNoMoreInteractions(tcsSyncService);

    Map<String, String> placementData = placement.getData();
    Set<Map<String, String>> otherSites = objectMapper.readValue(
        placementData.get(PLACEMENT_DATA_OTHER_SITE_NAME), new TypeReference<>() {
        });
    assertThat("Unexpected other sites count.", otherSites.size(), is(2));

    List<Map<String, String>> sortedOtherSites = otherSites.stream()
        .sorted(Comparator.comparing(os -> os.get(PLACEMENT_DATA_SITE_NAME))).toList();

    Map<String, String> otherSite1 = sortedOtherSites.get(0);
    assertThat("Unexpected site name.", otherSite1.get(PLACEMENT_DATA_SITE_NAME), is(SITE_1_NAME));
    assertThat("Unexpected site location.", otherSite1.get(PLACEMENT_DATA_SITE_LOCATION),
        is(SITE_1_LOCATION));
    assertThat("Unexpected site known as.", otherSite1.get(PLACEMENT_DATA_SITE_KNOWN_AS),
        is(SITE_1_KNOWN_AS));

    Map<String, String> otherSite2 = sortedOtherSites.get(1);
    assertThat("Unexpected site name.", otherSite2.get(PLACEMENT_DATA_SITE_NAME), is(SITE_2_NAME));
    assertThat("Unexpected site location.", otherSite2.get(PLACEMENT_DATA_SITE_LOCATION),
        is(SITE_2_LOCATION));
    assertThat("Unexpected site known as.", otherSite2.get(PLACEMENT_DATA_SITE_KNOWN_AS),
        is(SITE_2_KNOWN_AS));
  }

  @Test
  void shouldEnrichPlacementWhenDuplicateOtherSitesExist() throws JsonProcessingException {
    Placement placement = new Placement();
    placement.setTisId(PLACEMENT_1_ID);

    PlacementSite placementSite1 = new PlacementSite();
    placementSite1.setId(Long.parseLong(PLACEMENT_SITE_1_ID));
    placementSite1.setPlacementId(Long.parseLong(PLACEMENT_1_ID));
    placementSite1.setSiteId(Long.parseLong(SITE_1_ID));

    PlacementSite placementSite2 = new PlacementSite();
    placementSite2.setId(1L);
    placementSite2.setPlacementId(Long.parseLong(PLACEMENT_1_ID));
    placementSite2.setSiteId(Long.parseLong(SITE_1_ID));

    when(placementSiteService.findOtherSitesByPlacementId(
        Long.parseLong(PLACEMENT_1_ID))).thenReturn(Set.of(placementSite1, placementSite2));

    Site site1 = new Site();
    site1.setTisId(SITE_1_ID);
    site1.setData(Map.of(
        DATA_SITE_ID, SITE_1_ID,
        DATA_SITE_NAME, SITE_1_NAME,
        DATA_SITE_LOCATION, SITE_1_LOCATION,
        DATA_SITE_KNOWN_AS, SITE_1_KNOWN_AS
    ));
    when(siteService.findById(SITE_1_ID)).thenReturn(Optional.of(site1));

    enricher.enrich(placement);

    verify(placementService, never()).request(anyString());

    verify(tcsSyncService).syncRecord(placement);
    verifyNoMoreInteractions(tcsSyncService);

    Map<String, String> placementData = placement.getData();
    Set<Map<String, String>> otherSites = objectMapper.readValue(
        placementData.get(PLACEMENT_DATA_OTHER_SITE_NAME), new TypeReference<>() {
        });
    assertThat("Unexpected other sites count.", otherSites.size(), is(1));

    Map<String, String> otherSite = otherSites.iterator().next();
    assertThat("Unexpected site name.", otherSite.get(PLACEMENT_DATA_SITE_NAME), is(SITE_1_NAME));
    assertThat("Unexpected site location.", otherSite.get(PLACEMENT_DATA_SITE_LOCATION),
        is(SITE_1_LOCATION));
    assertThat("Unexpected site known as.", otherSite.get(PLACEMENT_DATA_SITE_KNOWN_AS),
        is(SITE_1_KNOWN_AS));
  }

  @Test
  void shouldNotEnrichPlacementWhenOtherSiteExistsWithNoSiteNameNorSiteLocationNorSiteKnownAs() {
    Placement placement = new Placement();
    placement.setTisId(PLACEMENT_1_ID);

    PlacementSite placementSite = new PlacementSite();
    placementSite.setId(Long.parseLong(PLACEMENT_SITE_1_ID));
    placementSite.setPlacementId(Long.parseLong(PLACEMENT_1_ID));
    placementSite.setSiteId(Long.parseLong(SITE_1_ID));
    when(placementSiteService.findOtherSitesByPlacementId(
        Long.parseLong(PLACEMENT_1_ID))).thenReturn(Set.of(placementSite));

    Site site = new Site();
    site.setTisId(SITE_1_ID);
    site.setData(Map.of(
        DATA_SITE_ID, SITE_1_ID
    ));
    when(siteService.findById(SITE_1_ID)).thenReturn(Optional.of(site));

    enricher.enrich(placement);

    verify(placementService, never()).request(anyString());
    verifyNoInteractions(trustService);

    verifyNoInteractions(tcsSyncService);

    Map<String, String> placementData = placement.getData();
    assertThat("Unexpected other sites.", placementData.get(PLACEMENT_DATA_OTHER_SITE_NAME),
        nullValue());
  }

  @Test
  void shouldEnrichPlacementWhenPlacementSpecialtyNotExists() {
    Placement placement = new Placement();
    placement.setTisId(PLACEMENT_1_ID);
    placement.setData(new HashMap<>(Map.of(PLACEMENT_DATA_SITE_ID, SITE_1_ID,
        PLACEMENT_DATA_SPECIALTY_NAME, DATA_SPECIALTY_NAME)));

    Site site = new Site();
    site.setTisId(SITE_1_ID);
    site.setData(Map.of(
        DATA_SITE_ID, SITE_1_ID,
        DATA_SITE_NAME, SITE_1_NAME,
        DATA_SITE_LOCATION, SITE_1_LOCATION
    ));

    when(siteService.findById(SITE_1_ID)).thenReturn(Optional.of(site));
    when(placementSpecialtyService.findById(any())).thenReturn(Optional.empty());

    enricher.enrich(placement);

    verify(placementService, never()).request(anyString());

    verify(tcsSyncService).syncRecord(placement);
    verifyNoMoreInteractions(tcsSyncService);

    Map<String, String> placementData = placement.getData();
    assertThat("Unexpected specialty.", placementData.get(PLACEMENT_DATA_SPECIALTY_NAME),
        is(DATA_SPECIALTY_NAME));
  }

  @Test
  void shouldDeletePlacementWithCorrectDetails() {
    Placement placement = new Placement();

    enricher.delete(placement);

    assertThat("Operation should be DELETE", placement.getOperation(), is(Operation.DELETE));
    assertThat("Schema should be tcs", placement.getSchema(), is("tcs"));
    assertThat("Unexpected table", placement.getTable(), is(Placement.ENTITY_NAME));
    verify(tcsSyncService).syncRecord(placement);
  }

  @Test
  void shouldEnrichPlacementWhenPostAndSiteAndPlacementSpecialtyExist() {
    Specialty specialty = new Specialty();
    specialty.setTisId(SPECIALTY_1_ID);
    specialty.setData(Map.of(
        DATA_SPECIALTY_ID, SPECIALTY_1_ID,
        DATA_SPECIALTY_NAME, SPECIALTY_1_NAME
    ));

    Placement placement = new Placement();
    placement.setTisId(PLACEMENT_1_ID);
    placement.setData(new HashMap<>(Map.of(
        DATA_POST_ID, POST_1_ID,
        PLACEMENT_DATA_SITE_ID, SITE_1_ID
    )));

    PlacementSpecialty placementSpecialty = new PlacementSpecialty();
    placementSpecialty.setData(Map.of(
        DATA_PLACEMENT_SPECIALTY_PLACEMENT_ID, PLACEMENT_1_ID,
        DATA_PLACEMENT_SPECIALTY_SPECIALTY_ID, SPECIALTY_1_ID
    ));

    Post post = new Post();
    post.setTisId(POST_1_ID);
    post.setData(Map.of(
        DATA_EMPLOYING_BODY_ID, TRUST_1_ID,
        DATA_TRAINING_BODY_ID, TRUST_1_ID
    ));

    Site site = new Site();
    site.setTisId(SITE_1_ID);
    site.setData(Map.of(
        DATA_SITE_NAME, SITE_1_NAME
    ));

    Trust trust = new Trust();
    trust.setTisId(TRUST_1_ID);
    trust.setData(Map.of(DATA_TRUST_NAME, TRUST_1_NAME));

    when(postService.findById(POST_1_ID)).thenReturn(Optional.of(post));
    when(trustService.findById(TRUST_1_ID)).thenReturn(Optional.of(trust));
    when(siteService.findById(SITE_1_ID)).thenReturn(Optional.of(site));
    // note below: since only one primary specialty exists per placement, placement ID is used as
    // the primary key for placementSpecialty.
    // Hence 'findById(PLACEMENT_1_ID)'
    when(placementSpecialtyService.findById(PLACEMENT_1_ID))
        .thenReturn(Optional.of(placementSpecialty));
    when(specialtyService.findById(SPECIALTY_1_ID)).thenReturn(Optional.of(specialty));

    enricher.enrich(placement);

    verify(placementService, never()).request(anyString());
    verify(siteService, never()).request(anyString());

    verify(tcsSyncService).syncRecord(placement);
    verifyNoMoreInteractions(tcsSyncService);

    Map<String, String> placementData = placement.getData();
    assertThat("Unexpected specialty name.", placementData.get(PLACEMENT_DATA_SPECIALTY_NAME),
        is(SPECIALTY_1_NAME));
  }

  @Test
  void shouldNotEnrichPlacementWhenPostAndSiteAndPlacementSpecialtyExistButSpecialityDoesNot() {

    Placement placement = new Placement();
    placement.setTisId(PLACEMENT_1_ID);
    placement.setData(new HashMap<>(Map.of(
        DATA_POST_ID, POST_1_ID,
        PLACEMENT_DATA_SITE_ID, SITE_1_ID
    )));

    PlacementSpecialty placementSpecialty = new PlacementSpecialty();
    placementSpecialty.setData(Map.of(
        DATA_PLACEMENT_SPECIALTY_PLACEMENT_ID, PLACEMENT_1_ID,
        DATA_PLACEMENT_SPECIALTY_SPECIALTY_ID, SPECIALTY_1_ID
    ));

    Post post = new Post();
    post.setTisId(POST_1_ID);
    post.setData(Map.of(
        DATA_EMPLOYING_BODY_ID, TRUST_1_ID,
        DATA_TRAINING_BODY_ID, TRUST_1_ID
    ));

    Site site = new Site();
    site.setTisId(SITE_1_ID);
    site.setData(Map.of(
        DATA_SITE_NAME, SITE_1_NAME
    ));

    Trust trust = new Trust();
    trust.setTisId(TRUST_1_ID);
    trust.setData(Map.of(DATA_TRUST_NAME, TRUST_1_NAME));

    when(postService.findById(POST_1_ID)).thenReturn(Optional.of(post));
    when(trustService.findById(TRUST_1_ID)).thenReturn(Optional.of(trust));
    when(siteService.findById(SITE_1_ID)).thenReturn(Optional.of(site));
    // note below: since only one primary specialty exists per placement, placement ID is used as
    // the primary key for placementSpecialty.
    // Hence 'findById(PLACEMENT_1_ID)'
    when(placementSpecialtyService.findById(PLACEMENT_1_ID))
        .thenReturn(Optional.of(placementSpecialty));
    when(specialtyService.findById(SPECIALTY_1_ID)).thenReturn(Optional.empty());

    enricher.enrich(placement);

    verify(placementService, never()).request(anyString());
    verify(siteService, never()).request(anyString());

    verifyNoInteractions(tcsSyncService);

    Map<String, String> placementData = placement.getData();
    assertThat("Unexpected specialty name.", placementData.get(PLACEMENT_DATA_SPECIALTY_NAME),
        nullValue());
  }

  @Test
  void shouldNotEnrichPlacementWhenGradeNotExists() {
    Placement placement = new Placement();
    placement.setTisId(PLACEMENT_1_ID);
    placement.setData(new HashMap<>(Map.of(PLACEMENT_DATA_GRADE_ID, GRADE_1_ID)));

    Grade grade = new Grade();
    grade.setTisId(GRADE_1_ID);
    grade.setData(Map.of(
        DATA_GRADE_ID, GRADE_1_ID,
        DATA_GRADE_ABBREVIATION, GRADE_1_ABBR
    ));

    when(gradeService.findById(GRADE_1_ID)).thenReturn(Optional.empty());

    enricher.enrich(placement);

    verify(placementService, never()).request(anyString());
    verify(gradeService).request(GRADE_1_ID);
    verifyNoInteractions(trustService);

    verifyNoInteractions(tcsSyncService);

    Map<String, String> placementData = placement.getData();
    assertThat("Unexpected grade abbreviation.",
        placementData.get(PLACEMENT_DATA_GRADE_ABBREVIATION),
        nullValue());
  }

  @Test
  void shouldEnrichPlacementWhenGradeExists() {
    Placement placement = new Placement();
    placement.setTisId(PLACEMENT_1_ID);
    placement.setData(new HashMap<>(Map.of(PLACEMENT_DATA_GRADE_ID, GRADE_1_ID)));

    Grade grade = new Grade();
    grade.setTisId(GRADE_1_ID);
    grade.setData(Map.of(
        DATA_GRADE_ID, GRADE_1_ID,
        DATA_GRADE_ABBREVIATION, GRADE_1_ABBR
    ));

    when(gradeService.findById(GRADE_1_ID)).thenReturn(Optional.of(grade));

    enricher.enrich(placement);

    verify(placementService, never()).request(anyString());

    verify(tcsSyncService).syncRecord(placement);
    verifyNoMoreInteractions(tcsSyncService);

    Map<String, String> placementData = placement.getData();
    assertThat("Unexpected grade abbreviation.",
        placementData.get(PLACEMENT_DATA_GRADE_ABBREVIATION),
        is(GRADE_1_ABBR));
  }

  @Test
  void shouldNotEnrichPlacementWhenGradeExistsWithNoGradeAbbreviation() {
    Placement placement = new Placement();
    placement.setTisId(PLACEMENT_1_ID);
    placement.setData(new HashMap<>(Map.of(PLACEMENT_DATA_GRADE_ID, GRADE_1_ID)));

    Grade grade = new Grade();
    grade.setTisId(GRADE_1_ID);
    grade.setData(Map.of(
        DATA_GRADE_ID, GRADE_1_ID
    ));

    when(gradeService.findById(GRADE_1_ID)).thenReturn(Optional.of(grade));

    enricher.enrich(placement);

    verify(placementService, never()).request(anyString());

    verifyNoMoreInteractions(tcsSyncService);

    Map<String, String> placementData = placement.getData();
    assertThat("Unexpected grade abbreviation.",
        placementData.get(PLACEMENT_DATA_GRADE_ABBREVIATION),
        nullValue());
  }


  @Test
  void shouldSetPlacementPostAllowsSubspecialtyTrueIfTheseExist() {
    Placement placement = new Placement();
    placement.setTisId(PLACEMENT_1_ID);
    placement.setData(new HashMap<>(Map.of(DATA_POST_ID, POST_1_ID)));

    Post post = new Post();
    post.setTisId(POST_1_ID);
    post.setData(Map.of(
        DATA_EMPLOYING_BODY_ID, TRUST_1_ID,
        DATA_TRAINING_BODY_ID, TRUST_1_ID
    ));

    PostSpecialty postSpecialty = new PostSpecialty();
    postSpecialty.setData(Map.of(
        DATA_POST_ID, POST_1_ID,
        DATA_POST_SPECIALTY_TYPE, "SUB_SPECIALTY"
    ));

    Trust trust1 = new Trust();
    trust1.setTisId(TRUST_1_ID);
    trust1.setData(Map.of(DATA_TRUST_NAME, TRUST_1_NAME));

    when(postService.findById(POST_1_ID)).thenReturn(Optional.of(post));
    when(trustService.findById(TRUST_1_ID)).thenReturn(Optional.of(trust1));
    when(postSpecialtyService.findByPostId(POST_1_ID))
        .thenReturn(Collections.singleton(postSpecialty));

    enricher.enrich(placement);

    verify(placementService, never()).request(anyString());
    verify(postService, never()).request(anyString());
    verify(trustService, never()).request(anyString());

    verify(tcsSyncService).syncRecord(placement);
    verifyNoMoreInteractions(tcsSyncService);

    Map<String, String> placementData = placement.getData();
    assertThat("Unexpected postAllowsSubspecialty value.",
        placementData.get(DATA_POST_ALLOWS_SUBSPECIALTY), is("true"));
  }


  @Test
  void shouldSetPlacementPostAllowsSubspecialtyFalseIfTheseDoNotExist() {
    Placement placement = new Placement();
    placement.setTisId(PLACEMENT_1_ID);
    placement.setData(new HashMap<>(Map.of(DATA_POST_ID, POST_1_ID)));

    Post post = new Post();
    post.setTisId(POST_1_ID);
    post.setData(Map.of(
        DATA_EMPLOYING_BODY_ID, TRUST_1_ID,
        DATA_TRAINING_BODY_ID, TRUST_1_ID
    ));

    Trust trust1 = new Trust();
    trust1.setTisId(TRUST_1_ID);
    trust1.setData(Map.of(DATA_TRUST_NAME, TRUST_1_NAME));

    when(postService.findById(POST_1_ID)).thenReturn(Optional.of(post));
    when(trustService.findById(TRUST_1_ID)).thenReturn(Optional.of(trust1));
    when(postSpecialtyService.findByPostId(POST_1_ID)).thenReturn(Collections.emptySet());

    enricher.enrich(placement);

    verify(placementService, never()).request(anyString());
    verify(postService, never()).request(anyString());
    verify(trustService, never()).request(anyString());

    verify(tcsSyncService).syncRecord(placement);
    verifyNoMoreInteractions(tcsSyncService);

    Map<String, String> placementData = placement.getData();
    assertThat("Unexpected postAllowsSubspecialty value.",
        placementData.get(DATA_POST_ALLOWS_SUBSPECIALTY), is("false"));
  }
}

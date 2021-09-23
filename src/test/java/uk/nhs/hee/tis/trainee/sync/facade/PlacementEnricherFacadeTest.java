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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.internal.util.collections.Sets;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.Placement;
import uk.nhs.hee.tis.trainee.sync.model.PlacementSpecialty;
import uk.nhs.hee.tis.trainee.sync.model.Post;
import uk.nhs.hee.tis.trainee.sync.model.Site;
import uk.nhs.hee.tis.trainee.sync.model.Specialty;
import uk.nhs.hee.tis.trainee.sync.model.Trust;
import uk.nhs.hee.tis.trainee.sync.service.PlacementSpecialtySyncService;
import uk.nhs.hee.tis.trainee.sync.service.PlacementSyncService;
import uk.nhs.hee.tis.trainee.sync.service.PostSyncService;
import uk.nhs.hee.tis.trainee.sync.service.SiteSyncService;
import uk.nhs.hee.tis.trainee.sync.service.SpecialtySyncService;
import uk.nhs.hee.tis.trainee.sync.service.TcsSyncService;
import uk.nhs.hee.tis.trainee.sync.service.TrustSyncService;

@ExtendWith(MockitoExtension.class)
class PlacementEnricherFacadeTest {

  private static final String PLACEMENT_1_ID = "placement1";
  private static final String PLACEMENT_2_ID = "placement2";
  private static final String POST_1_ID = "post1";
  private static final String TRUST_1_ID = "trust1";
  private static final String TRUST_1_NAME = "Trust One";
  private static final String TRUST_2_ID = "trust2";
  private static final String TRUST_2_NAME = "Trust Two";
  private static final String SITE_1_ID = "site1";
  private static final String SITE_1_NAME = "Site One";
  private static final String SITE_1_LOCATION = "Site One Location";
  private static final String SPECIALTY_1_ID = "specialty1";
  private static final String SPECIALTY_1_NAME = "Specialty One";
  private static final String SPECIALTY_2_ID = "specialty2";

  private static final String DATA_PLACEMENT_SPECIALTY_PLACEMENT_ID = "placementId";
  private static final String DATA_PLACEMENT_SPECIALTY_SPECIALTY_ID = "specialtyId";
  private static final String DATA_POST_ID = "postId";
  private static final String DATA_EMPLOYING_BODY_ID = "employingBodyId";
  private static final String DATA_EMPLOYING_BODY_NAME = "employingBodyName";
  private static final String DATA_TRAINING_BODY_ID = "trainingBodyId";
  private static final String DATA_TRAINING_BODY_NAME = "trainingBodyName";
  private static final String DATA_TRUST_NAME = "trustKnownAs";
  private static final String PLACEMENT_DATA_SITE_ID = "siteId";
  private static final String PLACEMENT_DATA_SITE_NAME = "site";
  private static final String PLACEMENT_DATA_SITE_LOCATION = "siteLocation";
  private static final String DATA_SITE_ID = "id";
  private static final String DATA_SITE_NAME = "siteName";
  private static final String DATA_SITE_LOCATION = "address";
  private static final String DATA_SPECIALTY_ID = "id";
  private static final String DATA_SPECIALTY_NAME = "name";
  private static final String PLACEMENT_DATA_SPECIALTY_NAME = "specialty";

  @InjectMocks
  private PlacementEnricherFacade enricher;

  @Mock
  private PlacementSyncService placementService;

  @Mock
  private PostSyncService postService;

  @Mock
  private TrustSyncService trustService;

  @Mock
  private SiteSyncService siteService;

  @Mock
  private SpecialtySyncService specialtyService;

  @Mock
  private PlacementSpecialtySyncService placementSpecialtyService;

  @Mock
  private TcsSyncService tcsSyncService;

  @InjectMocks
  @Spy
  private PlacementEnricherFacade enricherSpy;

  @Test
  void shouldEnrichFromPlacementWhenPostAndSameTrustsExist() {
    Placement placement = new Placement();
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
  void shouldEnrichFromPlacementWhenPostAndDifferentTrustsExist() {
    Placement placement = new Placement();
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
  void shouldEnrichFromPlacementWhenPostExistsAndNoEmployingBody() {
    Placement placement = new Placement();
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
  void shouldEnrichFromPlacementWhenPostExistsAndNoTrainingBody() {
    Placement placement = new Placement();
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
  void shouldEnrichFromPlacementWhenPostExistsAndNoEmployingAndTrainingBody() {
    Placement placement = new Placement();
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
  void shouldNotEnrichFromPlacementWhenPostAndOnlyEmployingBodyTrustExists() {
    Placement placement = new Placement();
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
  void shouldNotEnrichFromPlacementWhenPostAndOnlyTrainingBodyTrustExists() {
    Placement placement = new Placement();
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
  void shouldNotEnrichFromPlacementWhenPostExistsAndTrustsNotExist() {
    Placement placement = new Placement();
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
  void shouldNotEnrichFromPlacementWhenPostNotExists() {
    Placement placement = new Placement();
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
  void shouldNotEnrichFromPlacementWhenSiteNotExists() {
    Placement placement = new Placement();
    placement.setData(new HashMap<>(Map.of(PLACEMENT_DATA_SITE_ID, SITE_1_ID)));

    Site site = new Site();
    site.setTisId(SITE_1_ID);
    site.setData(Map.of(
        DATA_SITE_ID, SITE_1_ID,
        DATA_SITE_NAME, SITE_1_NAME,
        DATA_SITE_LOCATION, SITE_1_LOCATION
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
  }

  @Test
  void shouldStillEnrichFromPlacementWhenSiteExistsButHasNoSiteLocation() {
    Placement placement = new Placement();
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
  void shouldEnrichFromPlacementWhenSiteExists() {
    Placement placement = new Placement();
    placement.setData(new HashMap<>(Map.of(PLACEMENT_DATA_SITE_ID, SITE_1_ID)));

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
    assertThat("Unexpected site name.", placementData.get(PLACEMENT_DATA_SITE_NAME),
        is(SITE_1_NAME));
    assertThat("Unexpected site location.", placementData.get(PLACEMENT_DATA_SITE_LOCATION),
        is(SITE_1_LOCATION));
  }

  @Test
  void shouldNotEnrichFromPlacementWhenSiteExistsWithNoSiteNameNorSiteLocation() {
    Placement placement = new Placement();
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
  }

  @Test
  void shouldEnrichFromPlacementWhenPlacementSpecialtyNotExists() {
    Placement placement = new Placement();
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
  void shouldEnrichFromSpecialtyWhenSpecialtyExists() {
    Specialty specialty = new Specialty();
    specialty.setTisId(SPECIALTY_1_ID);
    specialty.setData(Map.of(
        DATA_SPECIALTY_ID, SPECIALTY_1_ID,
        DATA_SPECIALTY_NAME, SPECIALTY_1_NAME
    ));

    Placement placement1 = new Placement();
    placement1.setTisId(PLACEMENT_1_ID);

    Placement placement2 = new Placement();
    placement2.setTisId(PLACEMENT_2_ID);

    PlacementSpecialty placementSpecialty1 = new PlacementSpecialty();
    placementSpecialty1.setData(Map.of(
        DATA_PLACEMENT_SPECIALTY_PLACEMENT_ID, PLACEMENT_1_ID,
        DATA_PLACEMENT_SPECIALTY_SPECIALTY_ID, SPECIALTY_1_ID
    ));

    PlacementSpecialty placementSpecialty2 = new PlacementSpecialty();
    placementSpecialty2.setData(Map.of(
        DATA_PLACEMENT_SPECIALTY_PLACEMENT_ID, PLACEMENT_2_ID,
        DATA_PLACEMENT_SPECIALTY_SPECIALTY_ID, SPECIALTY_1_ID
    ));

    when(placementSpecialtyService.findPrimaryPlacementSpecialtiesBySpecialtyId(SPECIALTY_1_ID))
        .thenReturn(Sets.newSet(placementSpecialty1, placementSpecialty2));
    when(placementService.findById(PLACEMENT_1_ID)).thenReturn(Optional.of(placement1));
    when(placementService.findById(PLACEMENT_2_ID)).thenReturn(Optional.of(placement2));

    enricher.enrich(specialty);

    verify(placementService, never()).request(anyString());
    verify(specialtyService, never()).request(anyString());

    verify(tcsSyncService).syncRecord(placement1);
    verify(tcsSyncService).syncRecord(placement2);
    verifyNoMoreInteractions(tcsSyncService);

    Map<String, String> placement1Data = placement1.getData();
    assertThat("Unexpected specialty name.", placement1Data.get(PLACEMENT_DATA_SPECIALTY_NAME),
        is(SPECIALTY_1_NAME));

    Map<String, String> placement2Data = placement2.getData();
    assertThat("Unexpected specialty name.", placement2Data.get(PLACEMENT_DATA_SPECIALTY_NAME),
        is(SPECIALTY_1_NAME));
  }

  @Test
  void shouldEnrichFromPlacementWhenPostAndSiteAndPlacementSpecialtyExist() {
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
  void shouldNotEnrichFromPlacementWhenPostAndSiteAndPlacementSpecialtyExistButSpecialityDoesNot() {

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
  void shouldNotEnrichFromSpecialtyWhenSpecialtyExistsWithoutName() {
    Specialty specialty = new Specialty();
    specialty.setTisId(SPECIALTY_1_ID);
    specialty.setData(Map.of(
        DATA_SPECIALTY_ID, SPECIALTY_1_ID
    ));

    Placement placement1 = new Placement();
    placement1.setTisId(PLACEMENT_1_ID);

    PlacementSpecialty placementSpecialty1 = new PlacementSpecialty();
    placementSpecialty1.setData(Map.of(
        DATA_PLACEMENT_SPECIALTY_PLACEMENT_ID, PLACEMENT_1_ID,
        DATA_PLACEMENT_SPECIALTY_SPECIALTY_ID, SPECIALTY_1_ID
    ));

    enricher.enrich(specialty);

    verify(placementService, never()).request(anyString());
    verify(specialtyService, never()).request(anyString());

    verifyNoInteractions(tcsSyncService);

    Map<String, String> placement1Data = placement1.getData();
    assertThat("Unexpected specialty name.", placement1Data.get(PLACEMENT_DATA_SPECIALTY_NAME),
        nullValue());
  }

  @Test
  void shouldRestartEnrichmentFromPlacementIfPlacementSpecialtyDeletedIncorrectly() {
    // PlacementSpecialty got deleted and Placement exists without a new PlacementSpecialty
    Placement placement = new Placement();
    placement.setTisId(PLACEMENT_1_ID);

    when(placementService.findById(PLACEMENT_1_ID)).thenReturn(Optional.of(placement));
    when(placementSpecialtyService.findById(PLACEMENT_1_ID)).thenReturn(Optional.empty());

    enricherSpy.restartPlacementEnrichmentIfDeletionIncorrect(PLACEMENT_1_ID);

    verify(enricherSpy).enrich(placement);
    verify(placementSpecialtyService).request(PLACEMENT_1_ID);
  }

  @Test
  void shouldDoNothingIfPlacementSpecialtyDeletedBecausePlacementGotDeleted() {
    // PlacementSpecialty deleted correctly.
    // PlacementSpecialty got deleted because its Placement got deleted also.
    when(placementService.findById(PLACEMENT_1_ID)).thenReturn(Optional.empty());

    enricherSpy.restartPlacementEnrichmentIfDeletionIncorrect(PLACEMENT_1_ID);

    verify(enricherSpy, never()).enrich(any(Placement.class));
    verifyNoInteractions(placementSpecialtyService);
  }

  @Test
  void shouldDoNothingIfPlacementSpecialtyDeletedBecausePlacementHasNewPlacementSpecialty() {
    // PlacementSpecialty deleted correctly.
    // PlacementSpecialty got deleted because its Placement has a new PlacementSpecialty.
    Placement placement = new Placement();
    placement.setTisId(PLACEMENT_1_ID);
    when(placementService.findById(PLACEMENT_1_ID)).thenReturn(Optional.of(placement));

    PlacementSpecialty newPlacementSpecialty = new PlacementSpecialty();
    newPlacementSpecialty.setData(Map.of(
        DATA_PLACEMENT_SPECIALTY_PLACEMENT_ID, PLACEMENT_1_ID,
        DATA_PLACEMENT_SPECIALTY_SPECIALTY_ID, SPECIALTY_2_ID
    ));
    when(placementSpecialtyService.findById(PLACEMENT_1_ID))
        .thenReturn(Optional.of(newPlacementSpecialty));

    enricherSpy.restartPlacementEnrichmentIfDeletionIncorrect(PLACEMENT_1_ID);

    verify(placementSpecialtyService, times(1)).findById(PLACEMENT_1_ID);
    verifyNoMoreInteractions(placementSpecialtyService);
    verify(enricherSpy, never()).enrich(any(Placement.class));
  }
}

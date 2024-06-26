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

import static uk.nhs.hee.tis.trainee.sync.model.Operation.DELETE;
import static uk.nhs.hee.tis.trainee.sync.model.Operation.LOAD;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.trainee.sync.model.Grade;
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
import uk.nhs.hee.tis.trainee.sync.service.PostSpecialtySyncService;
import uk.nhs.hee.tis.trainee.sync.service.PostSyncService;
import uk.nhs.hee.tis.trainee.sync.service.SiteSyncService;
import uk.nhs.hee.tis.trainee.sync.service.SpecialtySyncService;
import uk.nhs.hee.tis.trainee.sync.service.TcsSyncService;
import uk.nhs.hee.tis.trainee.sync.service.TrustSyncService;

@Slf4j
@Component
public class PlacementEnricherFacade {

  private static final String PLACEMENT_POST_ID = "postId";
  private static final String POST_TRUST_EMPLOYING_BODY_ID = "employingBodyId";
  private static final String POST_TRUST_TRAINING_BODY_ID = "trainingBodyId";
  private static final String POST_OWNER = "owner";
  private static final String TRUST_NAME = "trustKnownAs";
  private static final String PLACEMENT_DATA_EMPLOYING_BODY_NAME = "employingBodyName";
  private static final String PLACEMENT_DATA_TRAINING_BODY_NAME = "trainingBodyName";
  private static final String PLACEMENT_DATA_ALLOWED_SUBSPECIALTY = "postAllowsSubspecialty";
  private static final String PLACEMENT_SITE_ID = "siteId";
  private static final String PLACEMENT_DATA_SITE_NAME = "site";
  private static final String PLACEMENT_GRADE_ID = "gradeId";
  private static final String PLACEMENT_DATA_GRADE_ABBREVIATION = "gradeAbbreviation";
  private static final String PLACEMENT_DATA_SITE_LOCATION = "siteLocation";
  private static final String PLACEMENT_DATA_SITE_KNOWN_AS = "siteKnownAs";
  private static final String PLACEMENT_DATA_OTHER_SITES = "otherSites";
  private static final String PLACEMENT_DATA_SPECIALTY_NAME = "specialty";
  private static final String PLACEMENT_DATA_SUB_SPECIALTY_NAME = "subSpecialty";
  private static final String PLACEMENT_DATA_OTHER_SPECIALTIES_NAME = "otherSpecialties";
  private static final String PLACEMENT_DATA_OTHER_SPECIALTIES_SPECIALTY_NAME = "name";
  private static final String PLACEMENT_DATA_OTHER_SPECIALTIES_ID_NAME = "specialtyId";
  private static final String PLACEMENT_OWNER = "owner";
  private static final String SITE_NAME = "siteName";
  private static final String SITE_LOCATION = "address";
  private static final String SITE_KNOWN_AS = "siteKnownAs";
  private static final String GRADE_ABBREVIATION = "abbreviation";
  private static final String SPECIALTY_ID = "id";
  private static final String SPECIALTY_NAME = "name";
  private static final String PLACEMENT_SPECIALTY_TYPE_PRIMARY = "PRIMARY";
  private static final String PLACEMENT_SPECIALTY_TYPE_SUB_SPECIALTY = "SUB_SPECIALTY";
  private static final String PLACEMENT_SPECIALTY_TYPE_OTHER = "OTHER";
  private static final String PLACEMENT_SPECIALTY_SPECIALTY_ID_NAME = "specialtyId";

  private final PostSyncService postService;
  private final PostSpecialtySyncService postSpecialtyService;
  private final TrustSyncService trustService;
  private final GradeSyncService gradeService;
  private final SiteSyncService siteService;
  private final SpecialtySyncService specialtyService;
  private final PlacementSpecialtySyncService placementSpecialtyService;
  private final PlacementSiteSyncService placementSiteService;

  private final TcsSyncService tcsSyncService;

  private final ObjectMapper objectMapper;

  PlacementEnricherFacade(PostSyncService postService,
      PostSpecialtySyncService postSpecialtyService,
      TrustSyncService trustService, SiteSyncService siteService,
      GradeSyncService gradeService,
      SpecialtySyncService specialtyService,
      PlacementSpecialtySyncService placementSpecialtyService, TcsSyncService tcsSyncService,
      PlacementSiteSyncService placementSiteService, ObjectMapper objectMapper) {
    this.postService = postService;
    this.postSpecialtyService = postSpecialtyService;
    this.trustService = trustService;
    this.gradeService = gradeService;
    this.siteService = siteService;
    this.specialtyService = specialtyService;
    this.tcsSyncService = tcsSyncService;
    this.placementSpecialtyService = placementSpecialtyService;
    this.placementSiteService = placementSiteService;
    this.objectMapper = objectMapper;
  }

  /**
   * Delete a placement from tis-trainee-details.
   *
   * @param placement The placement to delete.
   */
  public void delete(Placement placement) {
    placement.setOperation(DELETE);
    placement.setSchema("tcs");
    placement.setTable(Placement.ENTITY_NAME);
    tcsSyncService.syncRecord(placement);
  }

  /**
   * Sync an enriched placement with the placement as the starting object.
   *
   * @param placement The placement to enrich.
   */
  public void enrich(Placement placement) {
    boolean doSync;

    doSync = enrichPlacementWithRelatedPost(placement);
    doSync &= enrichPlacementWithRelatedSite(placement);
    doSync &= enrichPlacementWithRelatedOtherSites(placement);
    doSync &= enrichPlacementWithRelatedGrade(placement);
    doSync &= enrichPlacementWithRelatedSpecialty(placement);
    doSync &= enrichPlacementWithRelatedOtherSpecialties(placement);

    if (doSync) {
      syncPlacement(placement);
    }
  }

  /**
   * Enrich the placement with details from the Post.
   *
   * @param placement The placement to enrich.
   * @param post      The post to enrich the placement with.
   * @return Whether enrichment was successful.
   */
  private boolean enrich(Placement placement, Post post) {
    String employingBodyId = getEmployingBodyId(post);
    Optional<String> employingBodyName = getTrustName(employingBodyId);

    String trainingBodyId = getTrainingBodyId(post);
    Optional<String> trainingBodyName = getTrustName(trainingBodyId);

    String owner = getOwner(post);

    if (employingBodyName.isPresent() && trainingBodyName.isPresent()) {
      Boolean postAllowsSubspecialty = ! getPostSubspecialties(post).isEmpty();
      populatePostDetails(placement, employingBodyName.get(), trainingBodyName.get(), owner,
          postAllowsSubspecialty);
      return true;
    } else {
      return false;
    }
  }

  /**
   * Enrich the placement with details from the Site.
   *
   * @param data The data to enrich with site data.
   * @param site The site to enrich the placement with.
   * @return Whether enrichment was successful.
   */
  private boolean enrich(Map<String, String> data, Site site) {

    String siteName = getSiteName(site);
    String siteLocation = getSiteLocation(site);
    String siteKnownAs = getSiteKnownAs(site);

    if (siteName != null || siteLocation != null) {
      // a few sites have no location,
      // and one (id = 14150, siteCode = C86011) has neither name nor location
      populateSiteDetails(data, siteName, siteLocation, siteKnownAs);
      return true;
    }

    return false;
  }

  /**
   * Enrich the placement with details from the Specialty.
   *
   * @param data      The data to enrich with specialty data.
   * @param specialty The specialty to enrich the placement with.
   * @return Whether enrichment was successful.
   */
  private boolean enrich(Map<String, String> data, Specialty specialty) {

    String specialtyName = getSpecialtyName(specialty);
    String specialtyId = getSpecialtyId(specialty);

    if (specialtyName != null) {
      populateSpecialtyDetails(data, specialtyName, specialtyId);
      return true;
    }

    return false;
  }

  /**
   * Enrich the placement with details from the Grade.
   *
   * @param placement The placement to enrich.
   * @param grade     The grade to enrich the placement with.
   * @return Whether enrichment was successful.
   */
  private boolean enrich(Placement placement, Grade grade) {

    String gradeAbbr = getGradeAbbr(grade);

    if (gradeAbbr != null) {
      populateGradeDetails(placement, gradeAbbr);
      return true;
    }

    return false;
  }

  /**
   * Enrich the placement with details from the Specialty.
   *
   * @param placement The placement to enrich.
   * @param specialty The specialty to enrich the placement with.
   * @param placementSpecialtyType The placement specialty type ("PRIMARY" or "SUB_SPECIALTY")
   * @return Whether enrichment was successful.
   */
  private boolean enrich(Placement placement, Specialty specialty, String placementSpecialtyType) {
    String specialtyName = getSpecialtyName(specialty);

    if (specialtyName != null) {
      if (placementSpecialtyType.equals(PLACEMENT_SPECIALTY_TYPE_PRIMARY)) {
        populateSpecialtyDetails(placement, specialtyName);
        return true;
      } else if (placementSpecialtyType.equals(PLACEMENT_SPECIALTY_TYPE_SUB_SPECIALTY)) {
        populateSubSpecialtyDetails(placement, specialtyName);
        return true;
      }
    }

    return false;
  }

  /**
   * Enrich the placement with details from its related Post.
   *
   * @param placement The placement to enrich.
   * @return Whether enrichment was successful.
   */
  private boolean enrichPlacementWithRelatedPost(Placement placement) {
    boolean isEnriched = true;
    String postId = getPostId(placement);

    if (postId != null) {
      Optional<Post> optionalPost = postService.findById(postId);

      if (optionalPost.isPresent()) {
        isEnriched = enrich(placement, optionalPost.get());
      } else {
        postService.request(postId);
        isEnriched = false;
      }
    }
    return isEnriched;
  }

  private boolean enrichPlacementWithRelatedSite(Placement placement) {
    boolean isEnriched = true;
    String siteId = getSiteId(placement);

    if (siteId != null) {
      Optional<Site> optionalSite = siteService.findById(siteId);

      if (optionalSite.isPresent()) {
        isEnriched = enrich(placement.getData(), optionalSite.get());
      } else {
        siteService.request(siteId);
        isEnriched = false;
      }
    }
    return isEnriched;
  }

  /**
   * Add other sites data to the placement.
   *
   * @param placement The placement to enrich.
   * @return Whether the placement was enriched, also returns true if no enrichment needed.
   */
  private boolean enrichPlacementWithRelatedOtherSites(Placement placement) {
    boolean isEnriched = true;
    String placementId = placement.getTisId();

    Set<Map<String, String>> otherSitesData = new HashSet<>();

    Set<String> otherSiteIds = placementSiteService.findOtherSitesByPlacementId(
            Long.parseLong(placementId)).stream()
        .map(PlacementSite::getSiteId)
        .filter(Objects::nonNull)
        .map(Object::toString)
        .collect(Collectors.toSet());

    for (String otherSiteId : otherSiteIds) {
      Optional<Site> otherSite = siteService.findById(otherSiteId);

      if (otherSite.isPresent()) {
        Map<String, String> otherSiteData = new HashMap<>();
        otherSitesData.add(otherSiteData);
        isEnriched &= enrich(otherSiteData, otherSite.get());
      } else {
        siteService.request(otherSiteId);
        isEnriched = false;
      }
    }

    if (isEnriched) {
      try {
        String string = objectMapper.writeValueAsString(otherSitesData);
        Map<String, String> placementData = placement.getData();
        placementData.put(PLACEMENT_DATA_OTHER_SITES, string);
      } catch (JsonProcessingException e) {
        throw new RuntimeException("Unable to process other sites data.", e);
      }
    }

    return isEnriched;
  }

  /**
   * Add other specialties data to the placement.
   *
   * @param placement The placement to enrich.
   * @return Whether the placement was enriched, also returns true if no enrichment needed.
   */
  private boolean enrichPlacementWithRelatedOtherSpecialties(Placement placement) {
    boolean isEnriched = true;
    String placementId = placement.getTisId();

    Set<Map<String, String>> otherSpecialtiesData = new HashSet<>();

    Set<String> otherSpecialtiesIds
        = placementSpecialtyService.findAllPlacementSpecialtyByPlacementIdAndSpecialtyType(
            placementId, PLACEMENT_SPECIALTY_TYPE_OTHER).stream()
        .map(sp -> sp.getData().get(PLACEMENT_SPECIALTY_SPECIALTY_ID_NAME))
        .filter(Objects::nonNull)
        .map(Object::toString)
        .collect(Collectors.toSet());

    for (String otherSpecialtyId : otherSpecialtiesIds) {
      Optional<Specialty> otherSpecialty = specialtyService.findById(otherSpecialtyId);

      if (otherSpecialty.isPresent()) {
        Map<String, String> otherSpecialtyData = new HashMap<>();
        otherSpecialtiesData.add(otherSpecialtyData);
        isEnriched &= enrich(otherSpecialtyData, otherSpecialty.get());
      } else {
        specialtyService.request(otherSpecialtyId);
        isEnriched = false;
      }
    }

    if (isEnriched) {
      try {
        String string = objectMapper.writeValueAsString(otherSpecialtiesData);
        Map<String, String> placementData = placement.getData();
        placementData.put(PLACEMENT_DATA_OTHER_SPECIALTIES_NAME, string);
      } catch (JsonProcessingException e) {
        throw new RuntimeException("Unable to process other specialties data.", e);
      }
    }

    return isEnriched;
  }

  private boolean enrichPlacementWithRelatedGrade(Placement placement) {
    boolean isEnriched = true;
    String gradeId = getGradeId(placement);

    if (gradeId != null) {
      Optional<Grade> optionalGrade = gradeService.findById(gradeId);

      if (optionalGrade.isPresent()) {
        isEnriched = enrich(placement, optionalGrade.get());
      } else {
        gradeService.request(gradeId);
        isEnriched = false;
      }
    }
    return isEnriched;
  }

  private boolean enrichPlacementWithRelatedSpecialty(Placement placement) {
    boolean isEnriched = true;
    String placementId = getPlacementId(placement);

    // fetch related Primary Specialty
    Optional<PlacementSpecialty> optionalPrimaryPlacementSpecialty = placementSpecialtyService
        .findSinglePlacementSpecialtyByPlacementIdAndSpecialtyType(
            placementId, PLACEMENT_SPECIALTY_TYPE_PRIMARY);
    if (optionalPrimaryPlacementSpecialty.isPresent()) {
      Optional<Specialty> optionalPrimarySpecialty =
          getSpecialty(getSpecialtyId(optionalPrimaryPlacementSpecialty.get()));

      isEnriched = optionalPrimarySpecialty
          .filter(specialty ->
              enrich(placement, specialty, PLACEMENT_SPECIALTY_TYPE_PRIMARY)).isPresent();
    } else {
      placementSpecialtyService.request(placementId);
      // isEnriched is not affected by a missing placement specialty
    }

    // fetch related Sub Specialty (sub specialty is not mandatory)
    Optional<PlacementSpecialty> optionalSubPlacementSpecialty = placementSpecialtyService
        .findSinglePlacementSpecialtyByPlacementIdAndSpecialtyType(
            placementId, PLACEMENT_SPECIALTY_TYPE_SUB_SPECIALTY);
    if (optionalSubPlacementSpecialty.isPresent()) {
      Optional<Specialty> optionalSubSpecialty =
          getSpecialty(getSpecialtyId(optionalSubPlacementSpecialty.get()));

      isEnriched = optionalSubSpecialty
          .filter(specialty ->
              enrich(placement, specialty, PLACEMENT_SPECIALTY_TYPE_SUB_SPECIALTY)).isPresent();
    }

    return isEnriched;
  }

  /**
   * Enrich the placement with the given employing body and training body names and then sync it.
   *
   * @param placement         The placement to sync.
   * @param employingBodyName The employing body name to enrich with.
   * @param trainingBodyName  The training body name to enrich with.
   */
  private void populatePostDetails(Placement placement, String employingBodyName,
      String trainingBodyName, String owner, Boolean postAllowsSubspecialty) {
    // Add extra data to placement data.
    if (Strings.isNotBlank(employingBodyName)) {
      placement.getData().put(PLACEMENT_DATA_EMPLOYING_BODY_NAME, employingBodyName);
    }

    if (Strings.isNotBlank(trainingBodyName)) {
      placement.getData().put(PLACEMENT_DATA_TRAINING_BODY_NAME, trainingBodyName);
    }

    if (Strings.isNotBlank(owner)) {
      placement.getData().put(PLACEMENT_OWNER, owner);
    }

    placement.getData().put(PLACEMENT_DATA_ALLOWED_SUBSPECIALTY,
        String.valueOf(postAllowsSubspecialty));
  }

  /**
   * Enrich the placement with the given site name and location and then sync it.
   *
   * @param data         The data object to add site details to.
   * @param siteName     The site name to enrich with.
   * @param siteLocation The site location to enrich with.
   * @param siteKnownAs  The site known as name to enrich with.
   */

  private void populateSiteDetails(Map<String, String> data, String siteName, String siteLocation,
      String siteKnownAs) {
    if (Strings.isNotBlank(siteName)) {
      data.put(PLACEMENT_DATA_SITE_NAME, siteName);
    }
    if (Strings.isNotBlank(siteLocation)) {
      data.put(PLACEMENT_DATA_SITE_LOCATION, siteLocation);
    }
    if (Strings.isNotBlank(siteKnownAs)) {
      data.put(PLACEMENT_DATA_SITE_KNOWN_AS, siteKnownAs);
    }
  }

  /**
   * Enrich the placement with the given specialty name and id and then sync it.
   *
   * @param data          The data object to add specialty details to.
   * @param specialtyName The specialty name to enrich with.
   * @param specialtyId   The specialty id to enrich with.
   */

  private void populateSpecialtyDetails(Map<String, String> data, String specialtyName,
      String specialtyId) {
    if (Strings.isNotBlank(specialtyName)) {
      data.put(PLACEMENT_DATA_OTHER_SPECIALTIES_SPECIALTY_NAME, specialtyName);
    }
    if (Strings.isNotBlank(specialtyId)) {
      data.put(PLACEMENT_DATA_OTHER_SPECIALTIES_ID_NAME, specialtyId);
    }
  }

  /**
   * Enrich the placement with the given grade name then sync it.
   *
   * @param placement The placement to sync.
   * @param gradeAbbr The grade name to enrich with.
   */

  private void populateGradeDetails(Placement placement, String gradeAbbr) {
    // Add extra data to placement data.
    Map<String, String> placementData = placement.getData();
    if (Strings.isNotBlank(gradeAbbr)) {
      placementData.put(PLACEMENT_DATA_GRADE_ABBREVIATION, gradeAbbr);
    }
  }

  private void populateSpecialtyDetails(Placement placement, String specialtyName) {
    // Add extra data to placement data.
    Map<String, String> placementData = placement.getData();
    if (Strings.isNotBlank(specialtyName)) {
      placementData.put(PLACEMENT_DATA_SPECIALTY_NAME, specialtyName);
    }
  }

  private void populateSubSpecialtyDetails(Placement placement, String subSpecialtyName) {
    // Add extra data to placement data.
    Map<String, String> placementData = placement.getData();
    if (Strings.isNotBlank(subSpecialtyName)) {
      placementData.put(PLACEMENT_DATA_SUB_SPECIALTY_NAME, subSpecialtyName);
    }
  }

  /**
   * Sync the (completely enriched) placement.
   *
   * @param placement The placement to sync.
   */
  private void syncPlacement(Placement placement) {
    // Set the required metadata so the record can be synced using common logic.
    placement.setOperation(LOAD);
    placement.setSchema("tcs");
    placement.setTable("Placement");

    tcsSyncService.syncRecord(placement);
  }

  /**
   * Get the trust name for the trust with the given id, if the trust is not found it will be
   * requested.
   *
   * @param trustId The id of the trust to get the name of.
   * @return The trust's name, or an empty string if the ID is null.
   */
  private Optional<String> getTrustName(@Nullable String trustId) {
    if (trustId == null) {
      return Optional.of("");
    }

    String trustName = null;
    Optional<Trust> optionalTrust = trustService.findById(trustId);

    if (optionalTrust.isPresent()) {
      Trust trust = optionalTrust.get();
      trustName = getTrustName(trust);
    } else {
      trustService.request(trustId);
    }

    return Optional.ofNullable(trustName);
  }

  /**
   * Get the trust name from the trust.
   *
   * @param trust The trust to get the name of.
   * @return The trust's name.
   */
  private String getTrustName(Trust trust) {
    return trust.getData().get(TRUST_NAME);
  }

  /**
   * Get the specialty for the given id, if the specialty is not found it will be requested.
   *
   * @param specialtyId The id of the specialty to get.
   * @return The specialty, or Optional.empty() if the ID is null or the specialty is not found.
   */
  private Optional<Specialty> getSpecialty(@Nullable String specialtyId) {
    if (specialtyId == null) {
      return Optional.empty();
    }

    Optional<Specialty> optionalSpecialty = specialtyService.findById(specialtyId);

    if (optionalSpecialty.isEmpty()) {
      specialtyService.request(specialtyId);
    }

    return optionalSpecialty;
  }

  /**
   * Get the ID for the employing body trust of the post.
   *
   * @param post The post to get the employing body id from.
   * @return The employing body id.
   */
  private String getEmployingBodyId(Post post) {
    return post.getData().get(POST_TRUST_EMPLOYING_BODY_ID);
  }

  /**
   * Get the ID for the training body trust of the post.
   *
   * @param post The post to get the training body id from.
   * @return The training body id.
   */
  private String getTrainingBodyId(Post post) {
    return post.getData().get(POST_TRUST_TRAINING_BODY_ID);
  }

  /**
   * Get any sub specialties for the post.
   *
   * @param post The post to get the sub specialties for.
   * @return The post sub specialties.
   */
  private Set<PostSpecialty> getPostSubspecialties(Post post) {
    String postId = post.getTisId();

    return postSpecialtyService.findByPostId(postId);
  }

  /**
   * Get the owner of the post.
   *
   * @param post The post to get the owner from.
   * @return The owner.
   */
  private String getOwner(Post post) {
    return post.getData().get(POST_OWNER);
  }

  /**
   * Get the Post ID from the placement.
   *
   * @param placement The placement to get the post id from.
   * @return The post id.
   */
  private String getPostId(Placement placement) {
    return placement.getData().get(PLACEMENT_POST_ID);
  }

  /**
   * Get the Site ID from the placement.
   *
   * @param placement The placement to get the site id from.
   * @return The site id.
   */
  private String getSiteId(Placement placement) {
    return placement.getData().get(PLACEMENT_SITE_ID);
  }

  /**
   * Get the Grade ID from the placement.
   *
   * @param placement The placement to get the grade id from.
   * @return The grade id.
   */
  private String getGradeId(Placement placement) {
    return placement.getData().get(PLACEMENT_GRADE_ID);
  }

  /**
   * Get the site name from the site.
   *
   * @param site The site to get the name of.
   * @return The site's name.
   */
  private String getSiteName(Site site) {
    return site.getData().get(SITE_NAME);
  }

  /**
   * Get the grade abbreviation from the grade.
   *
   * @param grade The grade to get the name of.
   * @return The grade's abbreviation.
   */
  private String getGradeAbbr(Grade grade) {
    return grade.getData().get(GRADE_ABBREVIATION);
  }

  /**
   * Get the site location from the site.
   *
   * @param site The site to get the location of.
   * @return The site's location.
   */
  private String getSiteLocation(Site site) {
    return site.getData().get(SITE_LOCATION);
  }

  /**
   * Get the id of a placement. Note: since only one primary specialty exists per placement, we
   * consider the placementId of a placementSpecialty as its primary key. PlacementSpecialties don't
   * have an id, so we generally use placementSpecialty.placementId to uniquely identify them.
   *
   * @param placement The placement to get the id from.
   * @return The placement id.
   */
  private String getPlacementId(Placement placement) {
    return placement.getTisId();
  }

  /**
   * Get the Specialty ID from the placement specialty.
   *
   * @param placementSpecialty The placement specialty to get the specialty id from.
   * @return The specialty id.
   */
  private String getSpecialtyId(PlacementSpecialty placementSpecialty) {
    return placementSpecialty.getData().get(PLACEMENT_SPECIALTY_SPECIALTY_ID_NAME);
  }

  /**
   * Get the Specialty ID from the specialty.
   *
   * @param specialty The specialty to get the specialty id from.
   * @return The specialty id.
   */
  private String getSpecialtyId(Specialty specialty) {
    return specialty.getData().get(SPECIALTY_ID);
  }

  /**
   * Get the specialty name from the specialty.
   *
   * @param specialty The specialty to get the name of.
   * @return The specialty's name.
   */
  private String getSpecialtyName(Specialty specialty) {
    return specialty.getData().get(SPECIALTY_NAME);
  }

  /**
   * Get the site known as from the site.
   *
   * @param site The site to get the name of.
   * @return The site's know as name.
   */
  private String getSiteKnownAs(Site site) {
    return site.getData().get(SITE_KNOWN_AS);
  }
}

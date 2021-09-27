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

import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
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

@Slf4j
@Component
public class PlacementEnricherFacade {

  private static final String PLACEMENT_POST_ID = "postId";
  private static final String POST_TRUST_EMPLOYING_BODY_ID = "employingBodyId";
  private static final String POST_TRUST_TRAINING_BODY_ID = "trainingBodyId";
  private static final String TRUST_NAME = "trustKnownAs";
  private static final String PLACEMENT_DATA_EMPLOYING_BODY_NAME = "employingBodyName";
  private static final String PLACEMENT_DATA_TRAINING_BODY_NAME = "trainingBodyName";
  private static final String PLACEMENT_SITE_ID = "siteId";
  private static final String PLACEMENT_DATA_SITE_NAME = "site";
  private static final String PLACEMENT_DATA_SITE_LOCATION = "siteLocation";
  private static final String PLACEMENT_DATA_SPECIALTY_NAME = "specialty";
  private static final String PLACEMENT_SPECIALTY_SPECIALITY_ID = "specialtyId";
  private static final String SITE_NAME = "siteName";
  private static final String SITE_LOCATION = "address";
  private static final String SPECIALTY_NAME = "name";

  private final PlacementSyncService placementService;
  private final PostSyncService postService;
  private final TrustSyncService trustService;
  private final SiteSyncService siteService;
  private final SpecialtySyncService specialtyService;
  private final PlacementSpecialtySyncService placementSpecialtyService;

  private final TcsSyncService tcsSyncService;

  PlacementEnricherFacade(PlacementSyncService placementService, PostSyncService postService,
      TrustSyncService trustService, SiteSyncService siteService,
      SpecialtySyncService specialtyService,
      PlacementSpecialtySyncService placementSpecialtyService, TcsSyncService tcsSyncService) {
    this.placementService = placementService;
    this.postService = postService;
    this.trustService = trustService;
    this.siteService = siteService;
    this.specialtyService = specialtyService;
    this.tcsSyncService = tcsSyncService;
    this.placementSpecialtyService = placementSpecialtyService;
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
    enrich(placement, true, true, true);
  }

  /**
   * Sync a Placement. Optionally enrich with Post, Site and/or Specialty details.
   *
   * @param placement         The Placement to enrich.
   * @param doPostEnrich      Enrich with post details.
   * @param doSiteEnrich      Enrich with site details.
   * @param doSpecialtyEnrich Enrich with specialty details.
   */
  private void enrich(Placement placement, boolean doPostEnrich, boolean doSiteEnrich,
      boolean doSpecialtyEnrich) {
    boolean doSync = true;

    if (doPostEnrich) {
      doSync = enrichPlacementWithRelatedPost(placement);
    }

    if (doSiteEnrich) {
      doSync &= enrichPlacementWithRelatedSite(placement);
    }

    if (doSpecialtyEnrich) {
      doSync &= enrichPlacementWithRelatedSpecialty(placement);
    }

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

    if (employingBodyName.isPresent() && trainingBodyName.isPresent()) {
      populatePostDetails(placement, employingBodyName.get(), trainingBodyName.get());
      return true;
    } else {
      return false;
    }
  }

  /**
   * Enrich the placement with details from the Site.
   *
   * @param placement The placement to enrich.
   * @param site      The site to enrich the placement with.
   * @return Whether enrichment was successful.
   */
  private boolean enrich(Placement placement, Site site) {

    String siteName = getSiteName(site);
    String siteLocation = getSiteLocation(site);

    if (siteName != null || siteLocation != null) {
      // a few sites have no location,
      // and one (id = 14150, siteCode = C86011) has neither name nor location
      populateSiteDetails(placement, siteName, siteLocation);
      return true;
    }

    return false;
  }

  /**
   * Enrich the placement with details from the Specialty.
   *
   * @param placement The placement to enrich.
   * @param specialty The specialty to enrich the placement with.
   * @return Whether enrichment was successful.
   */
  private boolean enrich(Placement placement, Specialty specialty) {

    String specialtyName = getSpecialtyName(specialty);

    if (specialtyName != null) {
      populateSpecialtyDetails(placement, specialtyName);
      return true;
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
        isEnriched = enrich(placement, optionalSite.get());
      } else {
        siteService.request(siteId);
        isEnriched = false;
      }
    }
    return isEnriched;
  }

  private boolean enrichPlacementWithRelatedSpecialty(Placement placement) {
    boolean isEnriched = true;
    // placementId in a placementSpecialty is used as the primary key
    String placementId = getPlacementId(placement);

    Optional<PlacementSpecialty> optionalPlacementSpecialty = placementSpecialtyService
        .findById(placementId);

    if (optionalPlacementSpecialty.isPresent()) {
      String specialtyId = getSpecialtyId(optionalPlacementSpecialty.get());
      Optional<Specialty> optionalSpecialty = getSpecialty(specialtyId);

      if (optionalSpecialty.isPresent()) {
        isEnriched = enrich(placement, optionalSpecialty.get());
      } else {
        isEnriched = false;
      }
    } else {
      placementSpecialtyService.request(placementId);
      // isEnriched is not affected by a missing placement specialty
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
      String trainingBodyName) {
    // Add extra data to placement data.
    if (Strings.isNotBlank(employingBodyName)) {
      placement.getData().put(PLACEMENT_DATA_EMPLOYING_BODY_NAME, employingBodyName);
    }

    if (Strings.isNotBlank(trainingBodyName)) {
      placement.getData().put(PLACEMENT_DATA_TRAINING_BODY_NAME, trainingBodyName);
    }
  }

  /**
   * Enrich the placement with the given site name and location and then sync it.
   *
   * @param placement    The placement to sync.
   * @param siteName     The site name to enrich with.
   * @param siteLocation The site location to enrich with.
   */

  private void populateSiteDetails(Placement placement, String siteName,
      String siteLocation) {
    // Add extra data to placement data.
    Map<String, String> placementData = placement.getData();
    if (Strings.isNotBlank(siteName)) {
      placementData.put(PLACEMENT_DATA_SITE_NAME, siteName);
    }

    if (Strings.isNotBlank(siteLocation)) {
      placementData.put(PLACEMENT_DATA_SITE_LOCATION, siteLocation);
    }
  }

  private void populateSpecialtyDetails(Placement placement, String specialtyName) {
    // Add extra data to placement data.
    Map<String, String> placementData = placement.getData();
    if (Strings.isNotBlank(specialtyName)) {
      placementData.put(PLACEMENT_DATA_SPECIALTY_NAME, specialtyName);
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

  private boolean isPlacementSpecialtyExists(String placementId) {
    if (placementId != null) {
      Optional<PlacementSpecialty> placementSpecialty = placementSpecialtyService
          .findById(placementId);
      return placementSpecialty.isPresent();
    }
    return false;
  }

  /**
   * Determine whether the PlacementSpecialty being deleted has been deleted correctly, i.e. the
   * Placement has also been deleted (isEmpty()) or has been superseded by a new PlacementSpecialty.
   * Enrichment of the Placement (prompting a request of the PlacementSpecialty) will be triggered
   * if deletion was incorrect according to the conditions above.
   *
   * @param placementId of the PlacementSpecialty to be deleted.
   */
  public void restartPlacementEnrichmentIfDeletionIncorrect(String placementId) {
    if (placementId != null) {
      Optional<Placement> placement = placementService.findById(placementId);
      boolean placementSpecialtyDeletedCorrectly =
          placement.isEmpty() || isPlacementSpecialtyExists(placementId);
      if (!placementSpecialtyDeletedCorrectly) {
        enrich(placement.get());
        log.warn(
            "PlacementSpecialty with placementId {} got deleted but its placement is still "
                + "existing without an associated placementSpecialty. Enrichment of Placement "
                + "has been restarted.",
            placementId);
      }
    }
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
   * Get the site name from the site.
   *
   * @param site The site to get the name of.
   * @return The site's name.
   */
  private String getSiteName(Site site) {
    return site.getData().get(SITE_NAME);
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
    return placementSpecialty.getData().get(PLACEMENT_SPECIALTY_SPECIALITY_ID);
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
}

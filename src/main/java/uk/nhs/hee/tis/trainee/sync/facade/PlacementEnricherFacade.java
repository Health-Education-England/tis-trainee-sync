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

import static uk.nhs.hee.tis.trainee.sync.model.Operation.LOAD;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.util.Strings;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.trainee.sync.model.Placement;
import uk.nhs.hee.tis.trainee.sync.model.PlacementSpecialty;
import uk.nhs.hee.tis.trainee.sync.model.Post;
import uk.nhs.hee.tis.trainee.sync.model.Record;
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
  private static final String PLACEMENT_SPECIALTY_PLACEMENT_ID = "placementId";
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
   * Sync an enriched placement with the associated trust (employing/training body) as the starting
   * point.
   *
   * @param trust The trust triggering placement enrichment.
   */
  public void enrich(Trust trust) {
    String trustKnownAs = getTrustName(trust);

    String id = trust.getTisId();
    Set<Post> byEmployingBodyId = postService.findByEmployingBodyId(id);
    Set<Post> byTrainingBodyId = postService.findByTrainingBodyId(id);

    // If the same post exists in each collection then they use the same trust name.
    byEmployingBodyId.forEach(
        post -> enrich(post, trustKnownAs, byTrainingBodyId.remove(post) ? trustKnownAs : null));

    // Any remaining posts have employing bodies which do not match the training body.
    byTrainingBodyId.forEach(post -> enrich(post, null, trustKnownAs));
  }

  /**
   * Sync an enriched placement with the associated post as the starting point.
   *
   * @param post The post triggering placement enrichment.
   */
  public void enrich(Post post) {
    enrich(post, null, null);
  }

  /**
   * Enrich placements associated with the Post with the given employing and training bodies, if the
   * bodies are null they will be queried for.
   *
   * @param post              The post to get associated placements from.
   * @param employingBodyName The employing body to enrich with.
   * @param trainingBodyName  The training body to enrich with.
   */
  private void enrich(Post post, @Nullable String employingBodyName,
      @Nullable String trainingBodyName) {
    if (employingBodyName == null) {
      employingBodyName = getTrustName(getEmployingBodyId(post)).orElse(null);
    }

    if (trainingBodyName == null) {
      trainingBodyName = getTrustName(getTrainingBodyId(post)).orElse(null);
    }

    if (employingBodyName != null && trainingBodyName != null) {
      String id = post.getTisId();
      Set<Placement> placements = placementService.findByPostId(id);

      final String finalEmployingBodyName = employingBodyName;
      final String finalTrainingBodyName = trainingBodyName;
      placements.forEach(
          placement -> {
            populatePostDetails(placement, finalEmployingBodyName, finalTrainingBodyName);
            enrich(placement, false, true, true);
          }
      );
    }
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
   * @param placement          The Placement to enrich.
   * @param doPostEnrich       Enrich with post details.
   * @param doSiteEnrich       Enrich with site details.
   * @param doSpecialtyEnrich  Enrich with specialty details.
   */
  private void enrich(Placement placement, boolean doPostEnrich, boolean doSiteEnrich,
      boolean doSpecialtyEnrich) {
    boolean doSync = true;

    if (doPostEnrich) {
      String postId = getPostId(placement);

      if (postId != null) {
        Optional<Post> optionalPost = postService.findById(postId);

        if (optionalPost.isPresent()) {
          doSync = enrich(placement, optionalPost.get());
        } else {
          postService.request(postId);
          doSync = false;
        }
      }
    }

    if (doSiteEnrich) {
      String siteId = getSiteId(placement);

      if (siteId != null) {
        Optional<Site> optionalSite = siteService.findById(siteId);

        if (optionalSite.isPresent()) {
          doSync &= enrich(placement, optionalSite.get());
        } else {
          siteService.request(siteId);
          doSync = false;
        }
      }
    }

    if (doSpecialtyEnrich) {
      String placementSpecialtyId = getPlacementSpecialtyId(placement);

      Optional<PlacementSpecialty> optionalPlacementSpecialty = placementSpecialtyService
          .findById(placementSpecialtyId);

      if (optionalPlacementSpecialty.isPresent()) {
        String specialtyId = getSpecialtyId(optionalPlacementSpecialty.get());
        Optional<Specialty> optionalSpecialty = specialtyService.findById(specialtyId);

        if (optionalSpecialty.isPresent()) {
          doSync &= enrich(placement, optionalSpecialty.get());
        } else {
          specialtyService.request(specialtyId);
          doSync = false;
        }
      } else {
        //TODO: there is no obvious way to know whether the appropriate placementSpecialty
        // will actually exist
        // ~16 180 out of 1.85 million placements do not have any PRIMARY specialty.
        // Do we want to sync regardless?
        placementSpecialtyService.request(placementSpecialtyId);
        // doSync = false;
      }
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
   * Sync an enriched placement with the associated specialty as the starting point.
   *
   * @param specialty The specialty triggering placement enrichment.
   */
  public void enrich(Specialty specialty) {
    enrich(specialty, null);
  }

  /**
   * Sync an enriched placement with the associated placement specialty as the starting point.
   *
   * @param placementSpecialty The placement specialty triggering placement enrichment.
   */
  public void enrich(PlacementSpecialty placementSpecialty) {
    String placementId = getPlacementId(placementSpecialty);
    Optional<Placement> placement = placementService.findById(placementId);
    String specialtyId = getSpecialtyId(placementSpecialty);
    Optional<Specialty> specialty = specialtyService.findById(specialtyId);

    if (placement.isPresent() && specialty.isPresent()) {
      Placement finalPlacement = placement.get();
      Specialty finalSpecialty = specialty.get();
      populateSpecialtyDetails(finalPlacement, getSpecialtyName(finalSpecialty));
      enrich(finalPlacement, true, true, false);
    }

    if (!placement.isPresent()) {
      placementService.request(placementId);
    }

    if (!specialty.isPresent()) {
      specialtyService.request(specialtyId);
    }

  }

  /**
   * Enrich placements associated with the Specialty with the given specialty name, if this is null
   * it will be queried for.
   *
   * @param specialty     The specialty to get associated placements from.
   * @param specialtyName The specialty name to enrich with.
   */
  private void enrich(Specialty specialty, @Nullable String specialtyName) {

    if (specialtyName == null) {
      specialtyName = getSpecialtyName(specialty);
    }

    if (specialtyName != null) {
      String specialtyId = specialty.getTisId();
      Set<Placement> placements = getPlacementsBySpecialtyId(specialtyId);

      final String finalSpecialtyName = specialtyName;
      placements.forEach(
          placement -> {
            populateSpecialtyDetails(placement, finalSpecialtyName);
            enrich(placement, true, true, false);
          }
      );
    }
  }

  /**
   * Sync an enriched placement with the associated site as the starting point.
   *
   * @param site The site triggering placement enrichment.
   */
  public void enrich(Site site) {
    enrich(site, null, null);
  }

  /**
   * Enrich placements associated with the Site with the given site name and location, if these are
   * null they will be queried for.
   *
   * @param site         The site to get associated placements from.
   * @param siteName     The site name to enrich with.
   * @param siteLocation The site location to enrich with.
   */
  private void enrich(Site site, @Nullable String siteName,
      @Nullable String siteLocation) {

    if (siteName == null) {
      siteName = getSiteName(site);
    }

    if (siteLocation == null) {
      siteLocation = getSiteLocation(site);
    }

    if (siteName != null || siteLocation != null) {
      String id = site.getTisId();
      Set<Placement> placements = placementService.findBySiteId(id);

      final String finalSiteName = siteName;
      final String finalSiteLocation = siteLocation;
      placements.forEach(
          placement -> {
            populateSiteDetails(placement, finalSiteName, finalSiteLocation);
            enrich(placement, true, false, true);
          }
      );
    }
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
   * Get the Placement Specialty ID from the placement.
   *
   * @param placement The placement to get the placmeent specialty id from.
   * @return The placement specialty id.
   *
   *          Note: since only one primary specialty exists per placement,
   *          we use placementId as placement specialty ID
   */
  private String getPlacementSpecialtyId(Placement placement) {
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
   * Get the Placement ID from the placement specialty.
   *
   * @param placementSpecialty The placement specialty to get the placement id from.
   * @return The placement id.
   */
  private String getPlacementId(PlacementSpecialty placementSpecialty) {
    return placementSpecialty.getData().get(PLACEMENT_SPECIALTY_PLACEMENT_ID);
  }

  /**
   * Get the Placements for the specialty ID.
   *
   * @param id The specialty id to get the placements from.
   * @return The placements.
   */
  private Set<Placement> getPlacementsBySpecialtyId(String id) {
    Set<PlacementSpecialty> placementSpecialties = placementSpecialtyService
        .findPlacementSpecialtiesBySpecialtyId(id);

    return placementSpecialties.stream()
        .map(Record::getData)
        .map(data -> data.get(PLACEMENT_SPECIALTY_PLACEMENT_ID))
        .map(placementService::findById)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toSet());
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

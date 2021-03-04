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

package uk.nhs.hee.tis.trainee.sync.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;

import com.amazonaws.services.sqs.AmazonSQS;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import uk.nhs.hee.tis.trainee.sync.config.MongoConfiguration;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.Site;
import uk.nhs.hee.tis.trainee.sync.repository.SiteRepository;
import uk.nhs.hee.tis.trainee.sync.service.SiteSyncService;


@SpringBootTest
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
class CachingSiteIntTest {

  private static final String SITE_FORDY = "fordy";

  // We require access to the mock before the proxy wraps it.
  private static SiteRepository mockSiteRepository;

  @Autowired
  SiteSyncService siteSyncService;

  @Autowired
  CacheManager cacheManager;

  private Cache siteCache;

  private Site site;

  @BeforeEach
  void setup() {
    site = new Site();
    site.setTisId(SITE_FORDY);
    site.setOperation(Operation.DELETE);
    site.setTable(Site.ENTITY_NAME);

    siteCache = cacheManager.getCache(Site.ENTITY_NAME);
  }

  @Test
  void shouldHitCacheOnSecondInvocation() {
    when(mockSiteRepository.findById(SITE_FORDY))
        .thenReturn(Optional.of(site), Optional.of(new Site()));
    assertThat(siteCache.get(SITE_FORDY)).isNull();

    Optional<Site> actual1 = siteSyncService.findById(SITE_FORDY);
    assertThat(siteCache.get(SITE_FORDY)).isNotNull();
    Optional<Site> actual2 = siteSyncService.findById(SITE_FORDY);

    verify(mockSiteRepository).findById(SITE_FORDY);
    assertThat(actual1).isPresent().get().isEqualTo(site).isEqualTo(actual2.orElseThrow());
  }

  @Test
  void shouldEvictWhenDeletedAndMissCacheOnNextInvocation() {
    Site otherSite = new Site();
    final String otherKey = "Foo";
    otherSite.setTisId(otherKey);
    otherSite.setOperation(Operation.LOAD);
    when(mockSiteRepository.findById(otherKey)).thenReturn(Optional.of(otherSite));
    siteSyncService.findById(otherKey);
    assertThat(siteCache.get(otherKey)).isNotNull();
    when(mockSiteRepository.findById(SITE_FORDY)).thenReturn(Optional.of(site));
    siteSyncService.findById(SITE_FORDY);
    assertThat(siteCache.get(SITE_FORDY)).isNotNull();

    siteSyncService.syncRecord(site);
    assertThat(siteCache.get(SITE_FORDY)).isNull();
    assertThat(siteCache.get(otherKey)).isNotNull();
    verify(mockSiteRepository).deleteById(SITE_FORDY);

    siteSyncService.findById(SITE_FORDY);
    assertThat(siteCache.get(SITE_FORDY)).isNotNull();

    verify(mockSiteRepository, times(2)).findById(SITE_FORDY);
  }

  @Test
  void shouldReplaceCacheWhenSaved() {
    Site staleSite = new Site();
    staleSite.setTisId(SITE_FORDY);
    staleSite.setTable("Stale");
    staleSite.setOperation(Operation.UPDATE);
    when(mockSiteRepository.save(staleSite)).thenReturn(staleSite);
    siteSyncService.syncRecord(staleSite);
    assertThat(siteCache.get(SITE_FORDY).get()).isEqualTo(staleSite);

    Site updateSite = new Site();
    updateSite.setTable(Site.ENTITY_NAME);
    updateSite.setTisId(SITE_FORDY);
    updateSite.setOperation(Operation.UPDATE);
    when(mockSiteRepository.save(updateSite)).thenReturn(site);

    siteSyncService.syncRecord(updateSite);
    assertThat(siteCache.get(SITE_FORDY).get()).isEqualTo(site);
    verify(mockSiteRepository).save(staleSite);
    verify(mockSiteRepository).save(updateSite);
  }

  @TestConfiguration
  static class Configuration {

    @Primary
    @Bean
    SiteRepository mockSiteRepository() {
      mockSiteRepository = mock(SiteRepository.class);
      return mockSiteRepository;
    }

    ////// Mocks to enable application context //////
    @MockBean
    AmazonSQS amazonSqs;
    @MockBean
    private MongoConfiguration mongoConfiguration;
    /////////////////////////////////////////////////
  }
}

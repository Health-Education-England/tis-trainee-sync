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

package uk.nhs.hee.tis.trainee.sync.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;
import static uk.nhs.hee.tis.trainee.sync.event.UserDesignatedBodyEventListener.USER_DB_DBC;
import static uk.nhs.hee.tis.trainee.sync.event.UserDesignatedBodyEventListener.USER_DB_USER_NAME;

import io.awspring.cloud.autoconfigure.sqs.SqsAutoConfiguration;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.nhs.hee.tis.trainee.sync.config.MongoConfiguration;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.UserDesignatedBody;
import uk.nhs.hee.tis.trainee.sync.repository.UserDesignatedBodyRepository;
import uk.nhs.hee.tis.trainee.sync.service.ReferenceSyncService;
import uk.nhs.hee.tis.trainee.sync.service.UserDesignatedBodySyncService;

@SpringBootTest(properties = {"cloud.aws.region.static=eu-west-2"})
@ActiveProfiles("int")
@EnableAutoConfiguration(exclude = SqsAutoConfiguration.class)
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
class CachingUserDesignatedBodyIntTest {

  private static final String USER_DESIGNATED_BODY_FORDY = "fordy";
  private static final String USERNAME = "theUser";
  private static final String DBC = "dbc";

  // We require access to the mock before the proxy wraps it.
  private static UserDesignatedBodyRepository mockUserDesignatedBodyRepository;

  @Autowired
  UserDesignatedBodySyncService userDbSyncService;

  @MockitoBean
  ReferenceSyncService referenceSyncService;

  @MockitoBean
  private SqsTemplate sqsTemplate;

  @Autowired
  CacheManager cacheManager;

  private Cache dbcCache;

  private UserDesignatedBody userDesignatedBody;

  @BeforeEach
  void setup() {
    userDesignatedBody = new UserDesignatedBody();
    userDesignatedBody.setTisId(USER_DESIGNATED_BODY_FORDY);
    userDesignatedBody.setOperation(Operation.DELETE);
    userDesignatedBody.setTable(UserDesignatedBody.ENTITY_NAME);
    userDesignatedBody.setData(Map.of(USER_DB_USER_NAME, USERNAME, USER_DB_DBC, DBC));

    dbcCache = cacheManager.getCache(UserDesignatedBody.ENTITY_NAME);
  }

  @Test
  void shouldHitCacheOnSecondInvocation() {
    when(mockUserDesignatedBodyRepository.findById(USER_DESIGNATED_BODY_FORDY))
        .thenReturn(Optional.of(userDesignatedBody), Optional.of(new UserDesignatedBody()));
    assertThat(dbcCache.get(USER_DESIGNATED_BODY_FORDY)).isNull();

    Optional<UserDesignatedBody> actual1 = userDbSyncService.findById(USER_DESIGNATED_BODY_FORDY);
    assertThat(dbcCache.get(USER_DESIGNATED_BODY_FORDY)).isNotNull();
    Optional<UserDesignatedBody> actual2 = userDbSyncService.findById(USER_DESIGNATED_BODY_FORDY);

    verify(mockUserDesignatedBodyRepository).findById(USER_DESIGNATED_BODY_FORDY);
    assertThat(actual1).isPresent().get().isEqualTo(userDesignatedBody)
        .isEqualTo(actual2.orElseThrow());
  }

  @Test
  void shouldEvictWhenDeletedAndMissCacheOnNextInvocation() {
    UserDesignatedBody otherUserDesignatedBody = new UserDesignatedBody();
    final String otherKey = "Foo";
    otherUserDesignatedBody.setTisId(otherKey);
    otherUserDesignatedBody.setOperation(Operation.LOAD);
    when(mockUserDesignatedBodyRepository.findById(otherKey)).thenReturn(
        Optional.of(otherUserDesignatedBody));
    userDbSyncService.findById(otherKey);
    assertThat(dbcCache.get(otherKey)).isNotNull();
    when(mockUserDesignatedBodyRepository.findById(USER_DESIGNATED_BODY_FORDY)).thenReturn(
        Optional.of(userDesignatedBody));
    userDbSyncService.findById(USER_DESIGNATED_BODY_FORDY);
    assertThat(dbcCache.get(USER_DESIGNATED_BODY_FORDY)).isNotNull();

    when(mockUserDesignatedBodyRepository.findByUserNameAndDesignatedBodyCode(USERNAME, DBC))
        .thenReturn(Optional.of(userDesignatedBody));

    UserDesignatedBody userDesignatedBodyFromTis = new UserDesignatedBody(); //arrives without ID
    userDesignatedBodyFromTis.setOperation(Operation.DELETE);
    userDesignatedBodyFromTis.setTable(UserDesignatedBody.ENTITY_NAME);
    userDesignatedBodyFromTis.setData(Map.of(USER_DB_USER_NAME, USERNAME, USER_DB_DBC, DBC));
    userDbSyncService.syncRecord(userDesignatedBodyFromTis);

    assertThat(dbcCache.get(USER_DESIGNATED_BODY_FORDY)).isNull();
    assertThat(dbcCache.get(otherKey)).isNotNull();
    verify(mockUserDesignatedBodyRepository).deleteById(USER_DESIGNATED_BODY_FORDY);

    userDbSyncService.findById(USER_DESIGNATED_BODY_FORDY);
    assertThat(dbcCache.get(USER_DESIGNATED_BODY_FORDY)).isNotNull();

    verify(mockUserDesignatedBodyRepository, times(2)).findById(USER_DESIGNATED_BODY_FORDY);
  }

  @Test
  void shouldReplaceCacheWhenSaved() {
    UserDesignatedBody staleUserDesignatedBody = new UserDesignatedBody();
    staleUserDesignatedBody.setTisId(USER_DESIGNATED_BODY_FORDY);
    staleUserDesignatedBody.setTable("Stale");
    staleUserDesignatedBody.setOperation(Operation.UPDATE);
    when(mockUserDesignatedBodyRepository.save(staleUserDesignatedBody)).thenReturn(
        staleUserDesignatedBody);
    userDbSyncService.syncRecord(staleUserDesignatedBody);
    assertThat(dbcCache.get(USER_DESIGNATED_BODY_FORDY).get()).isEqualTo(staleUserDesignatedBody);

    UserDesignatedBody updateUserDesignatedBody = new UserDesignatedBody();
    updateUserDesignatedBody.setTable(UserDesignatedBody.ENTITY_NAME);
    updateUserDesignatedBody.setTisId(USER_DESIGNATED_BODY_FORDY);
    updateUserDesignatedBody.setOperation(Operation.UPDATE);
    when(mockUserDesignatedBodyRepository.save(updateUserDesignatedBody)).thenReturn(
        userDesignatedBody);

    userDbSyncService.syncRecord(updateUserDesignatedBody);
    assertThat(dbcCache.get(USER_DESIGNATED_BODY_FORDY).get()).isEqualTo(userDesignatedBody);
    verify(mockUserDesignatedBodyRepository).save(staleUserDesignatedBody);
    verify(mockUserDesignatedBodyRepository).save(updateUserDesignatedBody);
  }

  @TestConfiguration
  static class Configuration {

    @Primary
    @Bean
    UserDesignatedBodyRepository mockUserDesignatedBodyRepository() {
      mockUserDesignatedBodyRepository = mock(UserDesignatedBodyRepository.class);
      return mockUserDesignatedBodyRepository;
    }

    ////// Mocks to enable application context //////
    @MockitoBean
    private MongoConfiguration mongoConfiguration;
    /////////////////////////////////////////////////
  }
}

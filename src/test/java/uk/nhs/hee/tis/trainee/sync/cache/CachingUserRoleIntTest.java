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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import uk.nhs.hee.tis.trainee.sync.config.MongoConfiguration;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.UserRole;
import uk.nhs.hee.tis.trainee.sync.repository.UserRoleRepository;
import uk.nhs.hee.tis.trainee.sync.service.ReferenceSyncService;
import uk.nhs.hee.tis.trainee.sync.service.UserRoleSyncService;

@SpringBootTest(properties = {"cloud.aws.region.static=eu-west-2"})
@ActiveProfiles("int")
@EnableAutoConfiguration(exclude = SqsAutoConfiguration.class)
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
class CachingUserRoleIntTest {

  private static final String USER_ROLE_FORDY = "fordy";
  private static final String USERNAME = "theUser";
  private static final String ROLENAME = "theRole";

  // We require access to the mock before the proxy wraps it.
  private static UserRoleRepository mockUserRoleRepository;

  @Autowired
  UserRoleSyncService userRoleSyncService;

  @MockBean
  ReferenceSyncService referenceSyncService;

  @MockBean
  private SqsTemplate sqsTemplate;

  @Autowired
  CacheManager cacheManager;

  private Cache dbcCache;

  private UserRole userRole;

  @BeforeEach
  void setup() {
    userRole = new UserRole();
    userRole.setTisId(USER_ROLE_FORDY);
    userRole.setOperation(Operation.DELETE);
    userRole.setTable(UserRole.ENTITY_NAME);
    userRole.setData(Map.of("userName", USERNAME, "roleName", ROLENAME));

    dbcCache = cacheManager.getCache(UserRole.ENTITY_NAME);
  }

  @Test
  void shouldHitCacheOnSecondInvocation() {
    when(mockUserRoleRepository.findById(USER_ROLE_FORDY))
        .thenReturn(Optional.of(userRole), Optional.of(new UserRole()));
    assertThat(dbcCache.get(USER_ROLE_FORDY)).isNull();

    Optional<UserRole> actual1 = userRoleSyncService.findById(USER_ROLE_FORDY);
    assertThat(dbcCache.get(USER_ROLE_FORDY)).isNotNull();
    Optional<UserRole> actual2 = userRoleSyncService.findById(USER_ROLE_FORDY);

    verify(mockUserRoleRepository).findById(USER_ROLE_FORDY);
    assertThat(actual1).isPresent().get().isEqualTo(userRole).isEqualTo(actual2.orElseThrow());
  }

  @Test
  void shouldEvictWhenDeletedAndMissCacheOnNextInvocation() {
    UserRole otherUserRole = new UserRole();
    final String otherKey = "Foo";
    otherUserRole.setTisId(otherKey);
    otherUserRole.setOperation(Operation.LOAD);
    when(mockUserRoleRepository.findById(otherKey)).thenReturn(Optional.of(otherUserRole));
    userRoleSyncService.findById(otherKey);
    assertThat(dbcCache.get(otherKey)).isNotNull();
    when(mockUserRoleRepository.findById(USER_ROLE_FORDY)).thenReturn(Optional.of(userRole));
    userRoleSyncService.findById(USER_ROLE_FORDY);
    assertThat(dbcCache.get(USER_ROLE_FORDY)).isNotNull();

    when(mockUserRoleRepository.findByUserNameAndRoleName(USERNAME, ROLENAME))
        .thenReturn(Optional.of(userRole));
    userRoleSyncService.syncRecord(userRole);

    assertThat(dbcCache.get(USER_ROLE_FORDY)).isNull();
    assertThat(dbcCache.get(otherKey)).isNotNull();
    verify(mockUserRoleRepository).deleteById(USER_ROLE_FORDY);

    userRoleSyncService.findById(USER_ROLE_FORDY);
    assertThat(dbcCache.get(USER_ROLE_FORDY)).isNotNull();

    verify(mockUserRoleRepository, times(2)).findById(USER_ROLE_FORDY);
  }

  @Test
  void shouldReplaceCacheWhenSaved() {
    UserRole staleUserRole = new UserRole();
    staleUserRole.setTisId(USER_ROLE_FORDY);
    staleUserRole.setTable("Stale");
    staleUserRole.setOperation(Operation.UPDATE);
    when(mockUserRoleRepository.save(staleUserRole)).thenReturn(staleUserRole);
    userRoleSyncService.syncRecord(staleUserRole);
    assertThat(dbcCache.get(USER_ROLE_FORDY).get()).isEqualTo(staleUserRole);

    UserRole updateUserRole = new UserRole();
    updateUserRole.setTable(UserRole.ENTITY_NAME);
    updateUserRole.setTisId(USER_ROLE_FORDY);
    updateUserRole.setOperation(Operation.UPDATE);
    when(mockUserRoleRepository.save(updateUserRole)).thenReturn(userRole);

    userRoleSyncService.syncRecord(updateUserRole);
    assertThat(dbcCache.get(USER_ROLE_FORDY).get()).isEqualTo(userRole);
    verify(mockUserRoleRepository).save(staleUserRole);
    verify(mockUserRoleRepository).save(updateUserRole);
  }

  @TestConfiguration
  static class Configuration {

    @Primary
    @Bean
    UserRoleRepository mockUserRoleRepository() {
      mockUserRoleRepository = mock(UserRoleRepository.class);
      return mockUserRoleRepository;
    }

    ////// Mocks to enable application context //////
    @MockBean
    private MongoConfiguration mongoConfiguration;
    /////////////////////////////////////////////////
  }
}

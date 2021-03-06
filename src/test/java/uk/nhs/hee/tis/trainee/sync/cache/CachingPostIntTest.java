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

import io.awspring.cloud.autoconfigure.messaging.SqsAutoConfiguration;
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
import uk.nhs.hee.tis.trainee.sync.config.MongoConfiguration;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.Post;
import uk.nhs.hee.tis.trainee.sync.repository.PostRepository;
import uk.nhs.hee.tis.trainee.sync.service.PostSyncService;

@SpringBootTest
@EnableAutoConfiguration(exclude = SqsAutoConfiguration.class)
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
class CachingPostIntTest {

  private static final String POST_FORDY = "fordy";

  // We require access to the mock before the proxy wraps it.
  private static PostRepository mockPostRepository;

  @Autowired
  PostSyncService postSyncService;

  @Autowired
  CacheManager cacheManager;

  private Cache postCache;

  private Post post;

  @BeforeEach
  void setup() {
    post = new Post();
    post.setTisId(POST_FORDY);
    post.setOperation(Operation.DELETE);
    post.setTable(Post.ENTITY_NAME);

    postCache = cacheManager.getCache(Post.ENTITY_NAME);
  }

  @Test
  void shouldHitCacheOnSecondInvocation() {
    when(mockPostRepository.findById(POST_FORDY))
        .thenReturn(Optional.of(post), Optional.of(new Post()));
    assertThat(postCache.get(POST_FORDY)).isNull();

    Optional<Post> actual1 = postSyncService.findById(POST_FORDY);
    assertThat(postCache.get(POST_FORDY)).isNotNull();
    Optional<Post> actual2 = postSyncService.findById(POST_FORDY);

    verify(mockPostRepository).findById(POST_FORDY);
    assertThat(actual1).isPresent().get().isEqualTo(post).isEqualTo(actual2.orElseThrow());
  }

  @Test
  void shouldEvictWhenDeletedAndMissCacheOnNextInvocation() {
    Post otherPost = new Post();
    final String otherKey = "Foo";
    otherPost.setTisId(otherKey);
    otherPost.setOperation(Operation.LOAD);
    when(mockPostRepository.findById(otherKey)).thenReturn(Optional.of(otherPost));
    postSyncService.findById(otherKey);
    assertThat(postCache.get(otherKey)).isNotNull();
    when(mockPostRepository.findById(POST_FORDY)).thenReturn(Optional.of(post));
    postSyncService.findById(POST_FORDY);
    assertThat(postCache.get(POST_FORDY)).isNotNull();

    postSyncService.syncRecord(post);
    assertThat(postCache.get(POST_FORDY)).isNull();
    assertThat(postCache.get(otherKey)).isNotNull();
    verify(mockPostRepository).deleteById(POST_FORDY);

    postSyncService.findById(POST_FORDY);
    assertThat(postCache.get(POST_FORDY)).isNotNull();

    verify(mockPostRepository, times(2)).findById(POST_FORDY);
  }

  @Test
  void shouldReplaceCacheWhenSaved() {
    Post stalePost = new Post();
    stalePost.setTisId(POST_FORDY);
    stalePost.setTable("Stale");
    stalePost.setOperation(Operation.UPDATE);
    when(mockPostRepository.save(stalePost)).thenReturn(stalePost);
    postSyncService.syncRecord(stalePost);
    assertThat(postCache.get(POST_FORDY).get()).isEqualTo(stalePost);

    Post updatePost = new Post();
    updatePost.setTable(Post.ENTITY_NAME);
    updatePost.setTisId(POST_FORDY);
    updatePost.setOperation(Operation.UPDATE);
    when(mockPostRepository.save(updatePost)).thenReturn(post);

    postSyncService.syncRecord(updatePost);
    assertThat(postCache.get(POST_FORDY).get()).isEqualTo(post);
    verify(mockPostRepository).save(stalePost);
    verify(mockPostRepository).save(updatePost);
  }

  @TestConfiguration
  static class Configuration {

    @Primary
    @Bean
    PostRepository mockPostRepository() {
      mockPostRepository = mock(PostRepository.class);
      return mockPostRepository;
    }

    ////// Mocks to enable application context //////
    @MockBean
    private MongoConfiguration mongoConfiguration;
    /////////////////////////////////////////////////
  }
}

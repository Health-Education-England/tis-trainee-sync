package uk.nhs.hee.tis.trainee.sync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RequestCacheServiceTest {

  private static RequestCacheService requestCacheService;

  private static RedisCommands<String, String> syncCommands;

  @BeforeAll
  private static void setupService() {
    RedisClient redisClient = mock(RedisClient.class);
    StatefulRedisConnection<String, String> connection = mock(StatefulRedisConnection.class);
    syncCommands = mock(RedisCommands.class);

    when(redisClient.connect()).thenReturn(connection);
    when(connection.sync()).thenReturn(syncCommands);

    requestCacheService = new RequestCacheService(redisClient);
    requestCacheService.redisTtl = 1L;
  }

  @Test
  void shouldIdentifyIfItemIsInCache() {
    when(syncCommands.exists(any())).thenReturn(1L);

    assertTrue(requestCacheService.isItemInCache("SomeEntity","ID"));
  }

  @Test
  void shouldIdentifyIfItemIsNotInCache() {
    when(syncCommands.exists(any())).thenReturn(0L);

    assertFalse(requestCacheService.isItemInCache("SomeEntity", "ID"));
  }

  @Test
  void shouldDeleteFromCache() {
    when(syncCommands.del(any())).thenReturn(99L);

    assertThat(requestCacheService.deleteItemFromCache("SomeEntity", "ID")).isEqualTo(99L);
  }

  @Test
  void shouldAddItemToCache() {
    when(syncCommands.set(any(), any(), any())).thenReturn("OK");

    assertThat(requestCacheService.addItemToCache("SomeEntity", "ID")).isEqualTo("OK");
  }
}

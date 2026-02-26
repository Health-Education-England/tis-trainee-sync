/*
 * The MIT License (MIT)
 *
 * Copyright 2022 Crown Copyright (Health Education England)
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

package uk.nhs.hee.tis.trainee.sync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
  static void setupService() {
    RedisClient redisClient = mock(RedisClient.class);
    StatefulRedisConnection<String, String> connection = mock(StatefulRedisConnection.class);
    syncCommands = mock(RedisCommands.class);

    when(redisClient.connect()).thenReturn(connection);
    when(connection.sync()).thenReturn(syncCommands);

    requestCacheService = new RequestCacheService(redisClient);
    requestCacheService.setRedisTtl(1L);
  }

  @Test
  void shouldIdentifyIfItemIsInCache() {
    when(syncCommands.exists(any())).thenReturn(1L);

    assertTrue(requestCacheService.isItemInCache("SomeEntity", "ID"));
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

    assertThat(requestCacheService.addItemToCache(eq("SomeEntity"), eq("ID"), any()))
        .isEqualTo("OK");
  }
}

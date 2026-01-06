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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class RequestCacheServiceTest {

  private RequestCacheService requestCacheService;

  private RedisTemplate<String, String> template;
  private ValueOperations<String, String> operations;

  @BeforeEach
  void setUp() {
    template = mock(RedisTemplate.class);

    operations = mock(ValueOperations.class);
    when(template.opsForValue()).thenReturn(operations);

    requestCacheService = new RequestCacheService(template);
    requestCacheService.setRedisTtl(1L);
  }

  @Test
  void shouldIdentifyIfItemIsInCache() {
    when(template.hasKey(any())).thenReturn(true);

    assertTrue(requestCacheService.isItemInCache("SomeEntity", "ID"));
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(booleans = false)
  void shouldIdentifyIfItemIsNotInCache(Boolean hasKey) {
    when(template.hasKey(any())).thenReturn(hasKey);

    assertFalse(requestCacheService.isItemInCache("SomeEntity", "ID"));
  }

  @Test
  void shouldDeleteFromCacheIfExists() {
    when(template.delete((String) any())).thenReturn(true);

    assertTrue(requestCacheService.deleteItemFromCache("SomeEntity", "ID"));
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(booleans = false)
  void shouldNotDeleteFromCacheIfNotExists(Boolean exists) {
    when(template.delete((String) any())).thenReturn(exists);

    assertFalse(requestCacheService.deleteItemFromCache("SomeEntity", "ID"));
  }

  @Test
  void shouldAddItemToCache() {
    requestCacheService.setRedisTtl(40L);

    requestCacheService.addItemToCache("SomeEntity", "ID", "requestValue");

    verify(operations).set("SomeEntity::ID::request", "requestValue", Duration.ofMinutes(40));
  }
}

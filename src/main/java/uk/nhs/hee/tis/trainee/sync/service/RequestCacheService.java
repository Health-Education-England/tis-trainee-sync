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

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
public class RequestCacheService {

  private static final String KEY_DELIMITER = "::";
  private static final String KEY_SUFFIX = "request";

  @Value("${spring.data.redis.requests-cache.time-to-live}")
  private Long redisTtl;

  private final RedisTemplate<String, String> redisTemplate;

  RequestCacheService(RedisTemplate<String, String> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  public boolean isItemInCache(String entityType, String id) {
    return Boolean.TRUE.equals(redisTemplate.hasKey(getCacheKey(entityType, id)));
  }

  public boolean deleteItemFromCache(String entityType, String id) {
    return Boolean.TRUE.equals(redisTemplate.delete(getCacheKey(entityType, id)));
  }

  public void addItemToCache(String entityType, String id, String request) {
    redisTemplate.opsForValue()
        .set(getCacheKey(entityType, id), request, Duration.ofMinutes(redisTtl));
  }

  String getCacheKey(String entityType, String id) {
    return entityType + KEY_DELIMITER + id + KEY_DELIMITER + KEY_SUFFIX;
  }

  void setRedisTtl(Long ttl) {
    this.redisTtl = ttl;
  }
}

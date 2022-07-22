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

import io.lettuce.core.RedisClient;
import io.lettuce.core.SetArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import java.time.Duration;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RequestCacheService {
  private static final String KEY_DELIMITER = "::";
  private static final String KEY_SUFFIX = "request";

  @Value("${spring.redis.requests-cache.database}")
  private Integer redisDb;
  @Value("${spring.redis.requests-cache.time-to-live}")
  private Long redisTtl;

  private final RedisCommands<String, String> syncCommands;

  RequestCacheService(RedisClient redisClient) {
    StatefulRedisConnection<String, String> connection = redisClient.connect();
    syncCommands = connection.sync();
  }

  @PostConstruct
  void setDb() {
    syncCommands.select(redisDb);
  }

  public boolean isItemInCache(String entityType, String id) {
    return syncCommands.exists(getCacheKey(entityType, id)) != 0;
  }

  public Long deleteItemFromCache(String entityType, String id) {
    return syncCommands.del(getCacheKey(entityType, id));
  }

  public String addItemToCache(String entityType, String id, String request) {
    return syncCommands.set(getCacheKey(entityType, id), request,
        new SetArgs().ex(Duration.ofMinutes(redisTtl)));
  }

  String getCacheKey(String entityType, String id) {
    return entityType + KEY_DELIMITER + id + KEY_DELIMITER + KEY_SUFFIX;
  }

  void setRedisTtl(Long ttl) {
    this.redisTtl = ttl;
  }
}

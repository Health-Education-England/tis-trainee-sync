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

package uk.nhs.hee.tis.trainee.sync.service;

import io.lettuce.core.RedisClient;
import io.lettuce.core.SetArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RequestCacheService {

  @Value("${spring.redis.requests-cache.database}")
  Integer redisDb;
  @Value("${spring.redis.requests-cache.time-to-live}")
  Long redisTtl;

  public static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm:ss");

  RedisCommands<String, String> syncCommands;

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

  public String addItemToCache(String entityType, String id) {
    return syncCommands.set(getCacheKey(entityType, id), dtf.format(LocalDateTime.now()),
        new SetArgs().ex(Duration.ofMinutes(redisTtl)));
  }

  String getCacheKey(String entityType, String id) {
    return entityType + id;
  }
}

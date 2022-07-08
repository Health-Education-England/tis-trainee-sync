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

package uk.nhs.hee.tis.trainee.sync.config;

import io.lettuce.core.RedisClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

@Configuration
public class RedisConfig extends CachingConfigurerSupport {

  private static final String CACHE_NAME = "redisCache";
  private static final Integer CACHE_TTL_DATA = 2;
  private static final Integer CACHE_TTL_REQUEST = 10; //TODO rationalise the configurations below and incorporate this
  //TODO set reasonable value (for data, this might be infinite; for requests, maybe 24hrs?)

  @Value("${spring.redis.host}")
  String host;

  @Value("${spring.redis.port}")
  Integer port;

  @Value("${spring.redis.password}")
  String password;

  @Value("${spring.redis.url}")
  String redisUrl;

  @Bean
  public LettuceConnectionFactory redisConnectionFactory() {
    RedisStandaloneConfiguration redisConf = new RedisStandaloneConfiguration(host, port);
    redisConf.setPassword(password);
    return new LettuceConnectionFactory(redisConf);
  }

  @Bean
  public RedisClient getRedisClient() {
    RedisClient redisClient = RedisClient.create(redisUrl);
    return redisClient;
  }

  @Bean
  public RedisTemplate redisTemplate(LettuceConnectionFactory lettuceConnectionFactory) {

    RedisTemplate redisTemplate = new RedisTemplate();

    redisTemplate.setKeySerializer(new GenericJackson2JsonRedisSerializer());

    redisTemplate.setConnectionFactory(lettuceConnectionFactory);
    return redisTemplate;
  }

  /**
   * Configuration for the data cache.
   *
   * @return a RedisCacheConfiguration
   */
  @Bean
  public RedisCacheConfiguration cacheConfiguration() {

    return RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(CACHE_TTL_DATA))
        //.disableCachingNullValues()
        //i.e. allow NULLs to be cached
        .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
            new GenericJackson2JsonRedisSerializer()));
  }

  @Bean
  public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
    return (builder) -> builder
        .withCacheConfiguration(CACHE_NAME, cacheConfiguration());
  }

}

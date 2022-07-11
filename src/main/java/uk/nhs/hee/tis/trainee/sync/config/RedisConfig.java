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
import io.lettuce.core.RedisURI;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

@Configuration
public class RedisConfig extends CachingConfigurerSupport {

  @Value("${spring.redis.host}")
  String host;

  @Value("${spring.redis.port}")
  Integer port;

  @Value("${spring.redis.user}")
  String user;

  @Value("${spring.redis.password}")
  char[] password;

  @Value("${spring.redis.timeout}")
  Long timeout;

  @Value("${spring.redis.data-cache.ttl}")
  Long dataTtl; //TODO fixme, this is ugly

  /**
   * Configuration for the requests cache.
   *
   * @return a Lettuce RedisClient
   */
  @Bean
  public RedisClient getRedisClient() {
    RedisURI redisUri = new RedisURI();
    redisUri.setHost(host);
    redisUri.setPort(port);
    redisUri.setPassword(password);
    redisUri.setUsername(user);
    redisUri.setTimeout(Duration.ofSeconds(timeout));
    return RedisClient.create(redisUri);
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
  //TODO: rather figure out a lettuce config if possible
  @Bean
  public RedisCacheConfiguration cacheConfiguration() {

    return RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(dataTtl))
        //TODO how set DB?
        //.disableCachingNullValues()
        //i.e. allow NULLs to be cached
        .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
            new GenericJackson2JsonRedisSerializer()));
  }

}

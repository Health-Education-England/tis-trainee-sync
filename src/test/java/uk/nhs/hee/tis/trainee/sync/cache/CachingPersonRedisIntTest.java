package uk.nhs.hee.tis.trainee.sync.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import redis.embedded.RedisServer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Optional;
import uk.nhs.hee.tis.trainee.sync.config.MongoConfiguration;
import uk.nhs.hee.tis.trainee.sync.config.RedisCacheConfig;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.Person;
import uk.nhs.hee.tis.trainee.sync.repository.PersonRepository;
import uk.nhs.hee.tis.trainee.sync.service.PersonService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Import({RedisCacheConfig.class, PersonService.class})
@ExtendWith(SpringExtension.class)
@ImportAutoConfiguration(classes = {CacheAutoConfiguration.class, RedisAutoConfiguration.class})
@EnableCaching
class CachingPersonRedisIntTest {

  private static final String PERSON_FORDY = "fordy";

  // We require access to the mock before the proxy wraps it.
  private static PersonRepository mockPersonRepository;

  @Autowired
  PersonService personService;

  @Autowired
  CacheManager cacheManager;

  private Cache personCache;

  private Person person;

  @BeforeEach
  void setup() {
    person = new Person();
    person.setTisId(PERSON_FORDY);
    person.setOperation(Operation.DELETE);
    person.setTable(Person.ENTITY_NAME);

    personCache = cacheManager.getCache(Person.ENTITY_NAME);
  }

  @Test
  void givenRedisCaching_whenFindPersonById_thenItemReturnedFromCache() {
    given(mockPersonRepository.findById(PERSON_FORDY))
        .willReturn(Optional.of(person));

    Optional<Person> personCacheMiss = personService.findById(PERSON_FORDY);
    Optional<Person> personCacheHit = personService.findById(PERSON_FORDY);

    assertThat(personCacheMiss).contains(person);
    assertThat(personCacheHit).contains(person);

    verify(mockPersonRepository, times(1)).findById(PERSON_FORDY);
    assertThat(personFromCache()).isEqualTo(person);
  }

  private Object personFromCache() {
    return personCache.get(PERSON_FORDY).get();
  }

  @TestConfiguration
  static class Configuration {

    @Primary
    @Bean
    PersonRepository mockPersonRepository() {
      mockPersonRepository = mock(PersonRepository.class);
      return mockPersonRepository;
    }

    ////// Mocks to enable application context //////
    @MockBean
    private MongoConfiguration mongoConfiguration;
    /////////////////////////////////////////////////

    private final RedisServer redisServer;

    public Configuration() {
      this.redisServer = new RedisServer();
    }

    //FIXME: blocked by group policy on Windows machine

    @PostConstruct
    public void startRedis() {
      redisServer.start();
    }

    @PreDestroy
    public void stopRedis() {
      this.redisServer.stop();
    }
  }

}

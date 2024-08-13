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

import io.awspring.cloud.autoconfigure.sqs.SqsAutoConfiguration;
import io.awspring.cloud.sqs.operations.SqsTemplate;
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
import org.springframework.test.context.ActiveProfiles;
import uk.nhs.hee.tis.trainee.sync.config.MongoConfiguration;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.Person;
import uk.nhs.hee.tis.trainee.sync.repository.PersonRepository;
import uk.nhs.hee.tis.trainee.sync.service.PersonService;

@SpringBootTest(properties = {"cloud.aws.region.static=eu-west-2"})
@ActiveProfiles("int")
@EnableAutoConfiguration(exclude = SqsAutoConfiguration.class)
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
class CachingPersonIntTest {

  private static final String PERSON_FORDY = "fordy";

  // We require access to the mock before the proxy wraps it.
  private static PersonRepository mockPersonRepository;

  @Autowired
  PersonService personService;

  @Autowired
  CacheManager cacheManager;

  @MockBean
  private SqsTemplate sqsTemplate;

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
  void shouldHitCacheOnSecondInvocation() {
    when(mockPersonRepository.findById(PERSON_FORDY))
        .thenReturn(Optional.of(person), Optional.of(new Person()));
    assertThat(personCache.get(PERSON_FORDY)).isNull();

    Optional<Person> actual1 = personService.findById(PERSON_FORDY);
    assertThat(personCache.get(PERSON_FORDY)).isNotNull();
    Optional<Person> actual2 = personService.findById(PERSON_FORDY);

    verify(mockPersonRepository).findById(PERSON_FORDY);
    assertThat(actual1).isPresent().get().isEqualTo(person).isEqualTo(actual2.orElseThrow());
  }

  @Test
  void shouldEvictWhenDeletedAndMissCacheOnNextInvocation() {
    Person otherPerson = new Person();
    final String otherKey = "Foo";
    otherPerson.setTisId(otherKey);
    otherPerson.setOperation(Operation.LOAD);
    when(mockPersonRepository.findById(otherKey)).thenReturn(Optional.of(otherPerson));
    personService.findById(otherKey);
    assertThat(personCache.get(otherKey)).isNotNull();
    when(mockPersonRepository.findById(PERSON_FORDY)).thenReturn(Optional.of(person));
    personService.findById(PERSON_FORDY);
    assertThat(personCache.get(PERSON_FORDY)).isNotNull();

    personService.deleteById(person.getTisId());
    assertThat(personCache.get(PERSON_FORDY)).isNull();
    assertThat(personCache.get(otherKey)).isNotNull();
    verify(mockPersonRepository).deleteById(PERSON_FORDY);

    personService.findById(PERSON_FORDY);
    assertThat(personCache.get(PERSON_FORDY)).isNotNull();

    verify(mockPersonRepository, times(2)).findById(PERSON_FORDY);
  }

  @Test
  void shouldReplaceCacheWhenSaved() {
    Person stalePerson = new Person();
    stalePerson.setTisId(PERSON_FORDY);
    stalePerson.setTable("Stale");
    stalePerson.setOperation(Operation.UPDATE);
    when(mockPersonRepository.save(stalePerson)).thenReturn(stalePerson);
    personService.save(stalePerson);
    assertThat(personCache.get(PERSON_FORDY).get()).isEqualTo(stalePerson);

    Person updatePerson = new Person();
    updatePerson.setTable(Person.ENTITY_NAME);
    updatePerson.setTisId(PERSON_FORDY);
    updatePerson.setOperation(Operation.UPDATE);
    when(mockPersonRepository.save(updatePerson)).thenReturn(person);

    personService.save(updatePerson);
    assertThat(personCache.get(PERSON_FORDY).get()).isEqualTo(person);
    verify(mockPersonRepository).save(stalePerson);
    verify(mockPersonRepository).save(updatePerson);
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
  }
}

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;

import io.awspring.cloud.autoconfigure.messaging.SqsAutoConfiguration;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
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
import org.springframework.util.ReflectionUtils;
import org.springframework.web.client.RestTemplate;
import uk.nhs.hee.tis.trainee.sync.config.MongoConfiguration;
import uk.nhs.hee.tis.trainee.sync.mapper.TraineeDetailsMapperImpl;
import uk.nhs.hee.tis.trainee.sync.mapper.util.TraineeDetailsUtil;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.Person;
import uk.nhs.hee.tis.trainee.sync.repository.PersonRepository;

@SpringBootTest
@EnableAutoConfiguration(exclude = SqsAutoConfiguration.class)
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
class PersonCachingTest {

  private static final String ID = "40";

  @Autowired
  private static PersonRepository mockPersonRepository;

  @Autowired
  CacheManager cacheManager;

  private RestTemplate restTemplate;

  @Autowired
  private PersonService personService;

  private Cache personCache;

  private Person record;

  private TcsSyncService service;

  private Map<String, String> data;


  @BeforeEach
  void setUp() {

    TraineeDetailsMapperImpl mapper = new TraineeDetailsMapperImpl();
    Field field = ReflectionUtils.findField(TraineeDetailsMapperImpl.class, "traineeDetailsUtil");
    assert field != null;
    field.setAccessible(true);
    ReflectionUtils.setField(field, mapper, new TraineeDetailsUtil());

    restTemplate = mock(RestTemplate.class);
    service = new TcsSyncService(restTemplate, mapper, personService);

    record = new Person();
    record.setTisId(ID);
    record.setOperation(Operation.DELETE);
    record.setTable(Person.ENTITY_NAME);

    personCache = cacheManager.getCache(Person.ENTITY_NAME);
  }

  @Test
  void shouldUseCacheForSecondInvocationOfRecord() {

    when(mockPersonRepository.findById(record.getTisId()))
        .thenReturn(Optional.of(record), Optional.of(new Person()));
    assertThat(personCache.get(record.getTisId())).isNull();

    Optional<Person> actual1 = personService.findById(record.getTisId());
    assertThat(personCache.get(record.getTisId())).isNotNull();
    Optional<Person> actual2 = personService.findById(record.getTisId());

    verify(mockPersonRepository).findById(record.getTisId());
    assertThat(actual1).isPresent().get().isEqualTo(record)
        .isEqualTo(actual2.orElseThrow());
  }

  @Test
  void shouldEvictFromCacheAndMissCacheOnNextInvocation() {

    Person otherPerson = new Person();
    final String otherId = "30";
    otherPerson.setTisId(otherId);
    otherPerson.setOperation(Operation.LOAD);

    when(mockPersonRepository.findById(otherId)).thenReturn(Optional.of(otherPerson));
    service.findById(otherId);

    assertThat(personCache.get(otherId)).isNotNull();
    when(mockPersonRepository.findById(ID)).thenReturn(Optional.of(record));

    service.findById(ID);

    assertThat(personCache.get(ID)).isNotNull();

    service.syncRecord(record);

    assertThat(personCache.get(ID)).isNull();
    assertThat(personCache.get(otherId)).isNotNull();

    verify(mockPersonRepository).deleteById(ID);

    service.findById(ID);
    assertThat(personCache.get(ID)).isNotNull();

    verify(mockPersonRepository, times(2)).findById(record.getTisId());
  }

  @Test
  void shouldReplaceCacheWhenSaved() {

    data = new HashMap<>();
    data.put("surname", "oldSurname");
    data.put("role", "DR in Training");

    Person stalePerson = new Person();

    stalePerson.setTisId(ID);
    stalePerson.setTable(Person.ENTITY_NAME);
    stalePerson.setOperation(Operation.UPDATE);
    stalePerson.setData(data);

    when(mockPersonRepository.save(stalePerson)).thenReturn(stalePerson);
    service.syncRecord(stalePerson);
    assertThat(personCache.get(ID).get()).isEqualTo(stalePerson);

    Person updatePerson = new Person();
    data.put("surname", "newSurname");
    updatePerson.setTable(Person.ENTITY_NAME);
    updatePerson.setTisId(ID);
    updatePerson.setOperation(Operation.UPDATE);
    updatePerson.setData(data);
    when(mockPersonRepository.save(updatePerson)).thenReturn(record);

    service.syncRecord(updatePerson);
    assertThat(personCache.get(ID).get()).isEqualTo(updatePerson);
    assertThat(((Person) personCache.get(ID).get()).getData().get("surname"))
        .isEqualTo(data.get("surname"));
    verify(mockPersonRepository).save(stalePerson);
    verify(mockPersonRepository).save(updatePerson);
  }

  @TestConfiguration
  static class Configuration {

    ////// Mocks to enable application context //////
    @MockBean
    private MongoConfiguration mongoConfiguration;

    @Primary
    @Bean
    PersonRepository mockPersonRepository() {
      mockPersonRepository = mock(PersonRepository.class);
      return mockPersonRepository;
    }
    /////////////////////////////////////////////////
  }
}

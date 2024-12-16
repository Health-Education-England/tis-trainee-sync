/*
 * The MIT License (MIT)
 *
 * Copyright 2024 Crown Copyright (Health Education England)
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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.nhs.hee.tis.trainee.sync.model.Operation.DELETE;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.model.UserDesignatedBody;
import uk.nhs.hee.tis.trainee.sync.repository.UserDesignatedBodyRepository;

class UserDesignatedBodySyncServiceTest {

  private static final String ID = "UserDesignatedBodyId";
  private static final String ID_2 = "UserDesignatedBodyId2";
  private static final String DBC = "dbc";
  private static final String USERNAME = "theUser";

  private UserDesignatedBodySyncService service;
  private UserDesignatedBodyRepository repository;

  private UserDesignatedBody userDesignatedBody;
  private UserDesignatedBody userDesignatedBody2;
  private UserDesignatedBody userDesignatedBodyFromTis;

  @BeforeEach
  void setUp() {
    repository = mock(UserDesignatedBodyRepository.class);

    service = new UserDesignatedBodySyncService(repository);

    userDesignatedBody = new UserDesignatedBody();
    userDesignatedBody.setTisId(ID);
    userDesignatedBody.setData(Map.of("userName", USERNAME, "designatedBodyCode", DBC));
    userDesignatedBody2 = new UserDesignatedBody();
    userDesignatedBody2.setTisId(ID_2);
    userDesignatedBody2.setData(Map.of("userName", USERNAME, "designatedBodyCode", DBC));
    userDesignatedBodyFromTis = new UserDesignatedBody(); //no ID
    userDesignatedBodyFromTis.setData(Map.of("userName", USERNAME, "designatedBodyCode", DBC));
  }

  @Test
  void shouldThrowExceptionIfRecordNotUserDesignatedBody() {
    Record theRecord = new Record();
    assertThrows(IllegalArgumentException.class, () -> service.syncRecord(theRecord));
  }

  @ParameterizedTest(name = "Should store records when operation is {0}.")
  @EnumSource(value = Operation.class, names = {"LOAD", "INSERT", "UPDATE"})
  void shouldStoreRecordIfNotExists(Operation operation) {
    when(repository.findByUserNameAndDesignatedBodyCode(USERNAME, DBC))
        .thenReturn(Optional.empty());

    userDesignatedBodyFromTis.setOperation(operation);
    service.syncRecord(userDesignatedBodyFromTis);

    verify(repository).findByUserNameAndDesignatedBodyCode(USERNAME, DBC);
    verify(repository).save(userDesignatedBodyFromTis);
    verifyNoMoreInteractions(repository);
  }

  @ParameterizedTest(name = "Should not store records when operation is {0}.")
  @EnumSource(value = Operation.class, names = {"LOAD", "INSERT", "UPDATE"})
  void shouldNotStoreRecordIfExists(Operation operation) {
    when(repository.findByUserNameAndDesignatedBodyCode(USERNAME, DBC))
        .thenReturn(Optional.of(userDesignatedBody));

    userDesignatedBodyFromTis.setOperation(operation);
    service.syncRecord(userDesignatedBodyFromTis);

    verify(repository).findByUserNameAndDesignatedBodyCode(USERNAME, DBC);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldDeleteRecordFromStoreIfExists() {
    when(repository.findByUserNameAndDesignatedBodyCode(USERNAME, DBC))
        .thenReturn(Optional.of(userDesignatedBody));

    userDesignatedBodyFromTis.setOperation(DELETE);
    service.syncRecord(userDesignatedBodyFromTis);

    verify(repository).findByUserNameAndDesignatedBodyCode(USERNAME, DBC);
    verify(repository).deleteById(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotDeleteRecordFromStoreIfNotExists() {
    when(repository.findByUserNameAndDesignatedBodyCode(USERNAME, DBC))
        .thenReturn(Optional.empty());

    userDesignatedBodyFromTis.setOperation(DELETE);
    service.syncRecord(userDesignatedBodyFromTis);

    verify(repository).findByUserNameAndDesignatedBodyCode(USERNAME, DBC);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldFindRecordByIdWhenExists() {
    when(repository.findById(ID)).thenReturn(Optional.of(userDesignatedBody));

    Optional<UserDesignatedBody> found = service.findById(ID);
    assertThat("Record not found.", found.isPresent(), is(true));
    assertThat("Unexpected record.", found.orElse(null), sameInstance(userDesignatedBody));

    verify(repository).findById(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotFindRecordByIdWhenNotExists() {
    when(repository.findById(ID)).thenReturn(Optional.empty());

    Optional<UserDesignatedBody> found = service.findById(ID);
    assertThat("Record not found.", found.isEmpty(), is(true));

    verify(repository).findById(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldFindByUserNameWhenExists() {
    when(repository.findByUserName(USERNAME))
        .thenReturn(Set.of(userDesignatedBody, userDesignatedBody2));

    Set<UserDesignatedBody> found = service.findByUserName(USERNAME);
    assertThat("Unexpected number of records.", found.size(), is(2));
    assertThat("Unexpected record.", found.contains(userDesignatedBody), is(true));
    assertThat("Unexpected record.", found.contains(userDesignatedBody2), is(true));

    verify(repository).findByUserName(USERNAME);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotFindByUserNameWhenNotExists() {
    when(repository.findByUserName(USERNAME)).thenReturn(Set.of());

    Set<UserDesignatedBody> found = service.findByUserName(USERNAME);
    assertThat("Record not found.", found.isEmpty(), is(true));

    verify(repository).findByUserName(USERNAME);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldFindByDbcWhenExists() {
    when(repository.findByDbc(DBC)).thenReturn(Set.of(userDesignatedBody, userDesignatedBody2));

    Set<UserDesignatedBody> found = service.findByDbc(DBC);
    assertThat("Unexpected number of records.", found.size(), is(2));
    assertThat("Unexpected record.", found.contains(userDesignatedBody), is(true));
    assertThat("Unexpected record.", found.contains(userDesignatedBody2), is(true));

    verify(repository).findByDbc(DBC);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotFindByDbcWhenNotExists() {
    when(repository.findByDbc(DBC)).thenReturn(Set.of());

    Set<UserDesignatedBody> found = service.findByDbc(DBC);
    assertThat("Record not found.", found.isEmpty(), is(true));

    verify(repository).findByDbc(DBC);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldFindRecordByUserNameAndDesignatedBodyCodeWhenExists() {
    when(repository.findByUserNameAndDesignatedBodyCode(USERNAME, DBC))
        .thenReturn(Optional.of(userDesignatedBody));

    Optional<UserDesignatedBody> found = service.findByUserNameAndDesignatedBodyCode(USERNAME, DBC);
    assertThat("Record not found.", found.isPresent(), is(true));
    assertThat("Unexpected record.", found.orElse(null), sameInstance(userDesignatedBody));

    verify(repository).findByUserNameAndDesignatedBodyCode(USERNAME, DBC);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotFindRecordByUserNameAndDesignatedBodyCodeWhenNotExists() {
    when(repository.findByUserNameAndDesignatedBodyCode(USERNAME, DBC))
        .thenReturn(Optional.empty());

    Optional<UserDesignatedBody> found = service.findByUserNameAndDesignatedBodyCode(USERNAME, DBC);
    assertThat("Record not found.", found.isEmpty(), is(true));

    verify(repository).findByUserNameAndDesignatedBodyCode(USERNAME, DBC);
    verifyNoMoreInteractions(repository);
  }
}

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

  private UserDesignatedBodySyncService service;
  private UserDesignatedBodyRepository repository;

  private UserDesignatedBody userDesignatedBody;
  private UserDesignatedBody userDesignatedBody2;

  @BeforeEach
  void setUp() {
    repository = mock(UserDesignatedBodyRepository.class);

    service = new UserDesignatedBodySyncService(repository);

    userDesignatedBody = new UserDesignatedBody();
    userDesignatedBody.setTisId(ID);
    userDesignatedBody2 = new UserDesignatedBody();
    userDesignatedBody2.setTisId(ID_2);
  }

  @Test
  void shouldThrowExceptionIfRecordNotUserDesignatedBody() {
    Record theRecord = new Record();
    assertThrows(IllegalArgumentException.class, () -> service.syncRecord(theRecord));
  }

  @ParameterizedTest(name = "Should store records when operation is {0}.")
  @EnumSource(value = Operation.class, names = {"LOAD", "INSERT", "UPDATE"})
  void shouldStoreRecords(Operation operation) {
    userDesignatedBody.setOperation(operation);

    service.syncRecord(userDesignatedBody);

    verify(repository).save(userDesignatedBody);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldDeleteRecordFromStore() {
    userDesignatedBody.setOperation(DELETE);

    service.syncRecord(userDesignatedBody);

    verify(repository).deleteById(ID);
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
    when(repository.findByUserName(ID)).thenReturn(Set.of(userDesignatedBody, userDesignatedBody2));

    Set<UserDesignatedBody> found = service.findByUserName(ID);
    assertThat("Unexpected number of records.", found.size(), is(2));
    assertThat("Unexpected record.", found.contains(userDesignatedBody), is(true));
    assertThat("Unexpected record.", found.contains(userDesignatedBody2), is(true));

    verify(repository).findByUserName(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotFindByUserNameWhenNotExists() {
    when(repository.findByUserName(ID)).thenReturn(Set.of());

    Set<UserDesignatedBody> found = service.findByUserName(ID);
    assertThat("Record not found.", found.isEmpty(), is(true));

    verify(repository).findByUserName(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldFindByDbcWhenExists() {
    when(repository.findByDbc(ID)).thenReturn(Set.of(userDesignatedBody, userDesignatedBody2));

    Set<UserDesignatedBody> found = service.findByDbc(ID);
    assertThat("Unexpected number of records.", found.size(), is(2));
    assertThat("Unexpected record.", found.contains(userDesignatedBody), is(true));
    assertThat("Unexpected record.", found.contains(userDesignatedBody2), is(true));

    verify(repository).findByDbc(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotFindByDbcWhenNotExists() {
    when(repository.findByDbc(ID)).thenReturn(Set.of());

    Set<UserDesignatedBody> found = service.findByDbc(ID);
    assertThat("Record not found.", found.isEmpty(), is(true));

    verify(repository).findByDbc(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldFindRecordByUserNameAndDesignatedBodyCodeWhenExists() {
    when(repository.findByUserNameAndDesignatedBodyCode(ID, DBC))
        .thenReturn(Optional.of(userDesignatedBody));

    Optional<UserDesignatedBody> found = service.findByUserNameAndDesignatedBodyCode(ID, DBC);
    assertThat("Record not found.", found.isPresent(), is(true));
    assertThat("Unexpected record.", found.orElse(null), sameInstance(userDesignatedBody));

    verify(repository).findByUserNameAndDesignatedBodyCode(ID, DBC);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotFindRecordByUserNameAndDesignatedBodyCodeWhenNotExists() {
    when(repository.findByUserNameAndDesignatedBodyCode(ID, DBC))
        .thenReturn(Optional.empty());

    Optional<UserDesignatedBody> found = service.findByUserNameAndDesignatedBodyCode(ID, DBC);
    assertThat("Record not found.", found.isEmpty(), is(true));

    verify(repository).findByUserNameAndDesignatedBodyCode(ID, DBC);
    verifyNoMoreInteractions(repository);
  }
}

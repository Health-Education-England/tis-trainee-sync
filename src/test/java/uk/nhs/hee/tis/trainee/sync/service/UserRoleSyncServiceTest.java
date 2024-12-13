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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.model.UserRole;
import uk.nhs.hee.tis.trainee.sync.repository.UserRoleRepository;

class UserRoleSyncServiceTest {

  private static final String ID = "UserRoleId";
  private static final String USERNAME = "theUser";
  private static final String ROLENAME = "theRole";

  private UserRoleSyncService service;
  private UserRoleRepository repository;

  private UserRole userRole;

  @BeforeEach
  void setUp() {
    repository = mock(UserRoleRepository.class);

    service = new UserRoleSyncService(repository);

    userRole = new UserRole();
    userRole.setTisId(ID);
    userRole.setData(Map.of("userName", USERNAME, "roleName", ROLENAME));
  }

  @Test
  void shouldThrowExceptionIfRecordNotUserRole() {
    Record theRecord = new Record();
    assertThrows(IllegalArgumentException.class, () -> service.syncRecord(theRecord));
  }

  @ParameterizedTest(name = "Should store records when operation is {0}.")
  @EnumSource(value = Operation.class, names = {"LOAD", "INSERT", "UPDATE"})
  void shouldStoreRecords(Operation operation) {
    userRole.setOperation(operation);

    service.syncRecord(userRole);

    verify(repository).save(userRole);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldDeleteRecordFromStoreIfExists() {
    userRole.setOperation(DELETE);
    when(repository.findByUserNameAndRoleName(USERNAME, ROLENAME))
        .thenReturn(Optional.of(userRole));

    service.syncRecord(userRole);

    verify(repository).findByUserNameAndRoleName(USERNAME, ROLENAME);
    verify(repository).deleteById(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotDeleteRecordFromStoreIfNotExists() {
    userRole.setOperation(DELETE);
    when(repository.findByUserNameAndRoleName(USERNAME, ROLENAME))
        .thenReturn(Optional.empty());

    service.syncRecord(userRole);

    verify(repository).findByUserNameAndRoleName(USERNAME, ROLENAME);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldFindRecordByIdWhenExists() {
    when(repository.findById(ID)).thenReturn(Optional.of(userRole));

    Optional<UserRole> found = service.findById(ID);
    assertThat("Record not found.", found.isPresent(), is(true));
    assertThat("Unexpected record.", found.orElse(null), sameInstance(userRole));

    verify(repository).findById(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotFindRecordByIdWhenNotExists() {
    when(repository.findById(ID)).thenReturn(Optional.empty());

    Optional<UserRole> found = service.findById(ID);
    assertThat("Record not found.", found.isEmpty(), is(true));

    verify(repository).findById(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldFindRvOfficerRoleByUserNameWhenExists() {
    when(repository.findRvOfficerRoleByUserName(ID)).thenReturn(Optional.of(userRole));

    Optional<UserRole> found = service.findRvOfficerRoleByUserName(ID);
    assertThat("Record not found.", found.isPresent(), is(true));
    assertThat("Unexpected record.", found.orElse(null), sameInstance(userRole));

    verify(repository).findRvOfficerRoleByUserName(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotFindRvOfficerRoleByUserNameWhenNotExists() {
    when(repository.findRvOfficerRoleByUserName(ID)).thenReturn(Optional.empty());

    Optional<UserRole> found = service.findRvOfficerRoleByUserName(ID);
    assertThat("Record not found.", found.isEmpty(), is(true));

    verify(repository).findRvOfficerRoleByUserName(ID);
    verifyNoMoreInteractions(repository);
  }

}

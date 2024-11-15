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
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.nhs.hee.tis.trainee.sync.event.DbcEventListener.DBC_NAME;
import static uk.nhs.hee.tis.trainee.sync.event.UserDesignatedBodyEventListener.DESIGNATED_BODY_CODE;
import static uk.nhs.hee.tis.trainee.sync.event.UserRoleEventListener.ROLE_NAME;
import static uk.nhs.hee.tis.trainee.sync.event.UserRoleEventListener.USER_NAME;
import static uk.nhs.hee.tis.trainee.sync.model.Dbc.ENTITY_NAME;
import static uk.nhs.hee.tis.trainee.sync.model.Operation.DELETE;
import static uk.nhs.hee.tis.trainee.sync.model.Operation.LOAD;
import static uk.nhs.hee.tis.trainee.sync.model.Operation.LOOKUP;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import uk.nhs.hee.tis.trainee.sync.model.Dbc;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.model.UserDesignatedBody;
import uk.nhs.hee.tis.trainee.sync.model.UserRole;
import uk.nhs.hee.tis.trainee.sync.repository.DbcRepository;

class DbcSyncServiceTest {

  private static final String DBC = "dbc";
  private static final String DBC_2 = "dbc2";
  private static final String ABBR = "ABCDE";
  private static final String USERNAME = "some username";
  private static final String DBCODE1 = "some designated body code";
  private static final String DBCODE2 = "another designated body code";
  private static final String DBNAME1 = "some designated body";
  private static final String DBNAME2 = "another designated body";

  private DbcSyncService service;

  private DbcRepository repository;

  private DataRequestService dataRequestService;

  private ReferenceSyncService referenceSyncService;

  private UserRoleSyncService userRoleSyncService;

  private UserDesignatedBodySyncService udbSyncService;

  private RequestCacheService requestCacheService;

  private ApplicationEventPublisher eventPublisher;

  private Dbc dbc;

  private Map<String, String> whereMap;

  private Map<String, String> whereMap2;

  @BeforeEach
  void setUp() {
    repository = mock(DbcRepository.class);
    dataRequestService = mock(DataRequestService.class);
    referenceSyncService = mock(ReferenceSyncService.class);
    requestCacheService = mock(RequestCacheService.class);
    userRoleSyncService = mock(UserRoleSyncService.class);
    udbSyncService = mock(UserDesignatedBodySyncService.class);
    eventPublisher = mock(ApplicationEventPublisher.class);

    service = new DbcSyncService(repository, dataRequestService, referenceSyncService,
        userRoleSyncService, udbSyncService, requestCacheService, eventPublisher);

    dbc = new Dbc();
    dbc.setTisId(DBC);

    whereMap = Map.of("dbc", DBC);
    whereMap2 = Map.of("dbc", DBC_2);
  }

  @Test
  void shouldThrowExceptionIfRecordNotDbc() {
    Record recrd = new Record();
    assertThrows(IllegalArgumentException.class, () -> service.syncRecord(recrd));
  }

  @ParameterizedTest(name = "Should store records when operation is {0}.")
  @EnumSource(value = Operation.class, names = {"LOAD", "INSERT", "UPDATE"})
  void shouldStoreRecords(Operation operation) {
    dbc.setOperation(operation);

    service.syncRecord(dbc);

    verify(repository).save(dbc);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldRequestMissingDbcWhenOperationLookupAndDbcNotFound()
      throws JsonProcessingException {
    dbc.getData().put("designatedBodyCode", DBC);
    dbc.setOperation(LOOKUP);
    when(repository.findById(DBC)).thenReturn(Optional.empty());

    service.syncRecord(dbc);

    verify(repository).findById(DBC);
    verifyNoMoreInteractions(repository);

    verify(dataRequestService).sendRequest(Dbc.SCHEMA_NAME, ENTITY_NAME, whereMap);

    // The request is cached after it is sent, ensure it is not deleted straight away.
    verify(requestCacheService).addItemToCache(eq(ENTITY_NAME), eq(DBC), any());
    verify(requestCacheService, never()).deleteItemFromCache(any(), any());
  }

  @Test
  void shouldPublishSaveDbcEventWhenOperationLookupAndDbcFound() {
    dbc.setOperation(LOOKUP);

    Dbc lookupDbc = new Dbc();
    lookupDbc.setTisId(DBC);
    lookupDbc.setData(Map.of("dummy", "data"));
    when(repository.findById(DBC)).thenReturn(Optional.of(lookupDbc));

    service.syncRecord(dbc);

    verify(repository).findById(DBC);
    verifyNoMoreInteractions(repository);

    ArgumentCaptor<AfterSaveEvent<Dbc>> eventCaptor = ArgumentCaptor.captor();
    verify(eventPublisher).publishEvent(eventCaptor.capture());

    AfterSaveEvent<Dbc> event = eventCaptor.getValue();
    assertThat("Unexpected event source.", event.getSource(), sameInstance(lookupDbc));
    assertThat("Unexpected event collection.", event.getCollectionName(), is(ENTITY_NAME));
    assertThat("Unexpected event document.", event.getDocument(), nullValue());

    verify(requestCacheService).deleteItemFromCache(ENTITY_NAME, DBC);
    verifyNoMoreInteractions(requestCacheService);
  }

  @Test
  void shouldDeleteRecordFromStore() {
    dbc.setOperation(DELETE);

    service.syncRecord(dbc);

    verify(repository).deleteById(DBC);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldFindRecordByIdWhenExists() {
    when(repository.findById(DBC)).thenReturn(Optional.of(dbc));

    Optional<Dbc> found = service.findById(DBC);
    assertThat("Record not found.", found.isPresent(), is(true));
    assertThat("Unexpected record.", found.orElse(null), sameInstance(dbc));

    verify(repository).findById(DBC);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotFindRecordByIdWhenNotExists() {
    when(repository.findById(DBC)).thenReturn(Optional.empty());

    Optional<Dbc> found = service.findById(DBC);
    assertThat("Record not found.", found.isEmpty(), is(true));

    verify(repository).findById(DBC);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldFindRecordByDbcWhenExists() {
    when(repository.findByDbc(DBC)).thenReturn(Optional.of(dbc));

    Optional<Dbc> found = service.findByDbc(DBC);
    assertThat("Record not found.", found.isPresent(), is(true));
    assertThat("Unexpected record.", found.orElse(null), sameInstance(dbc));

    verify(repository).findByDbc(DBC);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotFindRecordByDbcWhenNotExists() {
    when(repository.findByDbc(DBC)).thenReturn(Optional.empty());

    Optional<Dbc> found = service.findByDbc(DBC);
    assertThat("Record not found.", found.isEmpty(), is(true));

    verify(repository).findByDbc(DBC);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldFindRecordByAbbrWhenExists() {
    when(repository.findByAbbr(ABBR)).thenReturn(Optional.of(dbc));

    Optional<Dbc> found = service.findByAbbr(ABBR);
    assertThat("Record not found.", found.isPresent(), is(true));
    assertThat("Unexpected record.", found.orElse(null), sameInstance(dbc));

    verify(repository).findByAbbr(ABBR);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotFindRecordByAbbrWhenNotExists() {
    when(repository.findByAbbr(ABBR)).thenReturn(Optional.empty());

    Optional<Dbc> found = service.findByAbbr(ABBR);
    assertThat("Record not found.", found.isEmpty(), is(true));

    verify(repository).findByAbbr(ABBR);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldSendRequestWhenNotAlreadyRequested() throws JsonProcessingException {
    when(requestCacheService.isItemInCache(Dbc.ENTITY_NAME, DBC)).thenReturn(false);
    service.requestByDbc(DBC);
    verify(dataRequestService).sendRequest(Dbc.SCHEMA_NAME, Dbc.ENTITY_NAME, whereMap);
  }

  @Test
  void shouldNotSendRequestWhenAlreadyRequested() throws JsonProcessingException {
    when(requestCacheService.isItemInCache(Dbc.ENTITY_NAME, DBC)).thenReturn(true);
    service.requestByDbc(DBC);
    verify(dataRequestService, never()).sendRequest(Dbc.SCHEMA_NAME, Dbc.ENTITY_NAME, whereMap);
    verifyNoMoreInteractions(dataRequestService);
  }

  @Test
  void shouldSendRequestWhenSyncedBetweenRequests() throws JsonProcessingException {
    when(requestCacheService.isItemInCache(Dbc.ENTITY_NAME, DBC)).thenReturn(false);
    service.requestByDbc(DBC);
    verify(requestCacheService).addItemToCache(eq(Dbc.ENTITY_NAME), eq(DBC), any());

    dbc.setOperation(DELETE);
    service.syncRecord(dbc);
    verify(requestCacheService).deleteItemFromCache(Dbc.ENTITY_NAME, DBC);

    service.requestByDbc(DBC);
    verify(dataRequestService, times(2)).sendRequest(Dbc.SCHEMA_NAME, Dbc.ENTITY_NAME, whereMap);
  }

  @Test
  void shouldSendRequestWhenRequestedDifferentIds() throws JsonProcessingException {
    service.requestByDbc(DBC);
    service.requestByDbc(DBC_2);

    verify(dataRequestService, atMostOnce())
        .sendRequest(Dbc.SCHEMA_NAME, Dbc.ENTITY_NAME, whereMap);
    verify(dataRequestService, atMostOnce())
        .sendRequest(Dbc.SCHEMA_NAME, Dbc.ENTITY_NAME, whereMap2);
  }

  @Test
  void shouldSendRequestWhenFirstRequestFails() throws JsonProcessingException {
    doThrow(JsonProcessingException.class).when(dataRequestService)
        .sendRequest(anyString(), anyString(), anyMap());

    service.requestByDbc(DBC);
    service.requestByDbc(DBC);

    verify(dataRequestService, times(2)).sendRequest(Dbc.SCHEMA_NAME, Dbc.ENTITY_NAME, whereMap);
  }

  @Test
  void shouldSendRequestByAbbrWhenNotAlreadyRequested() throws JsonProcessingException {
    when(requestCacheService.isItemInCache(Dbc.ENTITY_NAME, ABBR)).thenReturn(false);
    service.requestByAbbr(ABBR);
    Map<String, String> whereAbbrMap = Map.of("abbr", ABBR);
    verify(dataRequestService).sendRequest(Dbc.SCHEMA_NAME, Dbc.ENTITY_NAME, whereAbbrMap);
  }

  @Test
  void shouldCatchJsonProcessingExceptionIfThrown() throws JsonProcessingException {
    doThrow(JsonProcessingException.class).when(dataRequestService)
        .sendRequest(anyString(), anyString(), anyMap());
    assertDoesNotThrow(() -> service.requestByDbc(DBC));
  }

  @Test
  void shouldThrowAnExceptionIfNotJsonProcessingException() throws JsonProcessingException {
    IllegalStateException illegalStateException = new IllegalStateException("error");
    doThrow(illegalStateException).when(dataRequestService).sendRequest(anyString(), anyString(),
        anyMap());
    assertThrows(IllegalStateException.class, () -> service.requestByDbc(DBC));
    assertEquals("error", illegalStateException.getMessage());
  }

  @Test
  void shouldForwardRecordToReferenceSyncService() {
    dbc.setOperation(Operation.LOAD);
    service.syncRecord(dbc);

    verify(referenceSyncService).syncRecord(dbc);
  }

  @Test
  void shouldPublishDbcSaveEventsWhenResyncResponsibleOfficerUser() {
    UserRole userRole = new UserRole();
    userRole.getData().put(USER_NAME, USERNAME);
    userRole.getData().put(ROLE_NAME, "RVOfficer");

    when(userRoleSyncService.findRvOfficerRoleByUserName(USERNAME)).thenReturn(
        Optional.of(userRole));

    UserDesignatedBody udb1 = new UserDesignatedBody();
    udb1.getData().put(DESIGNATED_BODY_CODE, DBCODE1);
    UserDesignatedBody udb2 = new UserDesignatedBody();
    udb2.getData().put(DESIGNATED_BODY_CODE, DBCODE2);

    when(udbSyncService.findByUserName(USERNAME)).thenReturn(Set.of(udb1, udb2));

    Dbc dbc1 = new Dbc();
    dbc1.getData().put(DBC_NAME, DBNAME1);
    dbc1.setOperation(LOAD);
    Dbc dbc2 = new Dbc();
    dbc2.getData().put(DBC_NAME, DBNAME2);
    dbc2.setOperation(LOAD);

    when(repository.findByDbc(DBCODE1)).thenReturn(Optional.of(dbc1));
    when(repository.findByDbc(DBCODE2)).thenReturn(Optional.of(dbc2));

    service.resyncProgrammesIfUserIsResponsibleOfficer(USERNAME);

    ArgumentCaptor<AfterSaveEvent<Dbc>> eventCaptor = ArgumentCaptor.forClass(AfterSaveEvent.class);
    verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());

    List<AfterSaveEvent<Dbc>> events = eventCaptor.getAllValues();
    if (events.get(0).getSource().equals(dbc1)) {
      assertEquals(dbc1, events.get(0).getSource());
      assertNull(events.get(0).getDocument());
      assertEquals(Dbc.ENTITY_NAME, events.get(0).getCollectionName());
      assertEquals(dbc2, events.get(1).getSource());
      assertNull(events.get(1).getDocument());
      assertEquals(Dbc.ENTITY_NAME, events.get(1).getCollectionName());
    } else {
      assertEquals(dbc1, events.get(1).getSource());
      assertNull(events.get(1).getDocument());
      assertEquals(Dbc.ENTITY_NAME, events.get(1).getCollectionName());
      assertEquals(dbc2, events.get(0).getSource());
      assertNull(events.get(0).getDocument());
      assertEquals(Dbc.ENTITY_NAME, events.get(0).getCollectionName());
    }
  }

  @Test
  void shouldNotPublishDbcSaveEventsWhenDbcNotFound() {
    UserRole userRole = new UserRole();
    userRole.getData().put(USER_NAME, USERNAME);
    userRole.getData().put(ROLE_NAME, "RVOfficer");

    when(userRoleSyncService.findRvOfficerRoleByUserName(USERNAME)).thenReturn(
        Optional.of(userRole));

    UserDesignatedBody udb1 = new UserDesignatedBody();
    udb1.getData().put(DESIGNATED_BODY_CODE, DBCODE1);

    when(udbSyncService.findByUserName(USERNAME)).thenReturn(Set.of(udb1));

    when(repository.findByDbc(DBCODE1)).thenReturn(Optional.empty());

    service.resyncProgrammesIfUserIsResponsibleOfficer(USERNAME);

    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void shouldNotPublishDbcSaveEventsWhenResyncIfUserNotResponsibleOfficer() {
    when(userRoleSyncService.findRvOfficerRoleByUserName(USERNAME)).thenReturn(
        Optional.empty());

    service.resyncProgrammesIfUserIsResponsibleOfficer(USERNAME);

    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void shouldNotPublishDbcSaveEventsWhenResyncIfUserNull() {
    service.resyncProgrammesIfUserIsResponsibleOfficer(null);

    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void shouldPublishDbcSaveEventWhenResyncResponsibleOfficerUserSingleDbc() {
    UserRole userRole = new UserRole();
    userRole.getData().put(USER_NAME, USERNAME);
    userRole.getData().put(ROLE_NAME, "RVOfficer");

    when(userRoleSyncService.findRvOfficerRoleByUserName(USERNAME)).thenReturn(
        Optional.of(userRole));

    UserDesignatedBody udb1 = new UserDesignatedBody();
    udb1.getData().put(DESIGNATED_BODY_CODE, DBCODE1);

    when(udbSyncService.findByUserNameAndDesignatedBodyCode(USERNAME, DBCODE1))
        .thenReturn(Optional.of(udb1));

    Dbc dbc1 = new Dbc();
    dbc1.getData().put(DBC_NAME, DBNAME1);
    dbc1.setOperation(LOAD);

    when(repository.findByDbc(DBCODE1)).thenReturn(Optional.of(dbc1));

    service.resyncProgrammesForSingleDbcIfUserIsResponsibleOfficer(USERNAME, DBCODE1);

    ArgumentCaptor<AfterSaveEvent<Dbc>> eventCaptor = ArgumentCaptor.forClass(AfterSaveEvent.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());

    AfterSaveEvent<Dbc> event = eventCaptor.getValue();

    assertEquals(dbc1, event.getSource());
    assertNull(event.getDocument());
    assertEquals(Dbc.ENTITY_NAME, event.getCollectionName());
  }

  @Test
  void shouldNotPublishDbcSaveEventWhenSingleDbcNotFound() {
    UserRole userRole = new UserRole();
    userRole.getData().put(USER_NAME, USERNAME);
    userRole.getData().put(ROLE_NAME, "RVOfficer");

    when(userRoleSyncService.findRvOfficerRoleByUserName(USERNAME)).thenReturn(
        Optional.of(userRole));

    when(udbSyncService.findByUserNameAndDesignatedBodyCode(USERNAME, DBCODE1))
        .thenReturn(Optional.empty());

    service.resyncProgrammesForSingleDbcIfUserIsResponsibleOfficer(USERNAME, DBCODE1);

    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void shouldNotPublishDbcSaveEventWhenSingleDbcResyncIfUserNotResponsibleOfficer() {
    when(userRoleSyncService.findRvOfficerRoleByUserName(USERNAME)).thenReturn(
        Optional.empty());

    service.resyncProgrammesForSingleDbcIfUserIsResponsibleOfficer(USERNAME, DBCODE1);

    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void shouldNotPublishDbcSaveEventWhenSingleDbcResyncIfUserNull() {
    service.resyncProgrammesForSingleDbcIfUserIsResponsibleOfficer(null, DBCODE1);

    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void shouldNotPublishDbcSaveEventWhenSingleDbcResyncIfDbCodeNull() {
    service.resyncProgrammesForSingleDbcIfUserIsResponsibleOfficer(USERNAME, null);

    verify(eventPublisher, never()).publishEvent(any());
  }
}

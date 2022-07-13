package uk.nhs.hee.tis.trainee.sync.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.nhs.hee.tis.trainee.sync.model.Operation.DELETE;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.model.Specialty;
import uk.nhs.hee.tis.trainee.sync.repository.SpecialtyRepository;

class SpecialtySyncServiceTest {

  private static final String ID = "40";
  private static final String ID_2 = "140";

  private SpecialtySyncService service;

  private SpecialtyRepository repository;

  private DataRequestService dataRequestService;

  private RequestCacheService requestCacheService;

  private Specialty specialty;

  private Map<String, String> whereMap;

  private Map<String, String> whereMap2;

  @BeforeEach
  void setUp() {
    repository = mock(SpecialtyRepository.class);
    dataRequestService = mock(DataRequestService.class);
    requestCacheService = mock(RequestCacheService.class);

    service = new SpecialtySyncService(repository, dataRequestService, requestCacheService);

    specialty = new Specialty();
    specialty.setTisId(ID);

    whereMap = Map.of("id", ID);
    whereMap2 = Map.of("id", ID_2);
  }

  @Test
  void shouldThrowExceptionIfRecordNotSpecialty() {
    Record recrd = new Record();
    assertThrows(IllegalArgumentException.class, () -> service.syncRecord(recrd));
  }

  @ParameterizedTest(name = "Should store records when operation is {0}.")
  @EnumSource(value = Operation.class, names = {"LOAD", "INSERT", "UPDATE"})
  void shouldStoreRecords(Operation operation) {
    specialty.setOperation(operation);

    service.syncRecord(specialty);

    verify(repository).save(specialty);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldDeleteRecordFromStore() {
    specialty.setOperation(DELETE);

    service.syncRecord(specialty);

    verify(repository).deleteById(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldFindRecordByIdWhenExists() {
    when(repository.findById(ID)).thenReturn(Optional.of(specialty));

    Optional<Specialty> found = service.findById(ID);
    assertThat("Record not found.", found.isPresent(), is(true));
    assertThat("Unexpected record.", found.orElse(null), sameInstance(specialty));

    verify(repository).findById(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldNotFindRecordByIdWhenNotExists() {
    when(repository.findById(ID)).thenReturn(Optional.empty());

    Optional<Specialty> found = service.findById(ID);
    assertThat("Record not found.", found.isEmpty(), is(true));

    verify(repository).findById(ID);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldSendRequestWhenNotAlreadyRequested() throws JsonProcessingException {
    when(requestCacheService.isItemInCache(any())).thenReturn(false);
    service.request(ID);
    verify(dataRequestService).sendRequest("Specialty", whereMap);
  }

  @Test
  void shouldNotSendRequestWhenAlreadyRequested() throws JsonProcessingException {
    when(requestCacheService.isItemInCache(any())).thenReturn(true);
    service.request(ID);
    verify(dataRequestService, never()).sendRequest("Specialty", whereMap);
    verifyNoMoreInteractions(dataRequestService);
  }

  @Test
  void shouldSendRequestWhenSyncedBetweenRequests() throws JsonProcessingException {
    when(requestCacheService.isItemInCache(any())).thenReturn(false);
    service.request(ID);
    verify(requestCacheService).addItemToCache(ID);

    specialty.setOperation(DELETE);
    service.syncRecord(specialty);
    verify(requestCacheService).deleteItemFromCache(ID);

    service.request(ID);
    verify(dataRequestService, times(2)).sendRequest("Specialty", whereMap);
  }

  @Test
  void shouldSendRequestWhenRequestedDifferentIds() throws JsonProcessingException {
    when(requestCacheService.isItemInCache(any())).thenReturn(false);
    service.request(ID);
    service.request(ID_2);
    verify(dataRequestService, atMostOnce()).sendRequest("Specialty", whereMap);
    verify(dataRequestService, atMostOnce()).sendRequest("Specialty", whereMap2);
  }

  @Test
  void shouldSendRequestWhenFirstRequestFails() throws JsonProcessingException {
    doThrow(JsonProcessingException.class).when(dataRequestService)
        .sendRequest(anyString(), anyMap());

    service.request(ID);
    service.request(ID);

    verify(dataRequestService, times(2)).sendRequest("Specialty", whereMap);
  }

  @Test
  void shouldCatchJsonProcessingExceptionIfThrown() throws JsonProcessingException {
    doThrow(JsonProcessingException.class).when(dataRequestService)
        .sendRequest(anyString(), anyMap());
    assertDoesNotThrow(() -> service.request(ID));
  }

  @Test
  void shouldThrowAnExceptionIfNotJsonProcessingException() throws JsonProcessingException {
    IllegalStateException illegalStateException = new IllegalStateException("error");
    doThrow(illegalStateException).when(dataRequestService).sendRequest(anyString(),
        anyMap());
    assertThrows(IllegalStateException.class, () -> service.request(ID));
    assertEquals("error", illegalStateException.getMessage());
  }
}

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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.nhs.hee.tis.trainee.sync.model.Placement;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.repository.PlacementRepository;

class PlacementSyncServiceTest {

  private PlacementSyncService service;

  private PlacementRepository repository;

  @BeforeEach
  void setUp() {
    repository = mock(PlacementRepository.class);
    service = new PlacementSyncService(repository);
  }

  @Test
  void shouldThrowExceptionIfRecordNotPlacement() {
    Record record = new Record();
    assertThrows(IllegalArgumentException.class, () -> service.syncRecord(record));
  }

  @ParameterizedTest(
      name = "Should store placements when operation is {0} and table is Placement")
  @ValueSource(strings = {"load", "insert", "update"})
  void shouldStorePlacements(String operation) {
    LocalDate now = LocalDate.now();

    Map<String, String> data = Map.of(
        "traineeId", "traineeIdValue",
        "dateFrom", now.toString(),
        "dateTo", now.plusYears(1).toString(),
        "gradeAbbreviation", "gradeValue",
        "placementType", "placementTypeValue",
        "status", "statusValue");

    Placement record = new Placement();
    record.setTisId("idValue");
    record.setTable("Placement");
    record.setOperation(operation);
    record.setData(data);

    service.syncRecord(record);

    verify(repository).save(record);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldDeletePlacementFromStore() {
    LocalDate now = LocalDate.now();

    Map<String, String> data = Map.of(
        "traineeId", "traineeIdValue",
        "dateFrom", now.toString(),
        "dateTo", now.plusYears(1).toString(),
        "gradeAbbreviation", "gradeValue",
        "placementType", "placementTypeValue",
        "status", "statusValue");

    Placement record = new Placement();
    record.setTisId("idValue");
    record.setTable("Placement");
    record.setOperation("delete");
    record.setData(data);

    service.syncRecord(record);

    verify(repository).deleteById("idValue");
    verifyNoMoreInteractions(repository);
  }
}

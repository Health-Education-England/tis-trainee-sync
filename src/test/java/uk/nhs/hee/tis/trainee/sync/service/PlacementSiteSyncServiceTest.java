/*
 * The MIT License (MIT)
 *
 * Copyright 2023 Crown Copyright (Health Education England)
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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.nhs.hee.tis.trainee.sync.model.Operation.DELETE;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import uk.nhs.hee.tis.trainee.sync.mapper.PlacementSiteMapper;
import uk.nhs.hee.tis.trainee.sync.mapper.PlacementSiteMapperImpl;
import uk.nhs.hee.tis.trainee.sync.model.Operation;
import uk.nhs.hee.tis.trainee.sync.model.PlacementSite;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.repository.PlacementSiteRepository;

class PlacementSiteSyncServiceTest {

  private static final Long ID = 1L;
  private static final Long PLACEMENT_ID = 2L;
  private static final Long SITE_ID = 3L;
  private static final String TYPE = "OTHER";

  private PlacementSiteSyncService service;
  private PlacementSiteRepository repository;

  @BeforeEach
  void setUp() {
    repository = mock(PlacementSiteRepository.class);
    PlacementSiteMapper mapper = new PlacementSiteMapperImpl();
    service = new PlacementSiteSyncService(repository, mapper);
  }

  @Test
  void shouldThrowExceptionIfRecordNotSite() {
    Record recrd = new Record();
    assertThrows(IllegalArgumentException.class, () -> service.syncRecord(recrd));
  }

  @ParameterizedTest(name = "Should store records when operation is {0}.")
  @EnumSource(value = Operation.class, names = {"LOAD", "INSERT", "UPDATE"})
  void shouldStoreRecords(Operation operation) {
    Record placementSiteRecord = new Record();
    placementSiteRecord.setOperation(operation);
    placementSiteRecord.setTable(PlacementSite.ENTITY_NAME);
    placementSiteRecord.setData(Map.of(
        "id", ID.toString(),
        "placementId", PLACEMENT_ID.toString(),
        "siteId", SITE_ID.toString(),
        "placementSiteType", TYPE
    ));

    service.syncRecord(placementSiteRecord);

    ArgumentCaptor<PlacementSite> captor = ArgumentCaptor.forClass(PlacementSite.class);
    verify(repository).save(captor.capture());

    PlacementSite placementSite = captor.getValue();
    assertThat("Unexpected ID.", placementSite.getId(), is(ID));
    assertThat("Unexpected placement ID.", placementSite.getPlacementId(), is(PLACEMENT_ID));
    assertThat("Unexpected site ID.", placementSite.getSiteId(), is(SITE_ID));
    assertThat("Unexpected site type.", placementSite.getPlacementSiteType(), is(TYPE));

    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldDeleteRecordFromStore() {
    Record placementSiteRecord = new Record();
    placementSiteRecord.setOperation(DELETE);
    placementSiteRecord.setTable(PlacementSite.ENTITY_NAME);
    placementSiteRecord.setData(Map.of(
        "id", ID.toString()
    ));

    service.syncRecord(placementSiteRecord);

    verify(repository).deleteById(ID);
    verifyNoMoreInteractions(repository);
  }
}

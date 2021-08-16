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

package uk.nhs.hee.tis.trainee.sync.event;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import uk.nhs.hee.tis.trainee.sync.facade.PlacementEnricherFacade;
import uk.nhs.hee.tis.trainee.sync.model.PlacementSpecialty;

class PlacementSpecialtyEventListenerTest {

  private PlacementSpecialtyEventListener listener;
  private PlacementEnricherFacade enricher;

  @BeforeEach
  void setUp() {
    enricher = mock(PlacementEnricherFacade.class);
    listener = new PlacementSpecialtyEventListener(enricher);
  }

  @Test
  void shouldCallEnricherAfterSave() {
    PlacementSpecialty placementSpecialty = new PlacementSpecialty();
    AfterSaveEvent<PlacementSpecialty> event = new AfterSaveEvent<>(placementSpecialty, null, null);

    listener.onAfterSave(event);

    verify(enricher).enrich(placementSpecialty);
    verifyNoMoreInteractions(enricher);
  }

  @Test
  void shouldRestartPlacementEnrichmentIfDeletionIncorrect() {
    Document document = new Document();
    document.append("_id", "40");
    AfterDeleteEvent<PlacementSpecialty> event = new AfterDeleteEvent<>(document, null, null);

    listener.onAfterDelete(event);
    verify(enricher).restartPlacementEnrichmentIfDeletionIncorrect("40");
    verifyNoMoreInteractions(enricher);
  }


}

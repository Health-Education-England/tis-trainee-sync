package uk.nhs.hee.tis.trainee.sync.event;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import uk.nhs.hee.tis.trainee.sync.facade.PlacementEnricherFacade;
import uk.nhs.hee.tis.trainee.sync.model.Specialty;

class SpecialtyEventListenerTest {

  private SpecialtyEventListener listener;
  private PlacementEnricherFacade enricher;

  @BeforeEach
  void setUp() {
    enricher = mock(PlacementEnricherFacade.class);
    listener = new SpecialtyEventListener(enricher);
  }

  @Test
  void shouldCallEnricherAfterSave() {
    Specialty specialty = new Specialty();
    AfterSaveEvent<Specialty> event = new AfterSaveEvent<>(specialty, null, null);

    listener.onAfterSave(event);

    verify(enricher).enrich(specialty);
    verifyNoMoreInteractions(enricher);
  }
}

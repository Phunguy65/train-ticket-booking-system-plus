package io.github.phunguy65.ttbs.backend.station.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.station.application.command.BulkSoftDeleteStationsCommand;
import io.github.phunguy65.ttbs.backend.station.domain.errors.StationError;
import io.github.phunguy65.ttbs.backend.station.domain.event.StationsDeleted;
import io.github.phunguy65.ttbs.backend.station.domain.model.StationId;
import io.github.phunguy65.ttbs.backend.station.domain.repository.StationRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class BulkSoftDeleteStationsUseCaseTest {

    @Mock
    private StationRepository stationRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private BulkSoftDeleteStationsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new BulkSoftDeleteStationsUseCase(stationRepository, eventPublisher);
    }

    @Test
    void execute_shouldEmitSingleStationsDeletedEvent_notNIndividualEvents() {
        StationId id1 = StationId.of(UUID.randomUUID());
        StationId id2 = StationId.of(UUID.randomUUID());
        StationId id3 = StationId.of(UUID.randomUUID());
        List<StationId> ids = List.of(id1, id2, id3);

        when(stationRepository.softDeleteByIds(eq(ids), any(Instant.class))).thenReturn(3);

        Result<Integer, StationError> result =
                useCase.execute(new BulkSoftDeleteStationsCommand(ids));

        assertThat(result.isSuccess()).isTrue();
        assertThat(((Result.Success<Integer, StationError>) result).value()).isEqualTo(3);

        // Exactly ONE event, not 3
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(StationsDeleted.class);
        StationsDeleted event = (StationsDeleted) captor.getValue();
        assertThat(event.stationIds()).containsExactlyInAnyOrderElementsOf(ids);
    }

    @Test
    void execute_whenNothingAffected_shouldNotPublishEvent() {
        StationId id1 = StationId.of(UUID.randomUUID());
        List<StationId> ids = List.of(id1);

        when(stationRepository.softDeleteByIds(any(), any())).thenReturn(0);

        useCase.execute(new BulkSoftDeleteStationsCommand(ids));

        verifyNoInteractions(eventPublisher);
    }
}

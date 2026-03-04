package io.github.phunguy65.ttbs.backend.train.application.listener;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.train.domain.event.CoachesDeleted;
import io.github.phunguy65.ttbs.backend.train.domain.event.SeatsDeleted;
import io.github.phunguy65.ttbs.backend.train.domain.model.CoachId;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteSeatAvailabilityRepository;
import io.github.phunguy65.ttbs.backend.train.domain.repository.SeatRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class CascadeOnCoachesDeletedListenerTest {

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private RouteSeatAvailabilityRepository availabilityRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private CascadeOnCoachesDeletedListener listener;

    @BeforeEach
    void setUp() {
        listener = new CascadeOnCoachesDeletedListener(
                seatRepository, availabilityRepository, eventPublisher);
    }

    @Test
    void onCoachesDeleted_withActiveSeats_shouldHardDeleteRsaThenSoftDeleteSeatsAndPublish() {
        CoachId coachId = CoachId.of(UUID.randomUUID());
        SeatId seatId1 = SeatId.of(UUID.randomUUID());
        SeatId seatId2 = SeatId.of(UUID.randomUUID());
        List<SeatId> seatIds = List.of(seatId1, seatId2);

        CoachesDeleted event = CoachesDeleted.of(List.of(coachId), Instant.now());
        when(seatRepository.findActiveIdsByCoachIds(event.coachIds())).thenReturn(seatIds);

        listener.onCoachesDeleted(event);

        // RSA hard delete must happen before seat soft delete
        InOrder inOrder = inOrder(availabilityRepository, seatRepository);
        inOrder.verify(availabilityRepository).hardDeleteBySeatIds(seatIds);
        inOrder.verify(seatRepository).softDeleteByIds(eq(seatIds), any(Instant.class));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(SeatsDeleted.class);
        SeatsDeleted published = (SeatsDeleted) captor.getValue();
        assertThat(published.seatIds()).containsExactlyInAnyOrderElementsOf(seatIds);
    }

    @Test
    void onCoachesDeleted_withNoActiveSeats_shouldNotDeleteOrPublish() {
        CoachId coachId = CoachId.of(UUID.randomUUID());
        CoachesDeleted event = CoachesDeleted.of(List.of(coachId), Instant.now());
        when(seatRepository.findActiveIdsByCoachIds(any())).thenReturn(List.of());

        listener.onCoachesDeleted(event);

        verifyNoInteractions(availabilityRepository);
        verify(seatRepository, never()).softDeleteByIds(any(), any());
        verifyNoInteractions(eventPublisher);
    }
}

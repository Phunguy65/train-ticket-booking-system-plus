package io.github.phunguy65.ttbs.backend.train.application.sse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.shared.domain.event.SeatStatusChangedEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@ExtendWith(MockitoExtension.class)
class SeatEventListenerTest {

    @Mock
    private SeatEventBroadcaster broadcaster;

    private SeatEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new SeatEventListener(broadcaster);
    }

    @Test
    @DisplayName("onSeatStatusChanged() delegates to broadcaster.broadcast()")
    void onSeatStatusChanged_delegatesToBroadcaster() {
        UUID tripId = UUID.randomUUID();
        UUID seatId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        SeatStatusChangedEvent event = new SeatStatusChangedEvent(
                tripId,
                List.of(new SeatStatusChangedEvent.SeatChange(seatId, "HELD", bookingId)),
                Instant.now());

        listener.onSeatStatusChanged(event);

        verify(broadcaster).broadcast(tripId, event);
    }

    @Test
    @DisplayName("onSeatStatusChanged() passes correct event with multiple seat changes")
    void onSeatStatusChanged_multipleSeats() {
        UUID tripId = UUID.randomUUID();

        SeatStatusChangedEvent event = new SeatStatusChangedEvent(
                tripId,
                List.of(
                        new SeatStatusChangedEvent.SeatChange(
                                UUID.randomUUID(), "HELD", UUID.randomUUID()),
                        new SeatStatusChangedEvent.SeatChange(
                                UUID.randomUUID(), "HELD", UUID.randomUUID()),
                        new SeatStatusChangedEvent.SeatChange(
                                UUID.randomUUID(), "BOOKED", UUID.randomUUID())),
                Instant.now());

        listener.onSeatStatusChanged(event);

        ArgumentCaptor<SeatStatusChangedEvent> captor =
                ArgumentCaptor.forClass(SeatStatusChangedEvent.class);
        verify(broadcaster).broadcast(eq(tripId), captor.capture());

        SeatStatusChangedEvent captured = captor.getValue();
        assertEquals(3, captured.seats().size());
        assertEquals("HELD", captured.seats().get(0).status());
        assertEquals("BOOKED", captured.seats().get(2).status());
    }

    @Test
    @DisplayName("onSeatStatusChanged() handles empty seats list")
    void onSeatStatusChanged_emptySeats() {
        UUID tripId = UUID.randomUUID();

        SeatStatusChangedEvent event = new SeatStatusChangedEvent(tripId, List.of(), Instant.now());

        listener.onSeatStatusChanged(event);

        verify(broadcaster).broadcast(tripId, event);
    }

    @Test
    @DisplayName("@TransactionalEventListener annotation is present with AFTER_COMMIT phase")
    void transactionalEventListenerAnnotationPresent() throws NoSuchMethodException {
        var method = SeatEventListener.class.getMethod(
                "onSeatStatusChanged", SeatStatusChangedEvent.class);
        var annotation = method.getAnnotation(TransactionalEventListener.class);

        assertNotNull(
                annotation, "@TransactionalEventListener should be present on onSeatStatusChanged");
        assertEquals(
                TransactionPhase.AFTER_COMMIT, annotation.phase(), "Phase should be AFTER_COMMIT");
    }

    @Test
    @DisplayName("listener handles event with AVAILABLE status (released seats)")
    void onSeatStatusChanged_availableStatus() {
        UUID tripId = UUID.randomUUID();

        SeatStatusChangedEvent event = new SeatStatusChangedEvent(
                tripId,
                List.of(new SeatStatusChangedEvent.SeatChange(
                        UUID.randomUUID(), "AVAILABLE", null)),
                Instant.now());

        listener.onSeatStatusChanged(event);

        verify(broadcaster).broadcast(tripId, event);
    }

    @Test
    @DisplayName("listener passes event with CANCELLED status")
    void onSeatStatusChanged_cancelledStatus() {
        UUID tripId = UUID.randomUUID();

        SeatStatusChangedEvent event = new SeatStatusChangedEvent(
                tripId,
                List.of(new SeatStatusChangedEvent.SeatChange(
                        UUID.randomUUID(), "CANCELLED", null)),
                Instant.now());

        listener.onSeatStatusChanged(event);

        verify(broadcaster).broadcast(tripId, event);
    }
}

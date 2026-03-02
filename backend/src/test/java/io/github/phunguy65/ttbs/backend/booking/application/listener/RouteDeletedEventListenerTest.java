package io.github.phunguy65.ttbs.backend.booking.application.listener;

import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import io.github.phunguy65.ttbs.backend.train.domain.event.RouteDeleted;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RouteDeletedEventListenerTest {

    @Mock
    private BookingRepository bookingRepository;

    private RouteDeletedEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new RouteDeletedEventListener(bookingRepository);
    }

    @Test
    void onRouteDeleted_shouldCallSoftDeleteByRouteId() {
        RouteId routeId = RouteId.of(UUID.randomUUID());
        Instant occurredAt = Instant.now();
        RouteDeleted event = new RouteDeleted(routeId, occurredAt);

        listener.onRouteDeleted(event);

        verify(bookingRepository).softDeleteByRouteId(routeId, occurredAt);
    }

    @Test
    void onRouteDeleted_shouldPassCorrectTimestamp() {
        RouteId routeId = RouteId.of(UUID.randomUUID());
        Instant specificTime = Instant.parse("2025-06-01T10:00:00Z");
        RouteDeleted event = new RouteDeleted(routeId, specificTime);

        listener.onRouteDeleted(event);

        verify(bookingRepository).softDeleteByRouteId(routeId, specificTime);
        verifyNoMoreInteractions(bookingRepository);
    }
}

package io.github.phunguy65.ttbs.backend.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.github.phunguy65.ttbs.backend.booking.application.command.CreateSeatHoldCommand;
import io.github.phunguy65.ttbs.backend.booking.application.usecase.CreateSeatHoldUseCase;
import io.github.phunguy65.ttbs.backend.booking.domain.event.SeatHoldCreated;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.port.RoutePort;
import io.github.phunguy65.ttbs.backend.train.application.port.RouteSeatAvailabilityPort;
import io.github.phunguy65.ttbs.backend.train.application.port.SeatPort;
import io.github.phunguy65.ttbs.backend.train.domain.model.Route;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.PublishedEvents;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ApplicationModuleTest
class BookingModuleTest {

    @MockitoBean
    private RouteSeatAvailabilityPort routeSeatAvailabilityPort;

    @MockitoBean
    private RoutePort routePort;

    @MockitoBean
    private SeatPort seatPort;

    @Autowired
    private CreateSeatHoldUseCase createSeatHoldUseCase;

    @Test
    void bookingModule_isStructurallyValid() {
        // Spring Modulith verifies module structure upon context loading.
        // If this test starts successfully, the module boundaries are valid.
    }

    @Test
    void createSeatHold_publishesSeatHoldCreatedEvent(PublishedEvents events) {
        UUID userId = UUID.randomUUID();
        UUID routeId = UUID.randomUUID();
        UUID seatId = UUID.randomUUID();
        String key = "module-test-key-" + UUID.randomUUID();

        CreateSeatHoldCommand command = new CreateSeatHoldCommand(
                userId, routeId, List.of(seatId), key, "Nguyen Van A", "a@example.com", null);

        // Mock route
        Route mockRoute = org.mockito.Mockito.mock(Route.class);
        org.mockito.Mockito.when(mockRoute.getBasePrice())
                .thenReturn(java.math.BigDecimal.valueOf(100_000));
        when(routePort.findById(RouteId.of(routeId))).thenReturn(Optional.of(mockRoute));

        // Mock seat
        io.github.phunguy65.ttbs.backend.train.domain.model.Seat mockSeat =
                org.mockito.Mockito.mock(
                        io.github.phunguy65.ttbs.backend.train.domain.model.Seat.class);
        org.mockito.Mockito.when(mockSeat.getId()).thenReturn(SeatId.of(seatId));
        when(seatPort.findById(SeatId.of(seatId))).thenReturn(Optional.of(mockSeat));

        // Mock seat availability port — successful hold
        when(routeSeatAvailabilityPort.holdSeats(any(), any())).thenReturn(Result.success(null));

        createSeatHoldUseCase.execute(command);

        var seatHoldCreatedEvents = events.ofType(SeatHoldCreated.class);

        assertThat(seatHoldCreatedEvents)
                .as("Expected SeatHoldCreated event to be published")
                .hasSize(1);
        assertThat(seatHoldCreatedEvents.iterator().next().userId()).isEqualTo(userId);
    }
}

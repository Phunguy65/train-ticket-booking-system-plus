package io.github.phunguy65.ttbs.backend.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.github.phunguy65.ttbs.backend.booking.application.command.CreateBookingCommand;
import io.github.phunguy65.ttbs.backend.booking.application.usecase.CreateBookingUseCase;
import io.github.phunguy65.ttbs.backend.booking.domain.event.BookingCreated;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.port.RouteSeatAvailabilityPort;
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

    @Autowired
    private CreateBookingUseCase createBookingUseCase;

    @Test
    void bookingModule_isStructurallyValid() {
        // Spring Modulith verifies module structure upon context loading.
        // If this test starts successfully, the module boundaries are valid.
    }

    @Test
    void createBooking_publishesBookingCreatedEvent(PublishedEvents events) {
        UUID userId = UUID.randomUUID();
        UUID routeId = UUID.randomUUID();
        UUID seatId = UUID.randomUUID();
        String key = "module-test-key-" + UUID.randomUUID();
        CreateBookingCommand command = new CreateBookingCommand(userId, routeId, seatId, key);

        when(routeSeatAvailabilityPort.reserveSeat(any(), any())).thenReturn(Result.success(null));

        createBookingUseCase.execute(command);

        // PublishedEvents captures all events published via ApplicationEventPublisher during this
        // test
        var allEvents = events.ofType(Object.class);
        var bookingCreatedEvents = events.ofType(BookingCreated.class);

        assertThat(bookingCreatedEvents)
                .as("Expected BookingCreated event to be published. All events: " + allEvents)
                .hasSize(1);
        assertThat(bookingCreatedEvents.iterator().next().userId()).isEqualTo(userId);
    }
}

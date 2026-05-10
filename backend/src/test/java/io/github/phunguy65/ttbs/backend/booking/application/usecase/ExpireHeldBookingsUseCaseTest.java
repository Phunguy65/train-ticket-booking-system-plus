package io.github.phunguy65.ttbs.backend.booking.application.usecase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingStatus;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingUserInfo;
import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import io.github.phunguy65.ttbs.backend.shared.domain.Money;
import io.github.phunguy65.ttbs.backend.train.application.port.RouteSeatAvailabilityManager;
import io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripId;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExpireHeldBookingsUseCase")
class ExpireHeldBookingsUseCaseTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private RouteSeatAvailabilityManager seatAvailabilityPort;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ExpireHeldBookingsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ExpireHeldBookingsUseCase(
                bookingRepository, seatAvailabilityPort, eventPublisher);
    }

    private Booking heldBooking(UUID bookingUuid, UUID tripUuid) {
        return Booking.reconstitute(
                BookingId.of(bookingUuid),
                UserId.of(UUID.randomUUID()),
                ScheduledTripId.of(tripUuid),
                BookingUserInfo.of("Nguyen Van A", "a@b.com", null, null, null, null, null),
                List.of(),
                Money.vnd(500_000L),
                BookingStatus.HELD,
                "idem-key-" + bookingUuid,
                Instant.now().minusSeconds(60),
                Instant.now().minusSeconds(120));
    }

    @Nested
    @DisplayName("happy path — multiple expired bookings")
    class HappyPath {

        @Test
        @DisplayName(
                "expires multiple bookings, releases seats for each, saves all and publishes events")
        void execute_expireMultipleBookings_releasesSeatsAndPublishesEvents() {
            UUID booking1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
            UUID booking2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
            UUID trip1 = UUID.fromString("33333333-3333-3333-3333-333333333333");
            UUID trip2 = UUID.fromString("44444444-4444-4444-4444-444444444444");

            Booking b1 = heldBooking(booking1, trip1);
            Booking b2 = heldBooking(booking2, trip2);

            when(bookingRepository.findExpiredHeldBookings(any())).thenReturn(List.of(b1, b2));
            when(seatAvailabilityPort.findSeatIdsByBookingId(booking1))
                    .thenReturn(List.of(SeatId.of(UUID.randomUUID())));
            when(seatAvailabilityPort.findSeatIdsByBookingId(booking2))
                    .thenReturn(List.of(SeatId.of(UUID.randomUUID())));
            when(seatAvailabilityPort.findByScheduledTripIdAndSeatIds(any(), any()))
                    .thenReturn(List.of());

            useCase.execute();

            verify(bookingRepository).saveAll(anyList());
            verify(seatAvailabilityPort, times(2)).releaseHeldSeats(any(), any());
            verify(eventPublisher, times(4)).publishEvent(any(Object.class));
        }
    }

    @Nested
    @DisplayName("no-op — no expired bookings")
    class NoExpired {

        @Test
        @DisplayName("no-op when no expired bookings")
        void execute_noOp_whenNoExpiredBookings() {
            when(bookingRepository.findExpiredHeldBookings(any())).thenReturn(List.of());

            useCase.execute();

            verify(bookingRepository, never()).saveAll(any());
            verify(seatAvailabilityPort, never()).releaseHeldSeats(any(), any());
            verify(eventPublisher, never()).publishEvent(any(Object.class));
        }
    }

    @Nested
    @DisplayName("skips bookings that fail to cancel")
    class SkipFailures {

        @Test
        @DisplayName("skips already-CANCELLED booking and continues processing the next one")
        void execute_skipsAlreadyCancelledBookingAndContinues() {
            UUID booking1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
            UUID booking2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
            UUID trip1 = UUID.fromString("33333333-3333-3333-3333-333333333333");
            UUID trip2 = UUID.fromString("44444444-4444-4444-4444-444444444444");

            Booking alreadyCancelled = Booking.reconstitute(
                    BookingId.of(booking1),
                    UserId.of(UUID.randomUUID()),
                    ScheduledTripId.of(trip1),
                    BookingUserInfo.of("Nguyen Van A", "a@b.com", null, null, null, null, null),
                    List.of(),
                    Money.vnd(500_000L),
                    BookingStatus.CANCELLED,
                    "idem-key-1",
                    Instant.now().minusSeconds(60),
                    Instant.now().minusSeconds(120));
            Booking validHeld = heldBooking(booking2, trip2);

            when(bookingRepository.findExpiredHeldBookings(any()))
                    .thenReturn(List.of(alreadyCancelled, validHeld));
            when(seatAvailabilityPort.findSeatIdsByBookingId(booking2))
                    .thenReturn(List.of(SeatId.of(UUID.randomUUID())));
            when(seatAvailabilityPort.findByScheduledTripIdAndSeatIds(any(), any()))
                    .thenReturn(List.of());

            useCase.execute();

            verify(bookingRepository).saveAll(anyList());
            verify(seatAvailabilityPort, times(1)).releaseHeldSeats(any(), any());
        }
    }
}

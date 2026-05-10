package io.github.phunguy65.ttbs.backend.booking.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.phunguy65.ttbs.backend.booking.application.command.CancelBookingCommand;
import io.github.phunguy65.ttbs.backend.booking.domain.error.BookingError;
import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingStatus;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingUserInfo;
import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import io.github.phunguy65.ttbs.backend.shared.domain.Money;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.port.RouteSeatAvailabilityManager;
import io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripId;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
@DisplayName("CancelBookingUseCase")
class CancelBookingUseCaseTest {

    private static final UUID USER_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_USER_UUID =
            UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final UUID BOOKING_UUID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID TRIP_UUID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID SEAT_UUID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final BookingId BOOKING_ID = BookingId.of(BOOKING_UUID);

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private RouteSeatAvailabilityManager seatAvailabilityPort;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private CancelBookingUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CancelBookingUseCase(bookingRepository, seatAvailabilityPort, eventPublisher);
    }

    private Booking bookingWithStatus(BookingStatus status) {
        return Booking.reconstitute(
                BOOKING_ID,
                io.github.phunguy65.ttbs.backend.user.domain.model.UserId.of(USER_UUID),
                ScheduledTripId.of(TRIP_UUID),
                BookingUserInfo.of("Nguyen Van A", "a@b.com", null, null, null, null, null),
                List.of(),
                Money.vnd(500_000L),
                status,
                "idem-key-1",
                Instant.now().plusSeconds(900),
                Instant.now().minusSeconds(60));
    }

    @Nested
    @DisplayName("happy path — HELD cancellation")
    class HappyPathHeld {

        @Test
        @DisplayName("HELD → cancels booking and releases held seats via releaseHeldSeats")
        void execute_heldbooking_cancelsAndReleasesHeldSeats() {
            when(bookingRepository.findById(BOOKING_ID))
                    .thenReturn(Optional.of(bookingWithStatus(BookingStatus.HELD)));
            when(seatAvailabilityPort.findSeatIdsByBookingId(BOOKING_UUID))
                    .thenReturn(List.of(SeatId.of(SEAT_UUID)));
            when(seatAvailabilityPort.findByScheduledTripIdAndSeatIds(any(), any()))
                    .thenReturn(List.of());
            when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Result<Void, BookingError> result =
                    useCase.execute(new CancelBookingCommand(BOOKING_UUID, USER_UUID));

            assertThat(result.isSuccess()).isTrue();
            verify(seatAvailabilityPort).releaseHeldSeats(any(), any());
            verify(seatAvailabilityPort, never()).cancelBookedSeats(any(), any());
        }
    }

    @Nested
    @DisplayName("happy path — CONFIRMED cancellation")
    class HappyPathConfirmed {

        @Test
        @DisplayName("CONFIRMED → cancels booking and cancels booked seats via cancelBookedSeats")
        void execute_confirmedBooking_cancelsAndCancelsBookedSeats() {
            when(bookingRepository.findById(BOOKING_ID))
                    .thenReturn(Optional.of(bookingWithStatus(BookingStatus.CONFIRMED)));
            when(seatAvailabilityPort.findSeatIdsByBookingId(BOOKING_UUID))
                    .thenReturn(List.of(SeatId.of(SEAT_UUID)));
            when(seatAvailabilityPort.findByScheduledTripIdAndSeatIds(any(), any()))
                    .thenReturn(List.of());
            when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Result<Void, BookingError> result =
                    useCase.execute(new CancelBookingCommand(BOOKING_UUID, USER_UUID));

            assertThat(result.isSuccess()).isTrue();
            verify(seatAvailabilityPort).cancelBookedSeats(any(), any());
            verify(seatAvailabilityPort, never()).releaseHeldSeats(any(), any());
        }
    }

    @Nested
    @DisplayName("error cases")
    class ErrorCases {

        @Test
        @DisplayName("returns BookingNotFound when booking missing")
        void execute_returnsBookingNotFound_whenBookingMissing() {
            when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.empty());

            Result<Void, BookingError> result =
                    useCase.execute(new CancelBookingCommand(BOOKING_UUID, USER_UUID));

            assertThat(result.isFailure()).isTrue();
            assertThat(((Result.Failure<?, BookingError>) result).error())
                    .isInstanceOf(BookingError.BookingNotFound.class);
        }

        @Test
        @DisplayName("returns Forbidden when userId mismatch")
        void execute_returnsForbidden_whenUserIdMismatch() {
            when(bookingRepository.findById(BOOKING_ID))
                    .thenReturn(Optional.of(bookingWithStatus(BookingStatus.HELD)));

            Result<Void, BookingError> result =
                    useCase.execute(new CancelBookingCommand(BOOKING_UUID, OTHER_USER_UUID));

            assertThat(result.isFailure()).isTrue();
            assertThat(((Result.Failure<?, BookingError>) result).error())
                    .isInstanceOf(BookingError.Forbidden.class);
        }

        @Test
        @DisplayName("returns InvalidStatusTransition when already CANCELLED")
        void execute_returnsInvalidStatusTransition_whenAlreadyCancelled() {
            when(bookingRepository.findById(BOOKING_ID))
                    .thenReturn(Optional.of(bookingWithStatus(BookingStatus.CANCELLED)));

            Result<Void, BookingError> result =
                    useCase.execute(new CancelBookingCommand(BOOKING_UUID, USER_UUID));

            assertThat(result.isFailure()).isTrue();
            assertThat(((Result.Failure<?, BookingError>) result).error())
                    .isInstanceOf(BookingError.InvalidStatusTransition.class);
        }
    }
}

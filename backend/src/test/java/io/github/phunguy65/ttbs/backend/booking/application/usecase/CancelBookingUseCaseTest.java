package io.github.phunguy65.ttbs.backend.booking.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.booking.application.command.CancelBookingCommand;
import io.github.phunguy65.ttbs.backend.booking.domain.error.BookingError;
import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingStatus;
import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import io.github.phunguy65.ttbs.backend.shared.domain.Money;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.port.RouteSeatAvailabilityPort;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class CancelBookingUseCaseTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private RouteSeatAvailabilityPort seatAvailabilityPort;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private CancelBookingUseCase useCase;

    private static final UUID USER_UUID = UUID.randomUUID();
    private static final UUID ROUTE_UUID = UUID.randomUUID();
    private static final UUID BOOKING_UUID = UUID.randomUUID();
    private static final UUID SEAT_UUID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new CancelBookingUseCase(bookingRepository, seatAvailabilityPort, eventPublisher);
    }

    private Booking heldBooking() {
        return Booking.reconstitute(
                BookingId.of(BOOKING_UUID),
                UserId.of(USER_UUID),
                RouteId.of(ROUTE_UUID),
                "John Doe",
                "john@example.com",
                null,
                Money.vnd(100_000L),
                "VND",
                BookingStatus.HELD,
                "idem-key",
                Instant.now().plusSeconds(900),
                Instant.now());
    }

    private Booking confirmedBooking() {
        return Booking.reconstitute(
                BookingId.of(BOOKING_UUID),
                UserId.of(USER_UUID),
                RouteId.of(ROUTE_UUID),
                "John Doe",
                "john@example.com",
                null,
                Money.vnd(100_000L),
                "VND",
                BookingStatus.CONFIRMED,
                "idem-key",
                Instant.now().plusSeconds(900),
                Instant.now());
    }

    @Test
    void execute_cancelHeld_shouldReleaseHeldSeats() {
        when(bookingRepository.findById(BookingId.of(BOOKING_UUID)))
                .thenReturn(Optional.of(heldBooking()));
        when(seatAvailabilityPort.findSeatIdsByBookingId(BOOKING_UUID))
                .thenReturn(List.of(SeatId.of(SEAT_UUID)));
        when(seatAvailabilityPort.releaseHeldSeats(any(), any())).thenReturn(Result.success());
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Result<Void, BookingError> result =
                useCase.execute(new CancelBookingCommand(BOOKING_UUID, USER_UUID));

        assertThat(result.isSuccess()).isTrue();
        verify(seatAvailabilityPort).releaseHeldSeats(any(), any());
        verify(seatAvailabilityPort, never()).cancelBookedSeats(any(), any());
    }

    @Test
    void execute_cancelConfirmed_shouldCancelBookedSeats() {
        when(bookingRepository.findById(BookingId.of(BOOKING_UUID)))
                .thenReturn(Optional.of(confirmedBooking()));
        when(seatAvailabilityPort.findSeatIdsByBookingId(BOOKING_UUID))
                .thenReturn(List.of(SeatId.of(SEAT_UUID)));
        when(seatAvailabilityPort.cancelBookedSeats(any(), any())).thenReturn(Result.success());
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Result<Void, BookingError> result =
                useCase.execute(new CancelBookingCommand(BOOKING_UUID, USER_UUID));

        assertThat(result.isSuccess()).isTrue();
        verify(seatAvailabilityPort).cancelBookedSeats(any(), any());
        verify(seatAvailabilityPort, never()).releaseHeldSeats(any(), any());
    }

    @Test
    void execute_notFound_shouldReturnFailure() {
        when(bookingRepository.findById(any())).thenReturn(Optional.empty());

        Result<Void, BookingError> result =
                useCase.execute(new CancelBookingCommand(BOOKING_UUID, USER_UUID));

        assertThat(result.isFailure()).isTrue();
        assertThat(((Result.Failure<Void, BookingError>) result).error())
                .isInstanceOf(BookingError.BookingNotFound.class);
    }

    @Test
    void execute_forbidden_shouldReturnFailure() {
        when(bookingRepository.findById(BookingId.of(BOOKING_UUID)))
                .thenReturn(Optional.of(heldBooking()));

        Result<Void, BookingError> result = useCase.execute(
                new CancelBookingCommand(BOOKING_UUID, UUID.randomUUID())); // different user

        assertThat(result.isFailure()).isTrue();
        assertThat(((Result.Failure<Void, BookingError>) result).error())
                .isInstanceOf(BookingError.Forbidden.class);
    }

    @Test
    void execute_alreadyCancelled_shouldReturnFailure() {
        Booking cancelled = Booking.reconstitute(
                BookingId.of(BOOKING_UUID),
                UserId.of(USER_UUID),
                RouteId.of(ROUTE_UUID),
                "John Doe",
                "john@example.com",
                null,
                Money.vnd(100_000L),
                "VND",
                BookingStatus.CANCELLED,
                "idem-key",
                Instant.now().plusSeconds(900),
                Instant.now());
        when(bookingRepository.findById(BookingId.of(BOOKING_UUID)))
                .thenReturn(Optional.of(cancelled));

        Result<Void, BookingError> result =
                useCase.execute(new CancelBookingCommand(BOOKING_UUID, USER_UUID));

        assertThat(result.isFailure()).isTrue();
        assertThat(((Result.Failure<Void, BookingError>) result).error())
                .isInstanceOf(BookingError.InvalidStatusTransition.class);
    }
}

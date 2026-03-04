package io.github.phunguy65.ttbs.backend.booking.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.booking.application.command.CancelBookingCommand;
import io.github.phunguy65.ttbs.backend.booking.application.dto.HoldDto;
import io.github.phunguy65.ttbs.backend.booking.domain.errors.BookingError;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookedSeat;
import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingStatus;
import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.port.RouteSeatAvailabilityPort;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import java.math.BigDecimal;
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
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private RouteSeatAvailabilityPort seatAvailabilityPort;

    private CancelBookingUseCase useCase;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ROUTE_ID = UUID.randomUUID();
    private static final UUID SEAT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new CancelBookingUseCase(bookingRepository, eventPublisher, seatAvailabilityPort);
    }

    private Booking makeHeldBooking() {
        return Booking.createHold(
                USER_ID,
                ROUTE_ID,
                List.of(BookedSeat.of(SeatId.of(SEAT_ID), new BigDecimal("500000"))),
                new BigDecimal("500000"),
                "VND",
                null,
                "idem-cancel-held",
                "Test",
                "test@test.com",
                null);
    }

    private Booking makeConfirmedBooking() {
        Booking b = makeHeldBooking();
        b.confirm();
        b.clearDomainEvents();
        return b;
    }

    private Booking makeCancelledBooking() {
        Booking b = makeHeldBooking();
        b.cancel();
        b.clearDomainEvents();
        return b;
    }

    @Test
    void execute_cancelHeld_shouldReleaseSeatsAndCancel() {
        Booking booking = makeHeldBooking();
        UUID bookingId = booking.getId().value();
        when(bookingRepository.findByIdWithSeats(BookingId.of(bookingId)))
                .thenReturn(Optional.of(booking));
        when(seatAvailabilityPort.releaseHeldSeats(any(), any())).thenReturn(Result.success());
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Result<HoldDto, BookingError> result = useCase.execute(new CancelBookingCommand(bookingId));

        assertThat(result.isSuccess()).isTrue();
        HoldDto dto = ((Result.Success<HoldDto, BookingError>) result).value();
        assertThat(dto.status()).isEqualTo(BookingStatus.CANCELLED.name());
        verify(seatAvailabilityPort).releaseHeldSeats(any(), any());
        verify(seatAvailabilityPort, never()).cancelBookedSeats(any(), any());
    }

    @Test
    void execute_cancelConfirmed_shouldCancelSeatsAndBooking() {
        Booking booking = makeConfirmedBooking();
        UUID bookingId = booking.getId().value();
        when(bookingRepository.findByIdWithSeats(BookingId.of(bookingId)))
                .thenReturn(Optional.of(booking));
        when(seatAvailabilityPort.cancelBookedSeats(any(), any())).thenReturn(Result.success());
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Result<HoldDto, BookingError> result = useCase.execute(new CancelBookingCommand(bookingId));

        assertThat(result.isSuccess()).isTrue();
        HoldDto dto = ((Result.Success<HoldDto, BookingError>) result).value();
        assertThat(dto.status()).isEqualTo(BookingStatus.CANCELLED.name());
        verify(seatAvailabilityPort).cancelBookedSeats(any(), any());
        verify(seatAvailabilityPort, never()).releaseHeldSeats(any(), any());
    }

    @Test
    void execute_cancelAlreadyCancelled_shouldReturnSuccessIdempotent() {
        Booking booking = makeCancelledBooking();
        UUID bookingId = booking.getId().value();
        when(bookingRepository.findByIdWithSeats(BookingId.of(bookingId)))
                .thenReturn(Optional.of(booking));

        Result<HoldDto, BookingError> result = useCase.execute(new CancelBookingCommand(bookingId));

        assertThat(result.isSuccess()).isTrue();
        // No seat operations, no save
        verify(seatAvailabilityPort, never()).releaseHeldSeats(any(), any());
        verify(seatAvailabilityPort, never()).cancelBookedSeats(any(), any());
        verify(bookingRepository, never()).save(any());
    }
}

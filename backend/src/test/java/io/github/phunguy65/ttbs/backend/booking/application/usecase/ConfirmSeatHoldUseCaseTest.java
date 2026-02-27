package io.github.phunguy65.ttbs.backend.booking.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.booking.application.command.ConfirmSeatHoldCommand;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
class ConfirmSeatHoldUseCaseTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private RouteSeatAvailabilityPort seatAvailabilityPort;

    private ConfirmSeatHoldUseCase useCase;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ROUTE_ID = UUID.randomUUID();
    private static final UUID SEAT_ID = UUID.randomUUID();
    private static final String PAYMENT_REF = "PAY-REF-001";

    @BeforeEach
    void setUp() {
        useCase =
                new ConfirmSeatHoldUseCase(bookingRepository, eventPublisher, seatAvailabilityPort);
    }

    private Booking makeHeldBooking(Instant deadline) {
        return Booking.createHold(
                USER_ID,
                ROUTE_ID,
                List.of(BookedSeat.of(SeatId.of(SEAT_ID), new BigDecimal("500000"))),
                new BigDecimal("500000"),
                "VND",
                deadline,
                "idem-confirm",
                "Test",
                "test@test.com",
                null);
    }

    @Test
    void execute_success_shouldConfirmAndTransitionToConfirmed() {
        Booking booking = makeHeldBooking(Instant.now().plus(15, ChronoUnit.MINUTES));
        UUID bookingId = booking.getId().value();
        when(bookingRepository.findByIdWithSeats(BookingId.of(bookingId)))
                .thenReturn(Optional.of(booking));
        when(seatAvailabilityPort.confirmHeldSeats(any(), any())).thenReturn(Result.success());
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Result<HoldDto, BookingError> result =
                useCase.execute(new ConfirmSeatHoldCommand(bookingId, PAYMENT_REF));

        assertThat(result.isSuccess()).isTrue();
        HoldDto dto = ((Result.Success<HoldDto, BookingError>) result).value();
        assertThat(dto.status()).isEqualTo(BookingStatus.CONFIRMED.name());
    }

    @Test
    void execute_whenHoldExpired_shouldReturnHoldExpiredError() {
        // Create hold with deadline in the past
        Booking booking = makeHeldBooking(Instant.now().minus(1, ChronoUnit.SECONDS));
        UUID bookingId = booking.getId().value();
        when(bookingRepository.findByIdWithSeats(BookingId.of(bookingId)))
                .thenReturn(Optional.of(booking));
        when(seatAvailabilityPort.releaseHeldSeats(any(), any())).thenReturn(Result.success());
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Result<HoldDto, BookingError> result =
                useCase.execute(new ConfirmSeatHoldCommand(bookingId, PAYMENT_REF));

        assertThat(result.isFailure()).isTrue();
        assertThat(((Result.Failure<HoldDto, BookingError>) result).error())
                .isInstanceOf(BookingError.HoldExpired.class);
        // Expiry flow should have been triggered
        verify(seatAvailabilityPort).releaseHeldSeats(any(), any());
    }

    @Test
    void execute_whenBookingNotFound_shouldReturnInvalidStatusTransition() {
        UUID bookingId = UUID.randomUUID();
        when(bookingRepository.findByIdWithSeats(BookingId.of(bookingId)))
                .thenReturn(Optional.empty());

        Result<HoldDto, BookingError> result =
                useCase.execute(new ConfirmSeatHoldCommand(bookingId, PAYMENT_REF));

        assertThat(result.isFailure()).isTrue();
        assertThat(((Result.Failure<HoldDto, BookingError>) result).error())
                .isInstanceOf(BookingError.InvalidStatusTransition.class);
    }
}

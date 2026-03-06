package io.github.phunguy65.ttbs.backend.booking.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.booking.application.command.CreateBookingCommand;
import io.github.phunguy65.ttbs.backend.booking.application.dto.BookingDto;
import io.github.phunguy65.ttbs.backend.booking.domain.error.BookingError;
import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingStatus;
import io.github.phunguy65.ttbs.backend.booking.domain.model.UserId;
import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import io.github.phunguy65.ttbs.backend.shared.domain.Money;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.port.RouteSeatAvailabilityPort;
import io.github.phunguy65.ttbs.backend.train.domain.error.RouteSeatAvailabilityError;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
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
class CreateBookingUseCaseTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private RouteSeatAvailabilityPort seatAvailabilityPort;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private CreateBookingUseCase useCase;

    private static final UUID USER_UUID = UUID.randomUUID();
    private static final UUID ROUTE_UUID = UUID.randomUUID();
    private static final UUID SEAT_UUID = UUID.randomUUID();
    private static final String IDEM_KEY = "test-idem-key";

    private CreateBookingCommand command;

    @BeforeEach
    void setUp() {
        useCase = new CreateBookingUseCase(bookingRepository, seatAvailabilityPort, eventPublisher);
        command = new CreateBookingCommand(
                USER_UUID,
                ROUTE_UUID,
                List.of(SeatId.of(SEAT_UUID)),
                "John Doe",
                "john@example.com",
                null,
                IDEM_KEY);
    }

    @Test
    void execute_success_shouldSaveBookingAndReturnDto() {
        when(bookingRepository.findByIdempotencyKey(IDEM_KEY)).thenReturn(Optional.empty());
        when(bookingRepository.findActiveHoldByUserAndRoute(any(), any()))
                .thenReturn(Optional.empty());
        when(seatAvailabilityPort.holdSeats(any(), any())).thenReturn(Result.success());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Result<BookingDto, BookingError> result = useCase.execute(command);

        assertThat(result.isSuccess()).isTrue();
        BookingDto dto = ((Result.Success<BookingDto, BookingError>) result).value();
        assertThat(dto.userId()).isEqualTo(USER_UUID);
        assertThat(dto.routeId()).isEqualTo(ROUTE_UUID);
        assertThat(dto.status()).isEqualTo(BookingStatus.HELD);
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void execute_idempotency_shouldReturnExistingBooking() {
        Booking existing = Booking.reconstitute(
                BookingId.of(UUID.randomUUID()),
                UserId.of(USER_UUID),
                RouteId.of(ROUTE_UUID),
                "John Doe",
                "john@example.com",
                null,
                Money.vnd(100_000L),
                "VND",
                BookingStatus.HELD,
                IDEM_KEY,
                Instant.now().plusSeconds(900),
                Instant.now());
        when(bookingRepository.findByIdempotencyKey(IDEM_KEY)).thenReturn(Optional.of(existing));

        Result<BookingDto, BookingError> result = useCase.execute(command);

        assertThat(result.isSuccess()).isTrue();
        verify(bookingRepository, never()).save(any());
        verify(seatAvailabilityPort, never()).holdSeats(any(), any());
    }

    @Test
    void execute_activeHoldExists_shouldReturnFailure() {
        Booking activeHold = Booking.reconstitute(
                BookingId.of(UUID.randomUUID()),
                UserId.of(USER_UUID),
                RouteId.of(ROUTE_UUID),
                "John Doe",
                "john@example.com",
                null,
                Money.vnd(100_000L),
                "VND",
                BookingStatus.HELD,
                "other-key",
                Instant.now().plusSeconds(900),
                Instant.now());
        when(bookingRepository.findByIdempotencyKey(IDEM_KEY)).thenReturn(Optional.empty());
        when(bookingRepository.findActiveHoldByUserAndRoute(any(), any()))
                .thenReturn(Optional.of(activeHold));

        Result<BookingDto, BookingError> result = useCase.execute(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(((Result.Failure<BookingDto, BookingError>) result).error())
                .isInstanceOf(BookingError.ActiveHoldExists.class);
    }

    @Test
    void execute_seatNotAvailable_shouldReturnFailure() {
        when(bookingRepository.findByIdempotencyKey(IDEM_KEY)).thenReturn(Optional.empty());
        when(bookingRepository.findActiveHoldByUserAndRoute(any(), any()))
                .thenReturn(Optional.empty());
        when(seatAvailabilityPort.holdSeats(any(), any()))
                .thenReturn(Result.failure(new RouteSeatAvailabilityError.SeatNotAvailable()));

        Result<BookingDto, BookingError> result = useCase.execute(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(((Result.Failure<BookingDto, BookingError>) result).error())
                .isInstanceOf(BookingError.SeatNotAvailable.class);
    }
}

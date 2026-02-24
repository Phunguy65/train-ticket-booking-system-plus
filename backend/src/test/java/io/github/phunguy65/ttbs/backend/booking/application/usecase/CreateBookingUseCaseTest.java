package io.github.phunguy65.ttbs.backend.booking.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.booking.application.command.CreateBookingCommand;
import io.github.phunguy65.ttbs.backend.booking.application.dto.BookingDto;
import io.github.phunguy65.ttbs.backend.booking.domain.errors.BookingError;
import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingStatus;
import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.port.RouteSeatAvailabilityPort;
import io.github.phunguy65.ttbs.backend.train.domain.errors.RouteSeatAvailabilityError;
import java.math.BigDecimal;
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
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private RouteSeatAvailabilityPort seatAvailabilityPort;

    private CreateBookingUseCase useCase;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ROUTE_ID = UUID.randomUUID();
    private static final UUID SEAT_ID = UUID.randomUUID();
    private static final String IDEMPOTENCY_KEY = "idempotency-key-001";

    @BeforeEach
    void setUp() {
        useCase = new CreateBookingUseCase(bookingRepository, eventPublisher, seatAvailabilityPort);
    }

    @Test
    void execute_shouldCreateAndPersistBooking() {
        CreateBookingCommand command =
                new CreateBookingCommand(USER_ID, ROUTE_ID, SEAT_ID, IDEMPOTENCY_KEY);
        when(bookingRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
        when(seatAvailabilityPort.reserveSeat(any(), any())).thenReturn(Result.success());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Result<BookingDto, BookingError> result = useCase.execute(command);

        assertThat(result.isSuccess()).isTrue();
        verify(bookingRepository).save(any(Booking.class));
        verify(seatAvailabilityPort).reserveSeat(any(), any());
        BookingDto dto = ((Result.Success<BookingDto, BookingError>) result).value();
        assertThat(dto.userId()).isEqualTo(USER_ID);
        assertThat(dto.routeId()).isEqualTo(ROUTE_ID);
        assertThat(dto.seatId()).isEqualTo(SEAT_ID);
        assertThat(dto.status()).isEqualTo(BookingStatus.PENDING.name());
        assertThat(dto.idempotencyKey()).isEqualTo(IDEMPOTENCY_KEY);
    }

    @Test
    void execute_withExistingIdempotencyKey_shouldReturnExistingBooking() {
        CreateBookingCommand command =
                new CreateBookingCommand(USER_ID, ROUTE_ID, SEAT_ID, IDEMPOTENCY_KEY);
        Booking existingBooking =
                Booking.create(USER_ID, ROUTE_ID, SEAT_ID, BigDecimal.ZERO, "VND", IDEMPOTENCY_KEY);
        when(bookingRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
                .thenReturn(Optional.of(existingBooking));

        Result<BookingDto, BookingError> result = useCase.execute(command);

        assertThat(result.isSuccess()).isTrue();
        verify(bookingRepository, never()).save(any());
        verify(seatAvailabilityPort, never()).reserveSeat(any(), any());
        BookingDto dto = ((Result.Success<BookingDto, BookingError>) result).value();
        assertThat(dto.idempotencyKey()).isEqualTo(IDEMPOTENCY_KEY);
    }

    @Test
    void execute_shouldReturnDtoWithPendingStatus() {
        CreateBookingCommand command =
                new CreateBookingCommand(USER_ID, ROUTE_ID, SEAT_ID, IDEMPOTENCY_KEY);
        when(bookingRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
        when(seatAvailabilityPort.reserveSeat(any(), any())).thenReturn(Result.success());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Result<BookingDto, BookingError> result = useCase.execute(command);

        assertThat(result.isSuccess()).isTrue();
        BookingDto dto = ((Result.Success<BookingDto, BookingError>) result).value();
        assertThat(dto.status()).isEqualTo("PENDING");
        assertThat(dto.id()).isNotNull();
    }

    @Test
    void execute_whenSeatNotAvailable_shouldReturnSeatNotAvailableFailure() {
        CreateBookingCommand command =
                new CreateBookingCommand(USER_ID, ROUTE_ID, SEAT_ID, IDEMPOTENCY_KEY);
        when(bookingRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
        when(seatAvailabilityPort.reserveSeat(any(), any()))
                .thenReturn(Result.failure(new RouteSeatAvailabilityError.SeatNotAvailable()));

        Result<BookingDto, BookingError> result = useCase.execute(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(((Result.Failure<BookingDto, BookingError>) result).error())
                .isInstanceOf(BookingError.SeatNotAvailable.class);
        verify(bookingRepository, never()).save(any());
    }
}

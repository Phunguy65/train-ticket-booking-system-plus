package io.github.phunguy65.ttbs.backend.booking.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.booking.application.command.CreateSeatHoldCommand;
import io.github.phunguy65.ttbs.backend.booking.application.dto.HoldDto;
import io.github.phunguy65.ttbs.backend.booking.application.service.PricingService;
import io.github.phunguy65.ttbs.backend.booking.domain.errors.BookingError;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookedSeat;
import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingStatus;
import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.station.domain.model.StationId;
import io.github.phunguy65.ttbs.backend.train.application.port.RoutePort;
import io.github.phunguy65.ttbs.backend.train.application.port.RouteSeatAvailabilityPort;
import io.github.phunguy65.ttbs.backend.train.application.port.SeatPort;
import io.github.phunguy65.ttbs.backend.train.domain.errors.RouteSeatAvailabilityError;
import io.github.phunguy65.ttbs.backend.train.domain.model.Route;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteStatus;
import io.github.phunguy65.ttbs.backend.train.domain.model.Seat;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
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
class CreateSeatHoldUseCaseTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private RouteSeatAvailabilityPort seatAvailabilityPort;

    @Mock
    private RoutePort routePort;

    @Mock
    private SeatPort seatPort;

    private PricingService pricingService;
    private CreateSeatHoldUseCase useCase;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ROUTE_ID = UUID.randomUUID();
    private static final UUID SEAT_ID = UUID.randomUUID();
    private static final String IDEMPOTENCY_KEY = "hold-idem-001";

    private Route testRoute;
    private Seat testSeat;

    @BeforeEach
    void setUp() {
        pricingService = new PricingService();
        useCase = new CreateSeatHoldUseCase(
                bookingRepository,
                eventPublisher,
                seatAvailabilityPort,
                routePort,
                seatPort,
                pricingService);

        testRoute = Route.reconstitute(
                RouteId.of(ROUTE_ID),
                TrainId.of(UUID.randomUUID()),
                StationId.of(UUID.randomUUID()),
                StationId.of(UUID.randomUUID()),
                Instant.now(),
                Instant.now().plus(2, ChronoUnit.HOURS),
                new BigDecimal("500000"),
                RouteStatus.SCHEDULED,
                Instant.now());

        testSeat =
                Seat.reconstitute(SeatId.of(SEAT_ID), testRoute.getTrainId(), "1A", Instant.now());
    }

    private CreateSeatHoldCommand createCommand() {
        return new CreateSeatHoldCommand(
                USER_ID,
                ROUTE_ID,
                List.of(SEAT_ID),
                IDEMPOTENCY_KEY,
                "Test Passenger",
                "test@example.com",
                "+84901234567");
    }

    @Test
    void execute_success_shouldCreateHoldAndReturnDto() {
        when(bookingRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
        when(bookingRepository.findActiveHoldByUserIdAndRouteId(any(), any()))
                .thenReturn(Optional.empty());
        when(routePort.findById(any())).thenReturn(Optional.of(testRoute));
        when(seatPort.findById(SeatId.of(SEAT_ID))).thenReturn(Optional.of(testSeat));
        when(seatAvailabilityPort.holdSeats(any(), any())).thenReturn(Result.success());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Result<HoldDto, BookingError> result = useCase.execute(createCommand());

        assertThat(result.isSuccess()).isTrue();
        HoldDto dto = ((Result.Success<HoldDto, BookingError>) result).value();
        assertThat(dto.status()).isEqualTo(BookingStatus.HELD.name());
        assertThat(dto.routeId()).isEqualTo(ROUTE_ID);
        assertThat(dto.seats()).hasSize(1);
        assertThat(dto.seats().getFirst().seatId()).isEqualTo(SEAT_ID);
        assertThat(dto.expiresAt()).isNotNull();
    }

    @Test
    void execute_withExistingIdempotencyKey_shouldReturnExistingHold() {
        Booking existing = makeExistingHold();
        when(bookingRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
                .thenReturn(Optional.of(existing));

        Result<HoldDto, BookingError> result = useCase.execute(createCommand());

        assertThat(result.isSuccess()).isTrue();
        verify(bookingRepository, never()).save(any());
        verify(seatAvailabilityPort, never()).holdSeats(any(), any());
    }

    @Test
    void execute_whenActiveHoldExists_shouldReturnActiveHoldExistsError() {
        Booking activeHold = makeExistingHold();
        when(bookingRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
        when(bookingRepository.findActiveHoldByUserIdAndRouteId(any(), any()))
                .thenReturn(Optional.of(activeHold));

        Result<HoldDto, BookingError> result = useCase.execute(createCommand());

        assertThat(result.isFailure()).isTrue();
        assertThat(((Result.Failure<HoldDto, BookingError>) result).error())
                .isInstanceOf(BookingError.ActiveHoldExists.class);
        verify(seatAvailabilityPort, never()).holdSeats(any(), any());
    }

    @Test
    void execute_whenSeatsUnavailable_shouldReturnSeatsNotAvailableError() {
        when(bookingRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
        when(bookingRepository.findActiveHoldByUserIdAndRouteId(any(), any()))
                .thenReturn(Optional.empty());
        when(routePort.findById(any())).thenReturn(Optional.of(testRoute));
        when(seatPort.findById(SeatId.of(SEAT_ID))).thenReturn(Optional.of(testSeat));
        when(seatAvailabilityPort.holdSeats(any(), any()))
                .thenReturn(Result.failure(new RouteSeatAvailabilityError.SeatNotAvailable()));

        Result<HoldDto, BookingError> result = useCase.execute(createCommand());

        assertThat(result.isFailure()).isTrue();
        assertThat(((Result.Failure<HoldDto, BookingError>) result).error())
                .isInstanceOf(BookingError.SeatsNotAvailable.class);
        verify(bookingRepository, never()).save(any());
    }

    private Booking makeExistingHold() {
        return Booking.createHold(
                USER_ID,
                ROUTE_ID,
                List.of(BookedSeat.of(SeatId.of(SEAT_ID), new BigDecimal("500000"))),
                new BigDecimal("500000"),
                "VND",
                Instant.now().plus(15, ChronoUnit.MINUTES),
                IDEMPOTENCY_KEY,
                "Test",
                "test@test.com",
                null);
    }
}

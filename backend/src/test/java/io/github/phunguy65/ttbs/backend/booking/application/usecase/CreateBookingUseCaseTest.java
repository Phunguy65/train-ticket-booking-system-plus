package io.github.phunguy65.ttbs.backend.booking.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.phunguy65.ttbs.backend.booking.application.command.CreateBookingCommand;
import io.github.phunguy65.ttbs.backend.booking.application.response.BookingResponse;
import io.github.phunguy65.ttbs.backend.booking.application.response.BookingResponseMapper;
import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingStatus;
import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import io.github.phunguy65.ttbs.backend.shared.domain.Money;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.station.domain.model.StationId;
import io.github.phunguy65.ttbs.backend.train.application.port.RouteSeatAvailabilityManager;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteTemplate;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteTemplateId;
import io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTrip;
import io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripId;
import io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripStatus;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteTemplateRepository;
import io.github.phunguy65.ttbs.backend.train.domain.repository.ScheduledTripRepository;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import io.github.phunguy65.ttbs.backend.user.domain.projection.UserSummary;
import io.github.phunguy65.ttbs.backend.user.domain.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateBookingUseCase — price snapshot capture")
class CreateBookingUseCaseTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SCHEDULED_TRIP_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ROUTE_TEMPLATE_ID =
            UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID ORIGIN_STATION_ID =
            UUID.fromString("77777777-0000-0000-0000-000000000001");
    private static final UUID DEST_STATION_ID =
            UUID.fromString("77777777-0000-0000-0000-000000000002");
    private static final UUID SEAT_1_ID = UUID.fromString("44444444-0000-0000-0000-000000000001");
    private static final UUID SEAT_2_ID = UUID.fromString("44444444-0000-0000-0000-000000000002");
    private static final long BASE_PRICE_PER_SEAT = 500_000L;
    private static final Instant DEPARTURE = Instant.parse("2026-06-01T08:00:00Z");
    private static final Instant ARRIVAL = Instant.parse("2026-06-01T12:00:00Z");
    private static final int SEAT_COUNT = 2;
    private static final long TOTAL_PRICE = BASE_PRICE_PER_SEAT * SEAT_COUNT;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private RouteSeatAvailabilityManager seatAvailabilityPort;

    @Mock
    private ScheduledTripRepository scheduledTripRepository;

    @Mock
    private RouteTemplateRepository routeTemplateRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private CreateBookingUseCase useCase;

    @BeforeEach
    void setUp() {
        var mapper = new BookingResponseMapper();
        useCase = new CreateBookingUseCase(
                bookingRepository,
                seatAvailabilityPort,
                scheduledTripRepository,
                routeTemplateRepository,
                userRepository,
                eventPublisher,
                mapper);
    }

    private UserSummary userSummary() {
        return new UserSummary(
                USER_ID,
                "a@b.com",
                "Nguyen Van A",
                "0900000000",
                LocalDate.of(1990, 1, 1),
                "Male",
                "012345678",
                "123 Main St",
                "USER",
                Instant.now());
    }

    private CreateBookingCommand command() {
        return new CreateBookingCommand(
                USER_ID,
                SCHEDULED_TRIP_ID,
                List.of(SeatId.of(SEAT_1_ID), SeatId.of(SEAT_2_ID)),
                "idem-key-" + UUID.randomUUID());
    }

    private void stubHappyPath() {
        when(bookingRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(userRepository.findSummaryById(UserId.of(USER_ID)))
                .thenReturn(Optional.of(userSummary()));
        when(bookingRepository.findActiveHoldByUserAndScheduledTrip(any(), any()))
                .thenReturn(Optional.empty());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(scheduledTripRepository.findById(ScheduledTripId.of(SCHEDULED_TRIP_ID)))
                .thenReturn(Optional.of(ScheduledTrip.reconstitute(
                        ScheduledTripId.of(SCHEDULED_TRIP_ID),
                        RouteTemplateId.of(ROUTE_TEMPLATE_ID),
                        null,
                        DEPARTURE,
                        ARRIVAL,
                        ScheduledTripStatus.SCHEDULED,
                        DEPARTURE.minusSeconds(86400),
                        null)));
        when(routeTemplateRepository.findById(RouteTemplateId.of(ROUTE_TEMPLATE_ID)))
                .thenReturn(Optional.of(RouteTemplate.reconstitute(
                        RouteTemplateId.of(ROUTE_TEMPLATE_ID),
                        StationId.of(ORIGIN_STATION_ID),
                        StationId.of(DEST_STATION_ID),
                        Money.vnd(BASE_PRICE_PER_SEAT),
                        DEPARTURE.minusSeconds(86400),
                        null)));
        when(seatAvailabilityPort.holdSeatsWithBookingId(any(), any(), any(), any()))
                .thenReturn(Result.success());
        when(seatAvailabilityPort.findByBookingId(any())).thenReturn(List.of());
    }

    @Test
    @DisplayName("holdSeatsWithBookingId is called with pricePerSeat from routeTemplate.basePrice")
    void holdSeatsWithBookingId_receivesPricePerSeatFromRouteTemplate() {
        stubHappyPath();

        Result<BookingResponse, ?> result = useCase.execute(command());

        assertThat(result.isSuccess()).isTrue();

        ArgumentCaptor<Money> priceCaptor = ArgumentCaptor.forClass(Money.class);
        verify(seatAvailabilityPort)
                .holdSeatsWithBookingId(
                        eq(ScheduledTripId.of(SCHEDULED_TRIP_ID)),
                        eq(List.of(SeatId.of(SEAT_1_ID), SeatId.of(SEAT_2_ID))),
                        any(UUID.class),
                        priceCaptor.capture());

        assertThat(priceCaptor.getValue()).isEqualTo(Money.vnd(BASE_PRICE_PER_SEAT));
    }

    @Test
    @DisplayName("response totalPrice equals pricePerSeat × seatCount")
    void response_totalPriceEqualsPricePerSeatTimesSeatCount() {
        stubHappyPath();

        Result<BookingResponse, ?> result = useCase.execute(command());

        assertThat(result.isSuccess()).isTrue();
        BookingResponse response = ((Result.Success<BookingResponse, ?>) result).value();
        assertThat(response.totalPrice()).isEqualTo(TOTAL_PRICE);
    }

    @Test
    @DisplayName("response status is HELD")
    void response_statusIsHeld() {
        stubHappyPath();

        Result<BookingResponse, ?> result = useCase.execute(command());

        assertThat(result.isSuccess()).isTrue();
        BookingResponse response = ((Result.Success<BookingResponse, ?>) result).value();
        assertThat(response.status()).isEqualTo(BookingStatus.HELD);
    }
}

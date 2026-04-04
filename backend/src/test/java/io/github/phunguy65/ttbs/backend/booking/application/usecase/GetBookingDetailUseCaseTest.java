package io.github.phunguy65.ttbs.backend.booking.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.github.phunguy65.ttbs.backend.booking.application.query.GetBookingDetailQuery;
import io.github.phunguy65.ttbs.backend.booking.application.response.BookingDetailResponse;
import io.github.phunguy65.ttbs.backend.booking.domain.error.BookingError;
import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingStatus;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingUserInfo;
import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import io.github.phunguy65.ttbs.backend.payment.domain.projection.PaymentSummary;
import io.github.phunguy65.ttbs.backend.payment.domain.repository.PaymentRepository;
import io.github.phunguy65.ttbs.backend.shared.domain.Money;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripId;
import io.github.phunguy65.ttbs.backend.train.domain.projection.BookedSeatSummary;
import io.github.phunguy65.ttbs.backend.train.domain.projection.ScheduledTripEnrichedSummary;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteSeatAvailabilityRepository;
import io.github.phunguy65.ttbs.backend.train.domain.repository.ScheduledTripRepository;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetBookingDetailUseCaseTest {

    private static final UUID BOOKING_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID OTHER_USER_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000003");
    private static final UUID TRIP_ID = UUID.fromString("10000000-0000-0000-0000-000000000004");
    private static final UUID ROUTE_ID = UUID.fromString("10000000-0000-0000-0000-000000000005");
    private static final UUID TRAIN_ID = UUID.fromString("10000000-0000-0000-0000-000000000006");
    private static final UUID ORIGIN_ID = UUID.fromString("10000000-0000-0000-0000-000000000007");
    private static final UUID DEST_ID = UUID.fromString("10000000-0000-0000-0000-000000000008");
    private static final UUID SEAT_ID = UUID.fromString("10000000-0000-0000-0000-000000000009");
    private static final UUID COACH_ID = UUID.fromString("10000000-0000-0000-0000-000000000010");
    private static final UUID PAYMENT_ID = UUID.fromString("10000000-0000-0000-0000-000000000011");
    private static final Instant CREATED_AT = Instant.parse("2026-04-01T09:00:00Z");
    private static final Instant PAYMENT_DEADLINE = Instant.parse("2026-04-01T09:15:00Z");

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ScheduledTripRepository scheduledTripRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RouteSeatAvailabilityRepository routeSeatAvailabilityRepository;

    private GetBookingDetailUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetBookingDetailUseCase(
                bookingRepository,
                scheduledTripRepository,
                paymentRepository,
                routeSeatAvailabilityRepository);
    }

    @Test
    void executeReturnsNotFoundWhenBookingMissing() {
        when(bookingRepository.findById(BookingId.of(BOOKING_ID))).thenReturn(Optional.empty());

        Result<BookingDetailResponse, BookingError> result =
                useCase.execute(new GetBookingDetailQuery(BOOKING_ID, USER_ID));

        assertThat(result).isInstanceOf(Result.Failure.class);
        assertThat(((Result.Failure<BookingDetailResponse, BookingError>) result).error())
                .isInstanceOf(BookingError.BookingNotFound.class);
    }

    @Test
    void executeReturnsForbiddenWhenRequesterDoesNotOwnBooking() {
        when(bookingRepository.findById(BookingId.of(BOOKING_ID)))
                .thenReturn(Optional.of(booking()));

        Result<BookingDetailResponse, BookingError> result =
                useCase.execute(new GetBookingDetailQuery(BOOKING_ID, OTHER_USER_ID));

        assertThat(result).isInstanceOf(Result.Failure.class);
        assertThat(((Result.Failure<BookingDetailResponse, BookingError>) result).error())
                .isInstanceOf(BookingError.Forbidden.class);
    }

    @Test
    void executeReturnsEnrichedBookingDetail() {
        when(bookingRepository.findById(BookingId.of(BOOKING_ID)))
                .thenReturn(Optional.of(booking()));
        when(scheduledTripRepository.findEnrichedByIdIncludingDeleted(ScheduledTripId.of(TRIP_ID)))
                .thenReturn(Optional.of(trip()));
        when(paymentRepository.findSummaryByBookingId(BookingId.of(BOOKING_ID)))
                .thenReturn(Optional.of(payment()));
        when(routeSeatAvailabilityRepository.findBookedSeatSummariesByBookingId(
                        BookingId.of(BOOKING_ID)))
                .thenReturn(List.of(seat()));

        Result<BookingDetailResponse, BookingError> result =
                useCase.execute(new GetBookingDetailQuery(BOOKING_ID, USER_ID));

        assertThat(result).isInstanceOf(Result.Success.class);
        BookingDetailResponse response =
                ((Result.Success<BookingDetailResponse, BookingError>) result).value();
        assertThat(response.id()).isEqualTo(BOOKING_ID);
        assertThat(response.passengerInfo().fullName()).isEqualTo("Nguyen Van A");
        assertThat(response.trip()).isNotNull();
        assertThat(response.trip().status()).isEqualTo("SCHEDULED");
        assertThat(response.trip().route().basePrice()).isEqualTo(450_000);
        assertThat(response.trip().route().currency()).isEqualTo("VND");
        assertThat(response.trip().route().origin().name()).isEqualTo("Sai Gon");
        assertThat(response.trip().route().destination().name()).isEqualTo("Da Nang");
        assertThat(response.trip().train()).isNotNull();
        assertThat(response.trip().train().trainNumber()).isEqualTo("SE1");
        assertThat(response.trip().train().name()).isEqualTo("North-South Express");
        assertThat(response.trip().train().totalSeats()).isEqualTo(200);
        assertThat(response.payment()).isNotNull();
        assertThat(response.payment().status().name()).isEqualTo("PENDING");
        assertThat(response.payment().stripePaymentIntentId()).isEqualTo("pi_123");
        assertThat(response.seats()).hasSize(1);
        assertThat(response.seats().getFirst().seatNumber()).isEqualTo("A1");
        assertThat(response.seats().getFirst().coachNumber()).isEqualTo(1);
        assertThat(response.seats().getFirst().status().name()).isEqualTo("HELD");
    }

    @Test
    void executeAllowsNullPaymentAndTrip() {
        when(bookingRepository.findById(BookingId.of(BOOKING_ID)))
                .thenReturn(Optional.of(booking()));
        when(scheduledTripRepository.findEnrichedByIdIncludingDeleted(ScheduledTripId.of(TRIP_ID)))
                .thenReturn(Optional.empty());
        when(paymentRepository.findSummaryByBookingId(BookingId.of(BOOKING_ID)))
                .thenReturn(Optional.empty());
        when(routeSeatAvailabilityRepository.findBookedSeatSummariesByBookingId(
                        BookingId.of(BOOKING_ID)))
                .thenReturn(List.of());

        Result<BookingDetailResponse, BookingError> result =
                useCase.execute(new GetBookingDetailQuery(BOOKING_ID, USER_ID));

        assertThat(result).isInstanceOf(Result.Success.class);
        BookingDetailResponse response =
                ((Result.Success<BookingDetailResponse, BookingError>) result).value();
        assertThat(response.trip()).isNull();
        assertThat(response.payment()).isNull();
        assertThat(response.seats()).isEmpty();
    }

    @Test
    void executeSupportsLowercaseStatusesAndMultipleSeats() {
        when(bookingRepository.findById(BookingId.of(BOOKING_ID)))
                .thenReturn(Optional.of(booking()));
        when(scheduledTripRepository.findEnrichedByIdIncludingDeleted(ScheduledTripId.of(TRIP_ID)))
                .thenReturn(Optional.of(tripWithoutTrain()));
        when(paymentRepository.findSummaryByBookingId(BookingId.of(BOOKING_ID)))
                .thenReturn(Optional.of(new PaymentSummary(
                        PAYMENT_ID,
                        BOOKING_ID,
                        USER_ID,
                        "pending",
                        "https://checkout.test/session",
                        450_000,
                        "VND",
                        "pi_456",
                        CREATED_AT)));
        when(routeSeatAvailabilityRepository.findBookedSeatSummariesByBookingId(
                        BookingId.of(BOOKING_ID)))
                .thenReturn(List.of(
                        new BookedSeatSummary(SEAT_ID, COACH_ID, 1, "A1", "held", 225_000L),
                        new BookedSeatSummary(
                                UUID.fromString("10000000-0000-0000-0000-000000000012"),
                                UUID.fromString("10000000-0000-0000-0000-000000000013"),
                                2,
                                "B1",
                                "booked",
                                225_000L)));

        Result<BookingDetailResponse, BookingError> result =
                useCase.execute(new GetBookingDetailQuery(BOOKING_ID, USER_ID));

        assertThat(result).isInstanceOf(Result.Success.class);
        BookingDetailResponse response =
                ((Result.Success<BookingDetailResponse, BookingError>) result).value();
        assertThat(response.payment()).isNotNull();
        assertThat(response.payment().status().name()).isEqualTo("PENDING");
        assertThat(response.trip()).isNotNull();
        assertThat(response.trip().train()).isNull();
        assertThat(response.seats())
                .extracting(BookingDetailResponse.Seat::seatNumber)
                .containsExactly("A1", "B1");
        assertThat(response.seats())
                .extracting(seat -> seat.status().name())
                .containsExactly("HELD", "BOOKED");
    }

    private Booking booking() {
        return Booking.reconstitute(
                BookingId.of(BOOKING_ID),
                UserId.of(USER_ID),
                ScheduledTripId.of(TRIP_ID),
                BookingUserInfo.of(
                        "Nguyen Van A",
                        "a@example.com",
                        "0900000000",
                        LocalDate.of(1990, 1, 1),
                        "MALE",
                        "0123456789",
                        "123 Main St"),
                Money.vnd(450_000),
                BookingStatus.HELD,
                "idem-1",
                PAYMENT_DEADLINE,
                CREATED_AT);
    }

    private ScheduledTripEnrichedSummary trip() {
        return new ScheduledTripEnrichedSummary(
                TRIP_ID,
                ROUTE_ID,
                TRAIN_ID,
                Instant.parse("2026-05-01T08:00:00Z"),
                Instant.parse("2026-05-01T12:00:00Z"),
                "SCHEDULED",
                CREATED_AT,
                240,
                100,
                "SE1",
                "North-South Express",
                200,
                ORIGIN_ID,
                "SGN",
                "Sai Gon",
                "Ho Chi Minh",
                DEST_ID,
                "DAD",
                "Da Nang",
                "Da Nang",
                450_000,
                "VND");
    }

    private ScheduledTripEnrichedSummary tripWithoutTrain() {
        return new ScheduledTripEnrichedSummary(
                TRIP_ID,
                ROUTE_ID,
                null,
                Instant.parse("2026-05-01T08:00:00Z"),
                Instant.parse("2026-05-01T12:00:00Z"),
                "SCHEDULED",
                CREATED_AT,
                240,
                100,
                null,
                null,
                null,
                ORIGIN_ID,
                "SGN",
                "Sai Gon",
                "Ho Chi Minh",
                DEST_ID,
                "DAD",
                "Da Nang",
                "Da Nang",
                450_000,
                "VND");
    }

    private PaymentSummary payment() {
        return new PaymentSummary(
                PAYMENT_ID,
                BOOKING_ID,
                USER_ID,
                "PENDING",
                "https://checkout.test/session",
                450_000,
                "VND",
                "pi_123",
                CREATED_AT);
    }

    private BookedSeatSummary seat() {
        return new BookedSeatSummary(SEAT_ID, COACH_ID, 1, "A1", "HELD", 225_000L);
    }
}

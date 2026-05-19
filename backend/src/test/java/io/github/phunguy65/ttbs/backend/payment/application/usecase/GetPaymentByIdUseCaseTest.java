package io.github.phunguy65.ttbs.backend.payment.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingStatus;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingUserInfo;
import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import io.github.phunguy65.ttbs.backend.payment.application.query.GetPaymentByIdQuery;
import io.github.phunguy65.ttbs.backend.payment.application.response.PaymentDetailResponse;
import io.github.phunguy65.ttbs.backend.payment.domain.error.PaymentError;
import io.github.phunguy65.ttbs.backend.payment.domain.model.PaymentId;
import io.github.phunguy65.ttbs.backend.payment.domain.projection.PaymentSummary;
import io.github.phunguy65.ttbs.backend.payment.domain.repository.PaymentRepository;
import io.github.phunguy65.ttbs.backend.shared.domain.Money;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteSeatAvailabilityRepository;
import io.github.phunguy65.ttbs.backend.train.domain.repository.ScheduledTripRepository;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("GetPaymentByIdUseCase")
class GetPaymentByIdUseCaseTest {

    private static final UUID USER_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_USER_UUID =
            UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final UUID BOOKING_UUID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PAYMENT_UUID =
            UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID TRIP_UUID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ScheduledTripRepository scheduledTripRepository;

    @Mock
    private RouteSeatAvailabilityRepository routeSeatAvailabilityRepository;

    private GetPaymentByIdUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetPaymentByIdUseCase(
                paymentRepository,
                bookingRepository,
                scheduledTripRepository,
                routeSeatAvailabilityRepository);
    }

    private PaymentSummary paymentSummary(UUID userId) {
        return new PaymentSummary(
                PAYMENT_UUID,
                BOOKING_UUID,
                userId,
                "PENDING",
                "https://checkout.stripe.com/test",
                500_000L,
                "VND",
                null,
                Instant.now());
    }

    private Booking heldBooking() {
        return Booking.reconstitute(
                BookingId.of(BOOKING_UUID),
                UserId.of(USER_UUID),
                io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripId.of(TRIP_UUID),
                BookingUserInfo.of("Nguyen Van A", "a@b.com", null, null, null, null, null),
                List.of(),
                Money.vnd(500_000L),
                BookingStatus.HELD,
                "idem-key-1",
                Instant.now().plusSeconds(900),
                Instant.now().minusSeconds(60));
    }

    @Nested
    @DisplayName("happy path")
    class HappyPath {

        @Test
        @DisplayName("returns PaymentDetailResponse with booking, trip, seats, passengers")
        void execute_returnsPaymentDetailResponse() {
            when(paymentRepository.findSummaryById(PaymentId.of(PAYMENT_UUID)))
                    .thenReturn(Optional.of(paymentSummary(USER_UUID)));
            when(bookingRepository.findById(BookingId.of(BOOKING_UUID)))
                    .thenReturn(Optional.of(heldBooking()));
            when(scheduledTripRepository.findEnrichedByIdIncludingDeleted(any()))
                    .thenReturn(Optional.empty());
            when(routeSeatAvailabilityRepository.findBookedSeatSummariesByBookingId(any()))
                    .thenReturn(List.of());

            Result<PaymentDetailResponse, PaymentError> result =
                    useCase.execute(new GetPaymentByIdQuery(PAYMENT_UUID, USER_UUID));

            assertThat(result.isSuccess()).isTrue();
            PaymentDetailResponse response =
                    ((Result.Success<PaymentDetailResponse, PaymentError>) result).value();
            assertThat(response.paymentId()).isEqualTo(PAYMENT_UUID);
            assertThat(response.bookingId()).isEqualTo(BOOKING_UUID);
        }

        @Test
        @DisplayName(
                "returns response with null bookingForTicket when booking not found in repository")
        void execute_returnsResponseWithNullBookingForTicket_whenBookingNotFound() {
            when(paymentRepository.findSummaryById(PaymentId.of(PAYMENT_UUID)))
                    .thenReturn(Optional.of(paymentSummary(USER_UUID)));
            when(bookingRepository.findById(BookingId.of(BOOKING_UUID)))
                    .thenReturn(Optional.empty());

            Result<PaymentDetailResponse, PaymentError> result =
                    useCase.execute(new GetPaymentByIdQuery(PAYMENT_UUID, USER_UUID));

            assertThat(result.isSuccess()).isTrue();
            PaymentDetailResponse response =
                    ((Result.Success<PaymentDetailResponse, PaymentError>) result).value();
            assertThat(response.paymentId()).isEqualTo(PAYMENT_UUID);
            assertThat(response.booking()).isNull();
        }
    }

    @Nested
    @DisplayName("error cases")
    class ErrorCases {

        @Test
        @DisplayName("returns PaymentNotFound when payment missing")
        void execute_returnsPaymentNotFound_whenPaymentMissing() {
            when(paymentRepository.findSummaryById(PaymentId.of(PAYMENT_UUID)))
                    .thenReturn(Optional.empty());

            Result<PaymentDetailResponse, PaymentError> result =
                    useCase.execute(new GetPaymentByIdQuery(PAYMENT_UUID, USER_UUID));

            assertThat(result.isFailure()).isTrue();
            assertThat(((Result.Failure<?, PaymentError>) result).error())
                    .isInstanceOf(PaymentError.PaymentNotFound.class);
        }

        @Test
        @DisplayName("returns Forbidden when userId mismatch")
        void execute_returnsForbidden_whenUserIdMismatch() {
            when(paymentRepository.findSummaryById(PaymentId.of(PAYMENT_UUID)))
                    .thenReturn(Optional.of(paymentSummary(OTHER_USER_UUID)));

            Result<PaymentDetailResponse, PaymentError> result =
                    useCase.execute(new GetPaymentByIdQuery(PAYMENT_UUID, USER_UUID));

            assertThat(result.isFailure()).isTrue();
            assertThat(((Result.Failure<?, PaymentError>) result).error())
                    .isInstanceOf(PaymentError.Forbidden.class);
        }
    }
}

package io.github.phunguy65.ttbs.backend.payment.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.payment.application.helper.PaymentReadAuthorizer;
import io.github.phunguy65.ttbs.backend.payment.application.query.GetPaymentByBookingIdQuery;
import io.github.phunguy65.ttbs.backend.payment.application.response.PaymentResponse;
import io.github.phunguy65.ttbs.backend.payment.domain.error.PaymentError;
import io.github.phunguy65.ttbs.backend.payment.domain.model.PaymentStatus;
import io.github.phunguy65.ttbs.backend.payment.domain.projection.PaymentSummary;
import io.github.phunguy65.ttbs.backend.payment.domain.repository.PaymentRepository;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetPaymentByBookingIdUseCase")
class GetPaymentByBookingIdUseCaseTest {

    private static final UUID BOOKING_UUID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PAYMENT_UUID =
            UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentReadAuthorizer paymentReadAuthorizer;

    private GetPaymentByBookingIdUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetPaymentByBookingIdUseCase(paymentRepository, paymentReadAuthorizer);
    }

    private PaymentSummary paymentSummary() {
        return new PaymentSummary(
                PAYMENT_UUID,
                BOOKING_UUID,
                USER_UUID,
                "PENDING",
                "https://checkout.stripe.com/test",
                500_000L,
                "VND",
                null,
                Instant.now());
    }

    @Test
    @DisplayName(
            "delegates to PaymentReadAuthorizer.authorizeAndMap with correct bookingId and requestingUserId")
    void execute_delegatesToPaymentReadAuthorizer() {
        PaymentSummary summary = paymentSummary();
        when(paymentRepository.findSummaryByBookingId(BookingId.of(BOOKING_UUID)))
                .thenReturn(Optional.of(summary));
        when(paymentReadAuthorizer.authorizeAndMap(eq(summary), eq(USER_UUID)))
                .thenReturn(Result.success(new PaymentResponse(
                        PAYMENT_UUID,
                        BOOKING_UUID,
                        PaymentStatus.PENDING,
                        "https://checkout.stripe.com/test",
                        BigDecimal.valueOf(500_000L),
                        "VND")));

        Result<PaymentResponse, PaymentError> result =
                useCase.execute(new GetPaymentByBookingIdQuery(BOOKING_UUID, USER_UUID));

        verify(paymentReadAuthorizer).authorizeAndMap(eq(summary), eq(USER_UUID));
    }

    @Test
    @DisplayName("passes null summary to PaymentReadAuthorizer when payment not found")
    void execute_passesNullSummaryWhenPaymentNotFound() {
        when(paymentRepository.findSummaryByBookingId(BookingId.of(BOOKING_UUID)))
                .thenReturn(Optional.empty());
        when(paymentReadAuthorizer.authorizeAndMap(eq(null), eq(USER_UUID)))
                .thenReturn(Result.failure(new PaymentError.PaymentNotFound()));

        useCase.execute(new GetPaymentByBookingIdQuery(BOOKING_UUID, USER_UUID));

        verify(paymentReadAuthorizer).authorizeAndMap(eq(null), eq(USER_UUID));
    }

    @Test
    @DisplayName("returns success PaymentResponse when authorized")
    void execute_returnsSuccessPaymentResponse_whenAuthorized() {
        PaymentSummary summary = paymentSummary();
        PaymentResponse expectedResponse = new PaymentResponse(
                PAYMENT_UUID,
                BOOKING_UUID,
                PaymentStatus.PENDING,
                "https://checkout.stripe.com/test",
                BigDecimal.valueOf(500_000L),
                "VND");
        when(paymentRepository.findSummaryByBookingId(BookingId.of(BOOKING_UUID)))
                .thenReturn(Optional.of(summary));
        when(paymentReadAuthorizer.authorizeAndMap(eq(summary), eq(USER_UUID)))
                .thenReturn(Result.success(expectedResponse));

        Result<PaymentResponse, PaymentError> result =
                useCase.execute(new GetPaymentByBookingIdQuery(BOOKING_UUID, USER_UUID));

        assertThat(result.isSuccess()).isTrue();
        PaymentResponse actual = ((Result.Success<PaymentResponse, PaymentError>) result).value();
        assertThat(actual.paymentId()).isEqualTo(PAYMENT_UUID);
        assertThat(actual.bookingId()).isEqualTo(BOOKING_UUID);
        assertThat(actual.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(actual.amount()).isEqualTo(BigDecimal.valueOf(500_000L));
    }
}

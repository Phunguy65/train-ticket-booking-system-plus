package io.github.phunguy65.ttbs.backend.payment.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.payment.application.port.StripeGatewayPort;
import io.github.phunguy65.ttbs.backend.payment.domain.model.Payment;
import io.github.phunguy65.ttbs.backend.payment.domain.model.PaymentId;
import io.github.phunguy65.ttbs.backend.payment.domain.model.PaymentStatus;
import io.github.phunguy65.ttbs.backend.payment.domain.repository.PaymentRepository;
import io.github.phunguy65.ttbs.backend.shared.domain.Money;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class RefundPaymentUseCaseTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private StripeGatewayPort stripeGatewayPort;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private RefundPaymentUseCase useCase;

    private static final BookingId BOOKING_ID = BookingId.of(UUID.randomUUID());
    private static final UserId USER_ID = UserId.of(UUID.randomUUID());
    private static final String PAYMENT_INTENT_ID = "pi_test_456";

    @BeforeEach
    void setUp() {
        useCase = new RefundPaymentUseCase(paymentRepository, stripeGatewayPort, eventPublisher);
    }

    private Payment paidPayment() {
        Payment p = Payment.create(
                PaymentId.generate(),
                BOOKING_ID,
                USER_ID,
                Money.vnd(100_000L),
                "cs_test_123",
                "https://checkout.stripe.com/pay/cs_test_123");
        p.markPaid(PAYMENT_INTENT_ID, "evt_test_789");
        p.clearDomainEvents();
        return p;
    }

    @Test
    void execute_shouldIssueRefundWithCorrectIdempotencyKey() {
        Payment payment = paidPayment();
        when(paymentRepository.findByBookingId(BOOKING_ID)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(BOOKING_ID);

        verify(stripeGatewayPort)
                .createRefund(eq(PAYMENT_INTENT_ID), eq("refund_" + BOOKING_ID.value()));
        verify(paymentRepository).save(argThat(p -> p.getStatus() == PaymentStatus.REFUNDED));
    }

    @Test
    void execute_stripeException_shouldBeSwallowedAndNotRethrown() {
        Payment payment = paidPayment();
        when(paymentRepository.findByBookingId(BOOKING_ID)).thenReturn(Optional.of(payment));
        doThrow(new RuntimeException("Stripe network error"))
                .when(stripeGatewayPort)
                .createRefund(any(), any());

        // Must NOT throw — booking cancellation must not be blocked
        assertThatCode(() -> useCase.execute(BOOKING_ID)).doesNotThrowAnyException();

        // Payment should NOT be marked REFUNDED on failure
        verify(paymentRepository, never())
                .save(argThat(p -> p.getStatus() == PaymentStatus.REFUNDED));
    }

    @Test
    void execute_noPaymentFound_shouldDoNothing() {
        when(paymentRepository.findByBookingId(BOOKING_ID)).thenReturn(Optional.empty());

        useCase.execute(BOOKING_ID);

        verify(stripeGatewayPort, never()).createRefund(any(), any());
    }

    @Test
    void execute_paymentNotPaid_shouldSkipRefund() {
        Payment payment = Payment.create(
                PaymentId.generate(),
                BOOKING_ID,
                USER_ID,
                Money.vnd(100_000L),
                "cs_test_123",
                "https://checkout.stripe.com/pay/cs_test_123");
        // Still PENDING
        when(paymentRepository.findByBookingId(BOOKING_ID)).thenReturn(Optional.of(payment));

        useCase.execute(BOOKING_ID);

        verify(stripeGatewayPort, never()).createRefund(any(), any());
    }
}

package io.github.phunguy65.ttbs.backend.payment.application.usecase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.payment.application.command.RefundPaymentCommand;
import io.github.phunguy65.ttbs.backend.payment.application.port.StripeGatewayPort;
import io.github.phunguy65.ttbs.backend.payment.domain.model.Payment;
import io.github.phunguy65.ttbs.backend.payment.domain.model.PaymentId;
import io.github.phunguy65.ttbs.backend.payment.domain.repository.PaymentRepository;
import io.github.phunguy65.ttbs.backend.shared.domain.Money;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefundPaymentUseCase")
class RefundPaymentUseCaseTest {

    private static final UUID BOOKING_UUID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final BookingId BOOKING_ID = BookingId.of(BOOKING_UUID);
    private static final String PAYMENT_INTENT_ID = "pi_test_001";

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private StripeGatewayPort stripeGatewayPort;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private RefundPaymentUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RefundPaymentUseCase(paymentRepository, stripeGatewayPort, eventPublisher);
    }

    private Payment paidPayment() {
        Payment payment = Payment.create(
                PaymentId.of(UUID.randomUUID()),
                BOOKING_ID,
                UserId.of(UUID.randomUUID()),
                Money.vnd(500_000L),
                "cs_test_session",
                "https://checkout.stripe.com/test");
        payment.markPaid(PAYMENT_INTENT_ID, "evt_test");
        payment.clearDomainEvents();
        return payment;
    }

    private Payment pendingPayment() {
        return Payment.create(
                PaymentId.of(UUID.randomUUID()),
                BOOKING_ID,
                UserId.of(UUID.randomUUID()),
                Money.vnd(500_000L),
                "cs_test_session",
                "https://checkout.stripe.com/test");
    }

    @Nested
    @DisplayName("happy path")
    class HappyPath {

        @Test
        @DisplayName("calls stripeGatewayPort.createRefund with idempotencyKey and marks REFUNDED")
        void execute_callsCreateRefundAndMarksRefunded() {
            when(paymentRepository.findByBookingId(BOOKING_ID))
                    .thenReturn(Optional.of(paidPayment()));
            when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            useCase.execute(new RefundPaymentCommand(BOOKING_ID));

            verify(stripeGatewayPort)
                    .createRefund(eq(PAYMENT_INTENT_ID), eq("refund_" + BOOKING_UUID));
            verify(paymentRepository).save(any(Payment.class));
        }

        @Test
        @DisplayName("publishes PaymentRefunded event after marking refunded")
        void execute_publishesPaymentRefundedEvent() {
            when(paymentRepository.findByBookingId(BOOKING_ID))
                    .thenReturn(Optional.of(paidPayment()));
            when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            useCase.execute(new RefundPaymentCommand(BOOKING_ID));

            verify(eventPublisher).publishEvent(any(Object.class));
        }
    }

    @Nested
    @DisplayName("no-op cases")
    class NoOp {

        @Test
        @DisplayName("no-op when no payment found for bookingId")
        void execute_noOp_whenNoPaymentFound() {
            when(paymentRepository.findByBookingId(BOOKING_ID)).thenReturn(Optional.empty());

            useCase.execute(new RefundPaymentCommand(BOOKING_ID));

            verify(stripeGatewayPort, never()).createRefund(any(), any());
            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("no-op when payment is not PAID")
        void execute_noOp_whenPaymentIsNotPaid() {
            when(paymentRepository.findByBookingId(BOOKING_ID))
                    .thenReturn(Optional.of(pendingPayment()));

            useCase.execute(new RefundPaymentCommand(BOOKING_ID));

            verify(stripeGatewayPort, never()).createRefund(any(), any());
            verify(paymentRepository, never()).save(any());
        }
    }
}

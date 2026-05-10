package io.github.phunguy65.ttbs.backend.payment.application.usecase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.payment.application.command.HandlePaymentFailedByPaymentIntentCommand;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("HandlePaymentFailedByPaymentIntentUseCase")
class HandlePaymentFailedByPaymentIntentUseCaseTest {

    private static final String PAYMENT_INTENT_ID = "pi_test_001";
    private static final String STRIPE_EVENT_ID = "evt_test_001";
    private static final String ERROR_MESSAGE = "card_declined";

    @Mock
    private PaymentRepository paymentRepository;

    private HandlePaymentFailedByPaymentIntentUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new HandlePaymentFailedByPaymentIntentUseCase(paymentRepository);
    }

    private Payment pendingPayment() {
        return Payment.create(
                PaymentId.of(UUID.randomUUID()),
                BookingId.of(UUID.randomUUID()),
                UserId.of(UUID.randomUUID()),
                Money.vnd(500_000L),
                "cs_test_session",
                "https://checkout.stripe.com/test");
    }

    private Payment paidPayment() {
        Payment payment = pendingPayment();
        payment.markPaid(PAYMENT_INTENT_ID, "evt_other");
        payment.clearDomainEvents();
        return payment;
    }

    @Nested
    @DisplayName("happy path")
    class HappyPath {

        @Test
        @DisplayName("finds PENDING payment by paymentIntentId and marks FAILED")
        void execute_findsPendingPaymentAndMarksFailed() {
            when(paymentRepository.findByStripeEventId(STRIPE_EVENT_ID))
                    .thenReturn(Optional.empty());
            when(paymentRepository.findByStripePaymentIntentId(PAYMENT_INTENT_ID))
                    .thenReturn(Optional.of(pendingPayment()));
            when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            useCase.execute(new HandlePaymentFailedByPaymentIntentCommand(
                    PAYMENT_INTENT_ID, ERROR_MESSAGE, STRIPE_EVENT_ID));

            verify(paymentRepository).save(any(Payment.class));
        }
    }

    @Nested
    @DisplayName("idempotency — duplicate stripeEventId")
    class Idempotent {

        @Test
        @DisplayName("no-op when stripeEventId already processed")
        void execute_isIdempotent_whenStripeEventIdAlreadyProcessed() {
            when(paymentRepository.findByStripeEventId(STRIPE_EVENT_ID))
                    .thenReturn(Optional.of(pendingPayment()));

            useCase.execute(new HandlePaymentFailedByPaymentIntentCommand(
                    PAYMENT_INTENT_ID, ERROR_MESSAGE, STRIPE_EVENT_ID));

            verify(paymentRepository, never()).findByStripePaymentIntentId(any());
            verify(paymentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("no-op cases")
    class NoOp {

        @Test
        @DisplayName("no-op when no payment found for paymentIntentId")
        void execute_noOp_whenNoPaymentFound() {
            when(paymentRepository.findByStripeEventId(STRIPE_EVENT_ID))
                    .thenReturn(Optional.empty());
            when(paymentRepository.findByStripePaymentIntentId(PAYMENT_INTENT_ID))
                    .thenReturn(Optional.empty());

            useCase.execute(new HandlePaymentFailedByPaymentIntentCommand(
                    PAYMENT_INTENT_ID, ERROR_MESSAGE, STRIPE_EVENT_ID));

            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("no-op when payment is not PENDING")
        void execute_noOp_whenPaymentIsNotPending() {
            when(paymentRepository.findByStripeEventId(STRIPE_EVENT_ID))
                    .thenReturn(Optional.empty());
            when(paymentRepository.findByStripePaymentIntentId(PAYMENT_INTENT_ID))
                    .thenReturn(Optional.of(paidPayment()));

            useCase.execute(new HandlePaymentFailedByPaymentIntentCommand(
                    PAYMENT_INTENT_ID, ERROR_MESSAGE, STRIPE_EVENT_ID));

            verify(paymentRepository, never()).save(any());
        }
    }
}

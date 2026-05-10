package io.github.phunguy65.ttbs.backend.payment.application.usecase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.payment.application.command.CancelPendingPaymentCommand;
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
@DisplayName("CancelPendingPaymentUseCase")
class CancelPendingPaymentUseCaseTest {

    private static final String SESSION_ID = "cs_test_session_001";

    @Mock
    private PaymentRepository paymentRepository;

    private CancelPendingPaymentUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CancelPendingPaymentUseCase(paymentRepository);
    }

    private Payment pendingPayment() {
        return Payment.create(
                PaymentId.of(UUID.randomUUID()),
                BookingId.of(UUID.randomUUID()),
                UserId.of(UUID.randomUUID()),
                Money.vnd(500_000L),
                SESSION_ID,
                "https://checkout.stripe.com/test");
    }

    private Payment paidPayment() {
        Payment payment = pendingPayment();
        payment.markPaid("pi_test", "evt_test");
        payment.clearDomainEvents();
        return payment;
    }

    @Nested
    @DisplayName("happy path")
    class HappyPath {

        @Test
        @DisplayName("marks PENDING payment as CANCELLED")
        void execute_marksPendingPaymentAsCancelled() {
            when(paymentRepository.findByCheckoutSessionId(SESSION_ID))
                    .thenReturn(Optional.of(pendingPayment()));
            when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            useCase.execute(new CancelPendingPaymentCommand(SESSION_ID));

            verify(paymentRepository).save(any(Payment.class));
        }
    }

    @Nested
    @DisplayName("no-op cases")
    class NoOp {

        @Test
        @DisplayName("no-op when no payment found")
        void execute_noOp_whenNoPaymentFound() {
            when(paymentRepository.findByCheckoutSessionId(SESSION_ID))
                    .thenReturn(Optional.empty());

            useCase.execute(new CancelPendingPaymentCommand(SESSION_ID));

            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("no-op when payment is not PENDING")
        void execute_noOp_whenPaymentIsNotPending() {
            when(paymentRepository.findByCheckoutSessionId(SESSION_ID))
                    .thenReturn(Optional.of(paidPayment()));

            useCase.execute(new CancelPendingPaymentCommand(SESSION_ID));

            verify(paymentRepository, never()).save(any());
        }
    }
}

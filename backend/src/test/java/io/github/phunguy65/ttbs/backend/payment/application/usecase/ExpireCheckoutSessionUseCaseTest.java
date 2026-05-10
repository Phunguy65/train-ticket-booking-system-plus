package io.github.phunguy65.ttbs.backend.payment.application.usecase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.payment.application.command.ExpireCheckoutSessionCommand;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("ExpireCheckoutSessionUseCase")
class ExpireCheckoutSessionUseCaseTest {

    private static final UUID BOOKING_UUID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final BookingId BOOKING_ID = BookingId.of(BOOKING_UUID);
    private static final String SESSION_ID = "cs_test_session_001";

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private StripeGatewayPort stripeGatewayPort;

    private ExpireCheckoutSessionUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ExpireCheckoutSessionUseCase(paymentRepository, stripeGatewayPort);
    }

    private Payment pendingPayment() {
        return Payment.create(
                PaymentId.of(UUID.randomUUID()),
                BOOKING_ID,
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
        @DisplayName("calls stripeGatewayPort.expireCheckoutSession and marks CANCELLED")
        void execute_expiresSessionAndMarksCancelled() {
            when(paymentRepository.findByBookingId(BOOKING_ID))
                    .thenReturn(Optional.of(pendingPayment()));
            when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            useCase.execute(new ExpireCheckoutSessionCommand(BOOKING_ID));

            verify(stripeGatewayPort).expireCheckoutSession(SESSION_ID);
            verify(paymentRepository).save(any(Payment.class));
        }
    }

    @Nested
    @DisplayName("no-op cases")
    class NoOp {

        @Test
        @DisplayName("no-op when no payment found for bookingId")
        void execute_noOp_whenNoPaymentFound() {
            when(paymentRepository.findByBookingId(BOOKING_ID)).thenReturn(Optional.empty());

            useCase.execute(new ExpireCheckoutSessionCommand(BOOKING_ID));

            verify(stripeGatewayPort, never()).expireCheckoutSession(any());
            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("no-op when payment is not PENDING")
        void execute_noOp_whenPaymentIsNotPending() {
            when(paymentRepository.findByBookingId(BOOKING_ID))
                    .thenReturn(Optional.of(paidPayment()));

            useCase.execute(new ExpireCheckoutSessionCommand(BOOKING_ID));

            verify(stripeGatewayPort, never()).expireCheckoutSession(any());
            verify(paymentRepository, never()).save(any());
        }
    }
}

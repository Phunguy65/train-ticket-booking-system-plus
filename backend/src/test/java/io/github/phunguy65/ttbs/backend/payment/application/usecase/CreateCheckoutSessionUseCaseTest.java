package io.github.phunguy65.ttbs.backend.payment.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.payment.application.command.CreateCheckoutSessionCommand;
import io.github.phunguy65.ttbs.backend.payment.application.port.StripeGatewayPort;
import io.github.phunguy65.ttbs.backend.payment.domain.model.Payment;
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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CreateCheckoutSessionUseCaseTest {

    @Mock
    private StripeGatewayPort stripeGatewayPort;

    @Mock
    private PaymentRepository paymentRepository;

    private CreateCheckoutSessionUseCase useCase;

    private static final BookingId BOOKING_ID = BookingId.of(UUID.randomUUID());
    private static final UserId USER_ID = UserId.of(UUID.randomUUID());
    private static final Money AMOUNT = Money.vnd(100_000L);
    private static final String CURRENCY = "VND";

    @BeforeEach
    void setUp() {
        useCase = new CreateCheckoutSessionUseCase(stripeGatewayPort, paymentRepository);
        ReflectionTestUtils.setField(useCase, "successUrl", "https://example.com/success");
        ReflectionTestUtils.setField(useCase, "cancelUrl", "https://example.com/cancel");
    }

    @Test
    void execute_shouldCreateSessionAndSavePaymentAsPending() {
        when(paymentRepository.findByBookingId(BOOKING_ID)).thenReturn(Optional.empty());
        when(stripeGatewayPort.createCheckoutSession(any(CreateCheckoutSessionCommand.class)))
                .thenReturn(new StripeGatewayPort.CheckoutSessionResult(
                        "cs_test_123", "https://checkout.stripe.com/pay/cs_test_123"));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(BOOKING_ID, USER_ID, AMOUNT, CURRENCY);

        verify(stripeGatewayPort).createCheckoutSession(any());
        verify(paymentRepository)
                .save(argThat(p -> p.getStatus() == PaymentStatus.PENDING
                        && p.getCheckoutSessionId().equals("cs_test_123")
                        && p.getBookingId().equals(BOOKING_ID)));
    }

    @Test
    void execute_idempotency_shouldSkipIfPaymentAlreadyExists() {
        Payment existing = Payment.create(
                io.github.phunguy65.ttbs.backend.payment.domain.model.PaymentId.generate(),
                BOOKING_ID,
                USER_ID,
                AMOUNT,
                "cs_existing",
                "https://checkout.stripe.com/existing");
        when(paymentRepository.findByBookingId(BOOKING_ID)).thenReturn(Optional.of(existing));

        useCase.execute(BOOKING_ID, USER_ID, AMOUNT, CURRENCY);

        verify(stripeGatewayPort, never()).createCheckoutSession(any());
        verify(paymentRepository, never()).save(any());
    }
}

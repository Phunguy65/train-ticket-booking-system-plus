package io.github.phunguy65.ttbs.backend.payment.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingStatus;
import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import io.github.phunguy65.ttbs.backend.payment.application.port.StripeGatewayPort;
import io.github.phunguy65.ttbs.backend.payment.domain.model.Payment;
import io.github.phunguy65.ttbs.backend.payment.domain.model.PaymentId;
import io.github.phunguy65.ttbs.backend.payment.domain.model.PaymentStatus;
import io.github.phunguy65.ttbs.backend.payment.domain.repository.PaymentRepository;
import io.github.phunguy65.ttbs.backend.shared.domain.Money;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.port.RouteSeatAvailabilityPort;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class HandlePaymentSuccessUseCaseTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private RouteSeatAvailabilityPort seatAvailabilityPort;

    @Mock
    private StripeGatewayPort stripeGatewayPort;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private HandlePaymentSuccessUseCase useCase;

    private static final BookingId BOOKING_ID = BookingId.of(UUID.randomUUID());
    private static final UserId USER_ID = UserId.of(UUID.randomUUID());
    private static final RouteId ROUTE_ID = RouteId.of(UUID.randomUUID());
    private static final String SESSION_ID = "cs_test_123";
    private static final String PAYMENT_INTENT_ID = "pi_test_456";
    private static final String EVENT_ID = "evt_test_789";

    @BeforeEach
    void setUp() {
        useCase = new HandlePaymentSuccessUseCase(
                paymentRepository,
                bookingRepository,
                seatAvailabilityPort,
                stripeGatewayPort,
                eventPublisher);
    }

    private Payment pendingPayment() {
        return Payment.create(
                PaymentId.generate(),
                BOOKING_ID,
                USER_ID,
                Money.vnd(100_000L),
                SESSION_ID,
                "https://checkout.stripe.com/pay/" + SESSION_ID);
    }

    private Booking heldBooking() {
        return Booking.reconstitute(
                BOOKING_ID,
                USER_ID,
                ROUTE_ID,
                "John Doe",
                "john@example.com",
                null,
                Money.vnd(100_000L),
                "VND",
                BookingStatus.HELD,
                "idem-key",
                Instant.now().plusSeconds(900),
                Instant.now());
    }

    private Booking cancelledBooking() {
        return Booking.reconstitute(
                BOOKING_ID,
                USER_ID,
                ROUTE_ID,
                "John Doe",
                "john@example.com",
                null,
                Money.vnd(100_000L),
                "VND",
                BookingStatus.CANCELLED,
                "idem-key",
                Instant.now().minusSeconds(60),
                Instant.now());
    }

    @Test
    void execute_happyPath_shouldConfirmBookingAndSeatsAndMarkPaid() {
        Payment payment = pendingPayment();
        Booking booking = heldBooking();

        when(paymentRepository.findByStripeEventId(EVENT_ID)).thenReturn(Optional.empty());
        when(paymentRepository.findByCheckoutSessionId(SESSION_ID))
                .thenReturn(Optional.of(payment));
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(seatAvailabilityPort.confirmHeldSeats(BOOKING_ID.value()))
                .thenReturn(Result.success());
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(SESSION_ID, PAYMENT_INTENT_ID, EVENT_ID);

        verify(bookingRepository).save(argThat(b -> b.getStatus() == BookingStatus.CONFIRMED));
        verify(seatAvailabilityPort).confirmHeldSeats(BOOKING_ID.value());
        verify(paymentRepository)
                .save(argThat(p -> p.getStatus() == PaymentStatus.PAID
                        && PAYMENT_INTENT_ID.equals(p.getStripePaymentIntentId())));
    }

    @Test
    void execute_latePayment_shouldRefundImmediately() {
        Payment payment = pendingPayment();
        Booking booking = cancelledBooking();

        when(paymentRepository.findByStripeEventId(EVENT_ID)).thenReturn(Optional.empty());
        when(paymentRepository.findByCheckoutSessionId(SESSION_ID))
                .thenReturn(Optional.of(payment));
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(SESSION_ID, PAYMENT_INTENT_ID, EVENT_ID);

        verify(stripeGatewayPort)
                .createRefund(eq(PAYMENT_INTENT_ID), contains(BOOKING_ID.value().toString()));
        verify(paymentRepository).save(argThat(p -> p.getStatus() == PaymentStatus.REFUNDED));
        verify(seatAvailabilityPort, never()).confirmHeldSeats(any());
    }

    @Test
    void execute_idempotency_shouldSkipIfEventAlreadyProcessed() {
        Payment existing = pendingPayment();
        existing.markPaid(PAYMENT_INTENT_ID, EVENT_ID);
        when(paymentRepository.findByStripeEventId(EVENT_ID)).thenReturn(Optional.of(existing));

        useCase.execute(SESSION_ID, PAYMENT_INTENT_ID, EVENT_ID);

        verify(paymentRepository, never()).findByCheckoutSessionId(any());
        verify(bookingRepository, never()).findById(any());
    }
}

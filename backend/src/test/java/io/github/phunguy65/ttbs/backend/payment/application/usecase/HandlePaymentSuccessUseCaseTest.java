package io.github.phunguy65.ttbs.backend.payment.application.usecase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingStatus;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingUserInfo;
import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import io.github.phunguy65.ttbs.backend.payment.application.command.HandlePaymentSuccessCommand;
import io.github.phunguy65.ttbs.backend.payment.application.port.StripeGatewayPort;
import io.github.phunguy65.ttbs.backend.payment.domain.model.Payment;
import io.github.phunguy65.ttbs.backend.payment.domain.model.PaymentId;
import io.github.phunguy65.ttbs.backend.payment.domain.repository.PaymentRepository;
import io.github.phunguy65.ttbs.backend.shared.domain.Money;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.port.RouteSeatAvailabilityManager;
import io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripId;
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
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("HandlePaymentSuccessUseCase")
class HandlePaymentSuccessUseCaseTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID BOOKING_UUID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PAYMENT_UUID =
            UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID TRIP_UUID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final BookingId BOOKING_ID = BookingId.of(BOOKING_UUID);
    private static final String SESSION_ID = "cs_test_session_001";
    private static final String PAYMENT_INTENT_ID = "pi_test_001";
    private static final String STRIPE_EVENT_ID = "evt_test_001";

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private RouteSeatAvailabilityManager seatAvailabilityPort;

    @Mock
    private StripeGatewayPort stripeGatewayPort;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private HandlePaymentSuccessUseCase useCase;

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
                PaymentId.of(PAYMENT_UUID),
                BOOKING_ID,
                UserId.of(USER_ID),
                Money.vnd(500_000L),
                SESSION_ID,
                "https://checkout.stripe.com/test");
    }

    private Booking heldBooking() {
        return Booking.reconstitute(
                BOOKING_ID,
                UserId.of(USER_ID),
                ScheduledTripId.of(TRIP_UUID),
                BookingUserInfo.of("Nguyen Van A", "a@b.com", null, null, null, null, null),
                List.of(),
                Money.vnd(500_000L),
                BookingStatus.HELD,
                "idem-key-1",
                Instant.now().plusSeconds(900),
                Instant.now().minusSeconds(60));
    }

    private Booking cancelledBooking() {
        return Booking.reconstitute(
                BOOKING_ID,
                UserId.of(USER_ID),
                ScheduledTripId.of(TRIP_UUID),
                BookingUserInfo.of("Nguyen Van A", "a@b.com", null, null, null, null, null),
                List.of(),
                Money.vnd(500_000L),
                BookingStatus.CANCELLED,
                "idem-key-1",
                Instant.now().plusSeconds(900),
                Instant.now().minusSeconds(60));
    }

    @Nested
    @DisplayName("happy path — HELD booking")
    class HappyPath {

        @Test
        @DisplayName(
                "confirms booking, confirms held seats, marks payment PAID and publishes events")
        void execute_confirmsBookingAndSeatsAndMarksPaymentPaid() {
            when(paymentRepository.findByStripeEventId(STRIPE_EVENT_ID))
                    .thenReturn(Optional.empty());
            when(paymentRepository.findByCheckoutSessionId(SESSION_ID))
                    .thenReturn(Optional.of(pendingPayment()));
            when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(heldBooking()));
            when(seatAvailabilityPort.confirmHeldSeats(BOOKING_UUID)).thenReturn(Result.success());
            when(seatAvailabilityPort.findByBookingId(BOOKING_UUID)).thenReturn(List.of());
            when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            useCase.execute(new HandlePaymentSuccessCommand(
                    SESSION_ID, PAYMENT_INTENT_ID, STRIPE_EVENT_ID));

            verify(bookingRepository).save(any(Booking.class));
            verify(seatAvailabilityPort).confirmHeldSeats(BOOKING_UUID);
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

            useCase.execute(new HandlePaymentSuccessCommand(
                    SESSION_ID, PAYMENT_INTENT_ID, STRIPE_EVENT_ID));

            verify(paymentRepository, never()).findByCheckoutSessionId(any());
            verify(bookingRepository, never()).findById(any());
        }
    }

    @Nested
    @DisplayName("late payment — booking CANCELLED")
    class LatePayment {

        @Test
        @DisplayName("triggers immediate refund and marks payment REFUNDED")
        void execute_latePayment_triggersRefundAndMarksRefunded() {
            when(paymentRepository.findByStripeEventId(STRIPE_EVENT_ID))
                    .thenReturn(Optional.empty());
            when(paymentRepository.findByCheckoutSessionId(SESSION_ID))
                    .thenReturn(Optional.of(pendingPayment()));
            when(bookingRepository.findById(BOOKING_ID))
                    .thenReturn(Optional.of(cancelledBooking()));
            when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            useCase.execute(new HandlePaymentSuccessCommand(
                    SESSION_ID, PAYMENT_INTENT_ID, STRIPE_EVENT_ID));

            verify(stripeGatewayPort).createRefund(any(), any());
            verify(paymentRepository).save(any(Payment.class));
            verify(seatAvailabilityPort, never()).confirmHeldSeats(any());
        }
    }
}

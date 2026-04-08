package io.github.phunguy65.ttbs.backend.payment.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingStatus;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingUserInfo;
import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import io.github.phunguy65.ttbs.backend.payment.application.command.CreateCheckoutSessionCommand;
import io.github.phunguy65.ttbs.backend.payment.application.port.StripeGatewayPort;
import io.github.phunguy65.ttbs.backend.payment.application.response.CreateCheckoutResult;
import io.github.phunguy65.ttbs.backend.payment.domain.error.PaymentError;
import io.github.phunguy65.ttbs.backend.payment.domain.model.Payment;
import io.github.phunguy65.ttbs.backend.payment.domain.model.PaymentStatus;
import io.github.phunguy65.ttbs.backend.payment.domain.repository.PaymentRepository;
import io.github.phunguy65.ttbs.backend.shared.domain.Money;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
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
@DisplayName("CreateCheckoutSessionUseCase")
class CreateCheckoutSessionUseCaseTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_USER_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID BOOKING_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID SCHEDULED_TRIP_ID =
            UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private StripeGatewayPort stripeGatewayPort;

    @Mock
    private PaymentRepository paymentRepository;

    private CreateCheckoutSessionUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateCheckoutSessionUseCase(
                bookingRepository, stripeGatewayPort, paymentRepository);
    }

    private CreateCheckoutSessionCommand command() {
        return new CreateCheckoutSessionCommand(BookingId.of(BOOKING_ID), UserId.of(USER_ID));
    }

    private Booking heldBooking() {
        return Booking.reconstitute(
                BookingId.of(BOOKING_ID),
                UserId.of(USER_ID),
                io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripId.of(
                        SCHEDULED_TRIP_ID),
                new BookingUserInfo(
                        "Nguyen Van A", "a@b.com", "0900000000", null, null, null, null),
                Money.vnd(450_000),
                BookingStatus.HELD,
                "idem-key-1",
                Instant.now().plusSeconds(900),
                Instant.now());
    }

    private Booking confirmedBooking() {
        return Booking.reconstitute(
                BookingId.of(BOOKING_ID),
                UserId.of(USER_ID),
                io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripId.of(
                        SCHEDULED_TRIP_ID),
                new BookingUserInfo(
                        "Nguyen Van A", "a@b.com", "0900000000", null, null, null, null),
                Money.vnd(450_000),
                BookingStatus.CONFIRMED,
                "idem-key-1",
                Instant.now().plusSeconds(900),
                Instant.now());
    }

    private Booking cancelledBooking() {
        return Booking.reconstitute(
                BookingId.of(BOOKING_ID),
                UserId.of(USER_ID),
                io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripId.of(
                        SCHEDULED_TRIP_ID),
                new BookingUserInfo(
                        "Nguyen Van A", "a@b.com", "0900000000", null, null, null, null),
                Money.vnd(450_000),
                BookingStatus.CANCELLED,
                "idem-key-1",
                Instant.now().plusSeconds(900),
                Instant.now());
    }

    private Booking expiredDeadlineBooking() {
        return Booking.reconstitute(
                BookingId.of(BOOKING_ID),
                UserId.of(USER_ID),
                io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripId.of(
                        SCHEDULED_TRIP_ID),
                new BookingUserInfo(
                        "Nguyen Van A", "a@b.com", "0900000000", null, null, null, null),
                Money.vnd(450_000),
                BookingStatus.HELD,
                "idem-key-1",
                Instant.now().minusSeconds(60),
                Instant.now().minusSeconds(960));
    }

    private Booking bookingOwnedByOtherUser() {
        return Booking.reconstitute(
                BookingId.of(BOOKING_ID),
                UserId.of(OTHER_USER_ID),
                io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripId.of(
                        SCHEDULED_TRIP_ID),
                new BookingUserInfo(
                        "Nguyen Van B", "b@b.com", "0900000001", null, null, null, null),
                Money.vnd(450_000),
                BookingStatus.HELD,
                "idem-key-2",
                Instant.now().plusSeconds(900),
                Instant.now());
    }

    private Payment pendingPayment() {
        return Payment.create(
                io.github.phunguy65.ttbs.backend.payment.domain.model.PaymentId.of(
                        UUID.fromString("55555555-5555-5555-5555-555555555555")),
                BookingId.of(BOOKING_ID),
                UserId.of(USER_ID),
                Money.vnd(450_000),
                "cs_test_123",
                "https://checkout.stripe.com/session/test");
    }

    private Payment paidPayment() {
        Payment p = pendingPayment();
        p.markPaid("pi_test_123", "evt_test_123");
        p.clearDomainEvents();
        return p;
    }

    private Payment failedPayment() {
        Payment p = pendingPayment();
        p.markFailed("Card declined", "evt_test_fail");
        return p;
    }

    private void stubStripeSuccess() {
        when(stripeGatewayPort.createCheckoutSession(any()))
                .thenReturn(new StripeGatewayPort.CheckoutSessionResult(
                        "cs_new_123", "https://checkout.stripe.com/session/new"));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("happy path: creates checkout session for HELD booking with no existing payment")
    void happyPath_createsCheckoutSession() {
        when(bookingRepository.findById(BookingId.of(BOOKING_ID)))
                .thenReturn(Optional.of(heldBooking()));
        when(paymentRepository.findByBookingId(BookingId.of(BOOKING_ID)))
                .thenReturn(Optional.empty());
        stubStripeSuccess();

        Result<CreateCheckoutResult, PaymentError> result = useCase.execute(command());

        assertThat(result.isSuccess()).isTrue();
        CreateCheckoutResult checkout = ((Result.Success<CreateCheckoutResult, ?>) result).value();
        assertThat(checkout.created()).isTrue();
        assertThat(checkout.response().checkoutUrl())
                .isEqualTo("https://checkout.stripe.com/session/new");
        assertThat(checkout.response().status()).isEqualTo(PaymentStatus.PENDING);
        verify(stripeGatewayPort).createCheckoutSession(any());
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("returns BookingNotFound when booking does not exist")
    void bookingNotFound() {
        when(bookingRepository.findById(BookingId.of(BOOKING_ID))).thenReturn(Optional.empty());

        Result<CreateCheckoutResult, PaymentError> result = useCase.execute(command());

        assertThat(result.isFailure()).isTrue();
        PaymentError error = ((Result.Failure<?, PaymentError>) result).error();
        assertThat(error).isInstanceOf(PaymentError.BookingNotFound.class);
        verify(stripeGatewayPort, never()).createCheckoutSession(any());
    }

    @Test
    @DisplayName("returns Forbidden when user does not own the booking")
    void forbidden_differentUser() {
        when(bookingRepository.findById(BookingId.of(BOOKING_ID)))
                .thenReturn(Optional.of(bookingOwnedByOtherUser()));

        Result<CreateCheckoutResult, PaymentError> result = useCase.execute(command());

        assertThat(result.isFailure()).isTrue();
        PaymentError error = ((Result.Failure<?, PaymentError>) result).error();
        assertThat(error).isInstanceOf(PaymentError.Forbidden.class);
        verify(stripeGatewayPort, never()).createCheckoutSession(any());
    }

    @Test
    @DisplayName("returns InvalidBookingState when booking is CONFIRMED")
    void invalidState_confirmed() {
        when(bookingRepository.findById(BookingId.of(BOOKING_ID)))
                .thenReturn(Optional.of(confirmedBooking()));

        Result<CreateCheckoutResult, PaymentError> result = useCase.execute(command());

        assertThat(result.isFailure()).isTrue();
        PaymentError error = ((Result.Failure<?, PaymentError>) result).error();
        assertThat(error).isInstanceOf(PaymentError.InvalidBookingState.class);
    }

    @Test
    @DisplayName("returns InvalidBookingState when booking is CANCELLED")
    void invalidState_cancelled() {
        when(bookingRepository.findById(BookingId.of(BOOKING_ID)))
                .thenReturn(Optional.of(cancelledBooking()));

        Result<CreateCheckoutResult, PaymentError> result = useCase.execute(command());

        assertThat(result.isFailure()).isTrue();
        PaymentError error = ((Result.Failure<?, PaymentError>) result).error();
        assertThat(error).isInstanceOf(PaymentError.InvalidBookingState.class);
    }

    @Test
    @DisplayName("returns InvalidBookingState when payment deadline has expired")
    void invalidState_expiredDeadline() {
        when(bookingRepository.findById(BookingId.of(BOOKING_ID)))
                .thenReturn(Optional.of(expiredDeadlineBooking()));

        Result<CreateCheckoutResult, PaymentError> result = useCase.execute(command());

        assertThat(result.isFailure()).isTrue();
        PaymentError error = ((Result.Failure<?, PaymentError>) result).error();
        assertThat(error).isInstanceOf(PaymentError.InvalidBookingState.class);
        assertThat(error.message()).isEqualTo("Payment deadline has expired");
    }

    @Test
    @DisplayName("accepts booking when payment deadline is null")
    void validState_nullPaymentDeadline() {
        Booking booking = Booking.reconstitute(
                BookingId.of(BOOKING_ID),
                UserId.of(USER_ID),
                io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripId.of(
                        SCHEDULED_TRIP_ID),
                new BookingUserInfo(
                        "Nguyen Van A", "a@b.com", "0900000000", null, null, null, null),
                Money.vnd(450_000),
                BookingStatus.HELD,
                "idem-key-null-dl",
                null,
                Instant.now());
        when(bookingRepository.findById(BookingId.of(BOOKING_ID))).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingId(BookingId.of(BOOKING_ID)))
                .thenReturn(Optional.empty());
        stubStripeSuccess();

        Result<CreateCheckoutResult, PaymentError> result = useCase.execute(command());

        assertThat(result.isSuccess()).isTrue();
        verify(stripeGatewayPort).createCheckoutSession(any());
    }

    @Test
    @DisplayName("returns existing PENDING payment (idempotent)")
    void idempotent_existingPendingPayment() {
        when(bookingRepository.findById(BookingId.of(BOOKING_ID)))
                .thenReturn(Optional.of(heldBooking()));
        when(paymentRepository.findByBookingId(BookingId.of(BOOKING_ID)))
                .thenReturn(Optional.of(pendingPayment()));

        Result<CreateCheckoutResult, PaymentError> result = useCase.execute(command());

        assertThat(result.isSuccess()).isTrue();
        CreateCheckoutResult checkout = ((Result.Success<CreateCheckoutResult, ?>) result).value();
        assertThat(checkout.created()).isFalse();
        assertThat(checkout.response().checkoutUrl())
                .isEqualTo("https://checkout.stripe.com/session/test");
        verify(stripeGatewayPort, never()).createCheckoutSession(any());
    }

    @Test
    @DisplayName("returns AlreadyProcessed when existing payment is PAID")
    void alreadyProcessed_paidPayment() {
        when(bookingRepository.findById(BookingId.of(BOOKING_ID)))
                .thenReturn(Optional.of(heldBooking()));
        when(paymentRepository.findByBookingId(BookingId.of(BOOKING_ID)))
                .thenReturn(Optional.of(paidPayment()));

        Result<CreateCheckoutResult, PaymentError> result = useCase.execute(command());

        assertThat(result.isFailure()).isTrue();
        PaymentError error = ((Result.Failure<?, PaymentError>) result).error();
        assertThat(error).isInstanceOf(PaymentError.AlreadyProcessed.class);
    }

    @Test
    @DisplayName("creates new checkout session when existing payment is FAILED")
    void createsNew_whenExistingPaymentFailed() {
        when(bookingRepository.findById(BookingId.of(BOOKING_ID)))
                .thenReturn(Optional.of(heldBooking()));
        when(paymentRepository.findByBookingId(BookingId.of(BOOKING_ID)))
                .thenReturn(Optional.of(failedPayment()));
        stubStripeSuccess();

        Result<CreateCheckoutResult, PaymentError> result = useCase.execute(command());

        assertThat(result.isSuccess()).isTrue();
        CreateCheckoutResult checkout = ((Result.Success<CreateCheckoutResult, ?>) result).value();
        assertThat(checkout.created()).isTrue();
        verify(stripeGatewayPort).createCheckoutSession(any());
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("creates new checkout session when existing payment is CANCELLED")
    void createsNew_whenExistingPaymentCancelled() {
        when(bookingRepository.findById(BookingId.of(BOOKING_ID)))
                .thenReturn(Optional.of(heldBooking()));
        Payment cancelled = pendingPayment();
        cancelled.markCancelled();
        when(paymentRepository.findByBookingId(BookingId.of(BOOKING_ID)))
                .thenReturn(Optional.of(cancelled));
        stubStripeSuccess();

        Result<CreateCheckoutResult, PaymentError> result = useCase.execute(command());

        assertThat(result.isSuccess()).isTrue();
        CreateCheckoutResult checkout = ((Result.Success<CreateCheckoutResult, ?>) result).value();
        assertThat(checkout.created()).isTrue();
        verify(stripeGatewayPort).createCheckoutSession(any());
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("returns AlreadyProcessed when existing payment is REFUNDED")
    void alreadyProcessed_refundedPayment() {
        when(bookingRepository.findById(BookingId.of(BOOKING_ID)))
                .thenReturn(Optional.of(heldBooking()));
        Payment refunded = paidPayment();
        refunded.markRefunded();
        refunded.clearDomainEvents();
        when(paymentRepository.findByBookingId(BookingId.of(BOOKING_ID)))
                .thenReturn(Optional.of(refunded));

        Result<CreateCheckoutResult, PaymentError> result = useCase.execute(command());

        assertThat(result.isFailure()).isTrue();
        PaymentError error = ((Result.Failure<?, PaymentError>) result).error();
        assertThat(error).isInstanceOf(PaymentError.AlreadyProcessed.class);
        verify(stripeGatewayPort, never()).createCheckoutSession(any());
    }
}

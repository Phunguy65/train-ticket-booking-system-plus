package io.github.phunguy65.ttbs.backend.payment.domain.model;

import static org.assertj.core.api.Assertions.*;

import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.payment.domain.event.PaymentCompleted;
import io.github.phunguy65.ttbs.backend.payment.domain.event.PaymentRefunded;
import io.github.phunguy65.ttbs.backend.shared.domain.Money;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentTest {

    private static final PaymentId PAYMENT_ID = PaymentId.of(UUID.randomUUID());
    private static final BookingId BOOKING_ID = BookingId.of(UUID.randomUUID());
    private static final UserId USER_ID = UserId.of(UUID.randomUUID());
    private static final Money AMOUNT = Money.vnd(100_000L);
    private static final String SESSION_ID = "cs_test_123";
    private static final String CHECKOUT_URL = "https://checkout.stripe.com/pay/cs_test_123";

    private Payment newPendingPayment() {
        return Payment.create(PAYMENT_ID, BOOKING_ID, USER_ID, AMOUNT, SESSION_ID, CHECKOUT_URL);
    }

    @Test
    void create_shouldInitializeWithPendingStatus() {
        Payment payment = newPendingPayment();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getPaymentId()).isEqualTo(PAYMENT_ID);
        assertThat(payment.getBookingId()).isEqualTo(BOOKING_ID);
        assertThat(payment.getUserId()).isEqualTo(USER_ID);
        assertThat(payment.getCheckoutSessionId()).isEqualTo(SESSION_ID);
        assertThat(payment.getCheckoutUrl()).isEqualTo(CHECKOUT_URL);
    }

    @Test
    void create_shouldNotRegisterAnyEvents() {
        Payment payment = newPendingPayment();

        assertThat(payment.getDomainEvents()).isEmpty();
    }

    @Test
    void markPaid_shouldTransitionToPaymentPaidAndRegisterEvent() {
        Payment payment = newPendingPayment();

        payment.markPaid("pi_test_456", "evt_test_789");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getStripePaymentIntentId()).isEqualTo("pi_test_456");
        assertThat(payment.getStripeEventId()).isEqualTo("evt_test_789");
        assertThat(payment.getDomainEvents()).hasSize(1);
        assertThat(payment.getDomainEvents().getFirst()).isInstanceOf(PaymentCompleted.class);
    }

    @Test
    void markCancelled_shouldTransitionToCancelled() {
        Payment payment = newPendingPayment();

        payment.markCancelled();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(payment.getDomainEvents()).isEmpty();
    }

    @Test
    void markFailed_shouldTransitionToFailedWithErrorMessage() {
        Payment payment = newPendingPayment();

        payment.markFailed("Card declined", "evt_fail_001");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getErrorMessage()).isEqualTo("Card declined");
        assertThat(payment.getStripeEventId()).isEqualTo("evt_fail_001");
    }

    @Test
    void markRefunded_shouldTransitionToRefundedAndRegisterEvent() {
        Payment payment = newPendingPayment();
        payment.markPaid("pi_test_456", "evt_test_789");
        payment.clearDomainEvents();

        payment.markRefunded();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(payment.getDomainEvents()).hasSize(1);
        assertThat(payment.getDomainEvents().getFirst()).isInstanceOf(PaymentRefunded.class);
    }

    @Test
    void reconstitute_shouldNotRegisterEvents() {
        Payment payment = Payment.reconstitute(
                PAYMENT_ID,
                BOOKING_ID,
                USER_ID,
                AMOUNT,
                PaymentStatus.PAID,
                SESSION_ID,
                CHECKOUT_URL,
                "pi_test_456",
                "evt_test_789",
                null,
                java.time.Instant.now().minusSeconds(60),
                java.time.Instant.now());

        assertThat(payment.getDomainEvents()).isEmpty();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
    }
}

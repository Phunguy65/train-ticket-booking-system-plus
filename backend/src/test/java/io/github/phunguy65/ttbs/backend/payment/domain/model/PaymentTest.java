package io.github.phunguy65.ttbs.backend.payment.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.payment.domain.event.PaymentCompleted;
import io.github.phunguy65.ttbs.backend.payment.domain.event.PaymentRefunded;
import io.github.phunguy65.ttbs.backend.shared.domain.Money;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Payment")
class PaymentTest {

    private static final PaymentId PAYMENT_ID =
            PaymentId.of(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
    private static final BookingId BOOKING_ID =
            BookingId.of(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));
    private static final UserId USER_ID =
            UserId.of(UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"));
    private static final Money AMOUNT = Money.vnd(650_000L);
    private static final String SESSION_ID = "cs_test_session_001";
    private static final String CHECKOUT_URL = "https://checkout.stripe.com/test";
    private static final String PAYMENT_INTENT_ID = "pi_test_intent_001";
    private static final String STRIPE_EVENT_ID = "evt_test_001";

    private static Payment createPendingPayment() {
        return Payment.create(PAYMENT_ID, BOOKING_ID, USER_ID, AMOUNT, SESSION_ID, CHECKOUT_URL);
    }

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("returns PENDING status")
        void create_returnsPendingStatus() {
            Payment payment = createPendingPayment();

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(payment.getDomainEvents()).isEmpty();
        }
    }

    @Nested
    @DisplayName("markPaid()")
    class MarkPaid {

        @Test
        @DisplayName(
                "PENDING → PAID sets status, stripePaymentIntentId, stripeEventId and registers PaymentCompleted")
        void pending_toPaid_setsFieldsAndRegistersPaymentCompleted() {
            Payment payment = createPendingPayment();

            payment.markPaid(PAYMENT_INTENT_ID, STRIPE_EVENT_ID);

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(payment.getStripePaymentIntentId()).isEqualTo(PAYMENT_INTENT_ID);
            assertThat(payment.getStripeEventId()).isEqualTo(STRIPE_EVENT_ID);
            assertThat(payment.getDomainEvents()).hasSize(1);
            assertThat(payment.getDomainEvents().get(0)).isInstanceOf(PaymentCompleted.class);
        }
    }

    @Nested
    @DisplayName("markCancelled()")
    class MarkCancelled {

        @Test
        @DisplayName("PENDING → CANCELLED sets status")
        void pending_toCancelled_setsStatus() {
            Payment payment = createPendingPayment();

            payment.markCancelled();

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
            assertThat(payment.getDomainEvents()).isEmpty();
        }
    }

    @Nested
    @DisplayName("markFailed()")
    class MarkFailed {

        @Test
        @DisplayName("PENDING → FAILED sets FAILED status with errorMessage and stripeEventId")
        void pending_toFailed_setsFailed() {
            Payment payment = createPendingPayment();
            String errorMessage = "card_declined";

            payment.markFailed(errorMessage, STRIPE_EVENT_ID);

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(payment.getErrorMessage()).isEqualTo(errorMessage);
            assertThat(payment.getStripeEventId()).isEqualTo(STRIPE_EVENT_ID);
            assertThat(payment.getDomainEvents()).isEmpty();
        }
    }

    @Nested
    @DisplayName("markRefunded()")
    class MarkRefunded {

        @Test
        @DisplayName("PAID → REFUNDED sets status and registers PaymentRefunded")
        void paid_toRefunded_setsRefundedAndRegistersPaymentRefunded() {
            Payment payment = createPendingPayment();
            payment.markPaid(PAYMENT_INTENT_ID, STRIPE_EVENT_ID);
            payment.clearDomainEvents();

            payment.markRefunded();

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
            assertThat(payment.getDomainEvents()).hasSize(1);
            assertThat(payment.getDomainEvents().get(0)).isInstanceOf(PaymentRefunded.class);
        }
    }
}

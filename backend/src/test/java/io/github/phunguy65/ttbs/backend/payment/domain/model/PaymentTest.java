package io.github.phunguy65.ttbs.backend.payment.domain.model;

import static org.assertj.core.api.Assertions.*;

import io.github.phunguy65.ttbs.backend.payment.domain.errors.PaymentError;
import io.github.phunguy65.ttbs.backend.payment.domain.event.PaymentCancelled;
import io.github.phunguy65.ttbs.backend.payment.domain.event.PaymentConfirmed;
import io.github.phunguy65.ttbs.backend.payment.domain.event.PaymentCreated;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentTest {

    private static final UUID BOOKING_ID = UUID.randomUUID();
    private static final CheckoutSessionId SESSION_ID = CheckoutSessionId.of("cs_test_abc123");
    private static final BigDecimal AMOUNT = new BigDecimal("300000");
    private static final String EVENT_ID = "evt_test_001";

    private Payment createPendingPayment() {
        return Payment.create(BOOKING_ID, SESSION_ID, AMOUNT);
    }

    @Test
    void create_shouldStartInPendingStatus() {
        Payment payment = createPendingPayment();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void create_shouldRegisterPaymentCreatedEvent() {
        Payment payment = createPendingPayment();
        assertThat(payment.getDomainEvents()).hasSize(1);
        assertThat(payment.getDomainEvents().getFirst()).isInstanceOf(PaymentCreated.class);
    }

    @Test
    void create_shouldSetFields() {
        Payment payment = createPendingPayment();
        assertThat(payment.getBookingId()).isEqualTo(BOOKING_ID);
        assertThat(payment.getCheckoutSessionId()).isEqualTo(SESSION_ID);
        assertThat(payment.getAmountVnd()).isEqualByComparingTo(AMOUNT);
        assertThat(payment.getId()).isNotNull();
    }

    @Test
    void confirm_fromPending_shouldTransitionToPaid() {
        Payment payment = createPendingPayment();
        payment.clearDomainEvents();

        Result<Void, PaymentError> result = payment.confirm(EVENT_ID);

        assertThat(result.isSuccess()).isTrue();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getStripeEventId()).isEqualTo(EVENT_ID);
        assertThat(payment.getDomainEvents()).hasSize(1);
        assertThat(payment.getDomainEvents().getFirst()).isInstanceOf(PaymentConfirmed.class);
    }

    @Test
    void confirm_fromPaid_shouldBeIdempotent() {
        Payment payment = createPendingPayment();
        payment.confirm(EVENT_ID);
        payment.clearDomainEvents();

        Result<Void, PaymentError> result = payment.confirm(EVENT_ID);

        assertThat(result.isSuccess()).isTrue();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getDomainEvents()).isEmpty();
    }

    @Test
    void confirm_fromCancelled_shouldReturnAlreadyProcessed() {
        Payment payment = createPendingPayment();
        payment.cancel(EVENT_ID);
        payment.clearDomainEvents();

        Result<Void, PaymentError> result = payment.confirm("evt_other");

        assertThat(result.isFailure()).isTrue();
        assertThat(((Result.Failure<Void, PaymentError>) result).error())
                .isInstanceOf(PaymentError.AlreadyProcessed.class);
    }

    @Test
    void cancel_fromPending_shouldTransitionToCancelled() {
        Payment payment = createPendingPayment();
        payment.clearDomainEvents();

        Result<Void, PaymentError> result = payment.cancel(EVENT_ID);

        assertThat(result.isSuccess()).isTrue();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(payment.getDomainEvents()).hasSize(1);
        assertThat(payment.getDomainEvents().getFirst()).isInstanceOf(PaymentCancelled.class);
    }

    @Test
    void cancel_fromCancelled_shouldBeIdempotent() {
        Payment payment = createPendingPayment();
        payment.cancel(EVENT_ID);
        payment.clearDomainEvents();

        Result<Void, PaymentError> result = payment.cancel(EVENT_ID);

        assertThat(result.isSuccess()).isTrue();
        assertThat(payment.getDomainEvents()).isEmpty();
    }
}

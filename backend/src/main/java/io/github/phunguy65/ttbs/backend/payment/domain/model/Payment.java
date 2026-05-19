package io.github.phunguy65.ttbs.backend.payment.domain.model;

import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.payment.domain.event.PaymentCompleted;
import io.github.phunguy65.ttbs.backend.payment.domain.event.PaymentRefunded;
import io.github.phunguy65.ttbs.backend.shared.domain.AggregateRoot;
import io.github.phunguy65.ttbs.backend.shared.domain.Money;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import java.time.Instant;

/**
 * Aggregate root representing a Stripe payment for a booking.
 *
 * <p>Status machine:
 * <ul>
 *   <li>PENDING → PAID (via {@link #markPaid})
 *   <li>PENDING → CANCELLED (via {@link #markCancelled})
 *   <li>PENDING → FAILED (via {@link #markFailed})
 *   <li>PAID → REFUNDED (via {@link #markRefunded})
 * </ul>
 */
public class Payment extends AggregateRoot<PaymentId> {

    private final PaymentId paymentId;
    private final BookingId bookingId;
    private final UserId userId;
    private final Money amount;
    private PaymentStatus status;
    private final CheckoutSessionId checkoutSessionId;
    private final String checkoutUrl;
    private StripePaymentIntentId stripePaymentIntentId;
    private StripeEventId stripeEventId;
    private String errorMessage;
    private final Instant createdAt;
    private Instant updatedAt;

    private Payment(
            PaymentId paymentId,
            BookingId bookingId,
            UserId userId,
            Money amount,
            PaymentStatus status,
            String checkoutSessionId,
            String checkoutUrl,
            String stripePaymentIntentId,
            String stripeEventId,
            String errorMessage,
            Instant createdAt,
            Instant updatedAt) {
        this.paymentId = paymentId;
        this.bookingId = bookingId;
        this.userId = userId;
        this.amount = amount;
        this.status = status;
        this.checkoutSessionId = CheckoutSessionId.of(checkoutSessionId);
        this.checkoutUrl = checkoutUrl;
        this.stripePaymentIntentId = StripePaymentIntentId.ofNullable(stripePaymentIntentId);
        this.stripeEventId = StripeEventId.ofNullable(stripeEventId);
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** Creates a new PENDING payment after a Stripe Checkout Session is created. */
    public static Payment create(
            PaymentId paymentId,
            BookingId bookingId,
            UserId userId,
            Money amount,
            String checkoutSessionId,
            String checkoutUrl) {
        Instant now = Instant.now();
        return new Payment(
                paymentId,
                bookingId,
                userId,
                amount,
                PaymentStatus.PENDING,
                checkoutSessionId,
                checkoutUrl,
                null,
                null,
                null,
                now,
                now);
    }

    /** Reconstitutes a payment from persistence — no events registered. */
    public static Payment reconstitute(
            PaymentId paymentId,
            BookingId bookingId,
            UserId userId,
            Money amount,
            PaymentStatus status,
            String checkoutSessionId,
            String checkoutUrl,
            String stripePaymentIntentId,
            String stripeEventId,
            String errorMessage,
            Instant createdAt,
            Instant updatedAt) {
        return new Payment(
                paymentId,
                bookingId,
                userId,
                amount,
                status,
                checkoutSessionId,
                checkoutUrl,
                stripePaymentIntentId,
                stripeEventId,
                errorMessage,
                createdAt,
                updatedAt);
    }

    public void markPaid(String stripePaymentIntentId, String stripeEventId) {
        this.status = PaymentStatus.PAID;
        this.stripePaymentIntentId = StripePaymentIntentId.of(stripePaymentIntentId);
        this.stripeEventId = StripeEventId.of(stripeEventId);
        this.updatedAt = Instant.now();
        registerEvent(new PaymentCompleted(paymentId, bookingId));
    }

    public void markCancelled() {
        this.status = PaymentStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }

    public void markFailed(String errorMessage, String stripeEventId) {
        this.status = PaymentStatus.FAILED;
        this.errorMessage = errorMessage;
        this.stripeEventId = StripeEventId.of(stripeEventId);
        this.updatedAt = Instant.now();
    }

    public void markRefunded() {
        this.status = PaymentStatus.REFUNDED;
        this.updatedAt = Instant.now();
        registerEvent(new PaymentRefunded(paymentId, bookingId));
    }

    @Override
    public PaymentId getId() {
        return paymentId;
    }

    public PaymentId getPaymentId() {
        return paymentId;
    }

    public BookingId getBookingId() {
        return bookingId;
    }

    public UserId getUserId() {
        return userId;
    }

    public Money getAmount() {
        return amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getCheckoutSessionId() {
        return checkoutSessionId.value();
    }

    public String getCheckoutUrl() {
        return checkoutUrl;
    }

    public String getStripePaymentIntentId() {
        return stripePaymentIntentId == null ? null : stripePaymentIntentId.value();
    }

    public String getStripeEventId() {
        return stripeEventId == null ? null : stripeEventId.value();
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

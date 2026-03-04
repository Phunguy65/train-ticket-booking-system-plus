package io.github.phunguy65.ttbs.backend.payment.domain.model;

import io.github.phunguy65.ttbs.backend.payment.domain.errors.PaymentError;
import io.github.phunguy65.ttbs.backend.payment.domain.event.PaymentCancelled;
import io.github.phunguy65.ttbs.backend.payment.domain.event.PaymentConfirmed;
import io.github.phunguy65.ttbs.backend.payment.domain.event.PaymentCreated;
import io.github.phunguy65.ttbs.backend.shared.domain.AggregateRoot;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class Payment extends AggregateRoot<PaymentId> {

    private final PaymentId id;
    private final UUID bookingId;
    private final CheckoutSessionId checkoutSessionId;
    private final BigDecimal amountVnd;
    private PaymentStatus status;
    private String stripeEventId;
    private final Instant createdAt;
    private Instant updatedAt;

    private Payment(
            PaymentId id,
            UUID bookingId,
            CheckoutSessionId checkoutSessionId,
            BigDecimal amountVnd,
            PaymentStatus status,
            String stripeEventId,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.bookingId = bookingId;
        this.checkoutSessionId = checkoutSessionId;
        this.amountVnd = amountVnd;
        this.status = status;
        this.stripeEventId = stripeEventId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Payment create(
            UUID bookingId, CheckoutSessionId checkoutSessionId, BigDecimal amountVnd) {
        PaymentId paymentId = PaymentId.generate();
        Instant now = Instant.now();
        Payment payment = new Payment(
                paymentId,
                bookingId,
                checkoutSessionId,
                amountVnd,
                PaymentStatus.PENDING,
                null,
                now,
                now);
        payment.registerEvent(PaymentCreated.of(paymentId, bookingId));
        return payment;
    }

    public static Payment reconstitute(
            UUID id,
            UUID bookingId,
            String checkoutSessionId,
            BigDecimal amountVnd,
            PaymentStatus status,
            String stripeEventId,
            Instant createdAt,
            Instant updatedAt) {
        return new Payment(
                PaymentId.of(id),
                bookingId,
                CheckoutSessionId.of(checkoutSessionId),
                amountVnd,
                status,
                stripeEventId,
                createdAt,
                updatedAt);
    }

    /**
     * Confirms the payment after successful Stripe payment.
     * Idempotent: already-PAID payments return success without re-processing.
     */
    public Result<Void, PaymentError> confirm(String stripeEventId) {
        if (status == PaymentStatus.PAID) {
            return Result.success();
        }
        if (status != PaymentStatus.PENDING) {
            return Result.failure(new PaymentError.AlreadyProcessed(status.name()));
        }
        this.status = PaymentStatus.PAID;
        this.stripeEventId = stripeEventId;
        this.updatedAt = Instant.now();
        registerEvent(PaymentConfirmed.of(id, bookingId));
        return Result.success();
    }

    /**
     * Cancels the payment when the Checkout Session expires.
     * Idempotent: already-CANCELLED payments return success without re-processing.
     */
    public Result<Void, PaymentError> cancel(String stripeEventId) {
        if (status == PaymentStatus.CANCELLED) {
            return Result.success();
        }
        if (status != PaymentStatus.PENDING) {
            return Result.failure(new PaymentError.AlreadyProcessed(status.name()));
        }
        this.status = PaymentStatus.CANCELLED;
        this.stripeEventId = stripeEventId;
        this.updatedAt = Instant.now();
        registerEvent(PaymentCancelled.of(id, bookingId));
        return Result.success();
    }

    @Override
    public PaymentId getId() {
        return id;
    }

    public UUID getBookingId() {
        return bookingId;
    }

    public CheckoutSessionId getCheckoutSessionId() {
        return checkoutSessionId;
    }

    public BigDecimal getAmountVnd() {
        return amountVnd;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getStripeEventId() {
        return stripeEventId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

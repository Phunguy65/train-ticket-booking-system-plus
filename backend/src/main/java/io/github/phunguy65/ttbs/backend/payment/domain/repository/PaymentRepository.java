package io.github.phunguy65.ttbs.backend.payment.domain.repository;

import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.payment.domain.model.Payment;
import java.util.Optional;

/**
 * Domain-facing persistence contract for {@link Payment}.
 *
 * <p>No JPA or Spring framework types appear here.
 */
public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findByBookingId(BookingId bookingId);

    Optional<Payment> findByCheckoutSessionId(String checkoutSessionId);

    Optional<Payment> findByStripeEventId(String stripeEventId);

    Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);
}

package io.github.phunguy65.ttbs.backend.payment.domain.repository;

import io.github.phunguy65.ttbs.backend.payment.domain.model.Payment;
import io.github.phunguy65.ttbs.backend.payment.domain.model.PaymentId;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findById(PaymentId id);

    Optional<Payment> findByCheckoutSessionId(String checkoutSessionId);

    Optional<Payment> findByBookingId(UUID bookingId);
}

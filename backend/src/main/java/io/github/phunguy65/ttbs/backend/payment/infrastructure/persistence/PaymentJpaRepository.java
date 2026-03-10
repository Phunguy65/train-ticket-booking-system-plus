package io.github.phunguy65.ttbs.backend.payment.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface PaymentJpaRepository extends JpaRepository<PaymentEntity, UUID> {

    Optional<PaymentEntity> findByBookingId(UUID bookingId);

    Optional<PaymentEntity> findByCheckoutSessionId(String checkoutSessionId);

    Optional<PaymentEntity> findByStripeEventId(String stripeEventId);

    Optional<PaymentEntity> findByStripePaymentIntentId(String stripePaymentIntentId);
}

package io.github.phunguy65.ttbs.backend.payment.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface PaymentJpaRepository extends JpaRepository<PaymentEntity, UUID> {

    Optional<PaymentEntity> findByBookingId(UUID bookingId);

    List<PaymentEntity> findByBookingIdIn(List<UUID> bookingIds);

    Optional<PaymentEntity> findByCheckoutSessionId(String checkoutSessionId);

    Optional<PaymentEntity> findByStripeEventId(String stripeEventId);

    Optional<PaymentEntity> findByStripePaymentIntentId(String stripePaymentIntentId);
}

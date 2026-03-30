package io.github.phunguy65.ttbs.backend.payment.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.payment.domain.projection.PaymentSummary;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PaymentJpaRepository extends JpaRepository<PaymentEntity, UUID> {

    @Query("""
            SELECT new io.github.phunguy65.ttbs.backend.payment.domain.projection.PaymentSummary(
                p.id,
                p.bookingId,
                p.userId,
                p.status,
                p.checkoutUrl,
                p.amount,
                p.currency
            ) FROM PaymentEntity p WHERE p.id = :id
            """)
    Optional<PaymentSummary> findSummaryById(@Param("id") UUID id);

    Optional<PaymentEntity> findByBookingId(UUID bookingId);

    @Query("""
            SELECT new io.github.phunguy65.ttbs.backend.payment.domain.projection.PaymentSummary(
                p.id,
                p.bookingId,
                p.userId,
                p.status,
                p.checkoutUrl,
                p.amount,
                p.currency
            ) FROM PaymentEntity p WHERE p.bookingId = :bookingId
            """)
    Optional<PaymentSummary> findSummaryByBookingId(@Param("bookingId") UUID bookingId);

    List<PaymentEntity> findByBookingIdIn(List<UUID> bookingIds);

    Optional<PaymentEntity> findByCheckoutSessionId(String checkoutSessionId);

    Optional<PaymentEntity> findByStripeEventId(String stripeEventId);

    Optional<PaymentEntity> findByStripePaymentIntentId(String stripePaymentIntentId);
}

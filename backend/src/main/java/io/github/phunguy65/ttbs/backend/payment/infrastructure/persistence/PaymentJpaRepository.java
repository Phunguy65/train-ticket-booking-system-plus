package io.github.phunguy65.ttbs.backend.payment.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PaymentJpaRepository extends JpaRepository<PaymentEntity, UUID> {

    @Query(
            value = "SELECT p.id AS id, p.booking_id AS bookingId, b.user_id AS userId, "
                    + "p.status AS status, NULL AS checkoutUrl, p.amount AS amount, "
                    + "p.currency AS currency, p.stripe_payment_intent_id AS stripePaymentIntentId, "
                    + "p.created_at AS createdAt "
                    + "FROM payments p JOIN bookings b ON b.id = p.booking_id WHERE p.id = :id",
            nativeQuery = true)
    Optional<PaymentSummaryView> findPaymentSummaryById(@Param("id") UUID id);

    Optional<PaymentEntity> findByBookingId(UUID bookingId);

    @Query(
            value =
                    "SELECT p.id AS id, p.booking_id AS bookingId, b.user_id AS userId, "
                            + "p.status AS status, NULL AS checkoutUrl, p.amount AS amount, "
                            + "p.currency AS currency, p.stripe_payment_intent_id AS stripePaymentIntentId, "
                            + "p.created_at AS createdAt "
                            + "FROM payments p JOIN bookings b ON b.id = p.booking_id WHERE p.booking_id = :bookingId",
            nativeQuery = true)
    Optional<PaymentSummaryView> findPaymentSummaryByBookingId(@Param("bookingId") UUID bookingId);

    List<PaymentEntity> findByBookingIdIn(List<UUID> bookingIds);

    Optional<PaymentEntity> findByCheckoutSessionId(String checkoutSessionId);

    Optional<PaymentEntity> findByStripeEventId(String stripeEventId);

    Optional<PaymentEntity> findByStripePaymentIntentId(String stripePaymentIntentId);
}

interface PaymentSummaryView {
    UUID getId();

    UUID getBookingId();

    UUID getUserId();

    String getStatus();

    String getCheckoutUrl();

    long getAmount();

    String getCurrency();

    String getStripePaymentIntentId();

    java.time.Instant getCreatedAt();
}

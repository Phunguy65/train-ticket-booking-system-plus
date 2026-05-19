package io.github.phunguy65.ttbs.backend.payment.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PaymentJpaRepository extends JpaRepository<PaymentEntity, UUID> {

    @Query(
            value = "SELECT p.id AS id, p.booking_id AS bookingId, p.user_id AS userId, "
                    + "p.status AS status, p.checkout_url AS checkoutUrl, p.amount AS amount, "
                    + "p.currency AS currency, p.stripe_payment_intent_id AS stripePaymentIntentId, "
                    + "p.created_at AS createdAt "
                    + "FROM payments p WHERE p.id = :id",
            nativeQuery = true)
    Optional<PaymentSummaryView> findPaymentSummaryById(@Param("id") UUID id);

    Optional<PaymentEntity> findByBookingId(UUID bookingId);

    @Query(
            value = "SELECT p.id AS id, p.booking_id AS bookingId, p.user_id AS userId, "
                    + "p.status AS status, p.checkout_url AS checkoutUrl, p.amount AS amount, "
                    + "p.currency AS currency, p.stripe_payment_intent_id AS stripePaymentIntentId, "
                    + "p.created_at AS createdAt "
                    + "FROM payments p WHERE p.booking_id = :bookingId",
            nativeQuery = true)
    Optional<PaymentSummaryView> findPaymentSummaryByBookingId(@Param("bookingId") UUID bookingId);

    List<PaymentEntity> findByBookingIdIn(List<UUID> bookingIds);

    Optional<PaymentEntity> findByCheckoutSessionId(String checkoutSessionId);

    Optional<PaymentEntity> findByStripeEventId(String stripeEventId);

    Optional<PaymentEntity> findByStripePaymentIntentId(String stripePaymentIntentId);

    @Query(
            value = "SELECT p.id AS id, p.booking_id AS bookingId, p.user_id AS userId, "
                    + "p.status AS status, p.amount AS amount, p.currency AS currency, "
                    + "p.created_at AS createdAt, "
                    + "os.name AS originStationName, "
                    + "ds.name AS destinationStationName, "
                    + "st.departure_time AS departureTime "
                    + "FROM payments p "
                    + "JOIN bookings b ON b.id = p.booking_id "
                    + "JOIN scheduled_trips st ON st.id = b.scheduled_trip_id "
                    + "JOIN route_templates rt ON rt.id = st.route_template_id "
                    + "JOIN stations os ON os.id = rt.origin_station_id "
                    + "JOIN stations ds ON ds.id = rt.destination_station_id "
                    + "WHERE p.user_id = :userId",
            countQuery = "SELECT COUNT(*) FROM payments p WHERE p.user_id = :userId",
            nativeQuery = true)
    Page<UserPaymentSummaryView> findUserPaymentsByUserId(
            @Param("userId") UUID userId, Pageable pageable);
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

interface UserPaymentSummaryView {
    UUID getId();

    UUID getBookingId();

    UUID getUserId();

    String getStatus();

    long getAmount();

    String getCurrency();

    java.time.Instant getCreatedAt();

    String getOriginStationName();

    String getDestinationStationName();

    java.time.Instant getDepartureTime();
}

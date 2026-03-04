package io.github.phunguy65.ttbs.backend.booking.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface BookingJpaRepository extends JpaRepository<BookingEntity, UUID> {

    Optional<BookingEntity> findByIdempotencyKey(String idempotencyKey);

    Optional<BookingEntity> findByCheckoutSessionId(String checkoutSessionId);

    @Query("SELECT b FROM BookingEntity b "
            + "WHERE b.userId = :userId AND b.routeId = :routeId AND b.status = :status")
    Optional<BookingEntity> findByUserIdAndRouteIdAndStatus(
            @Param("userId") UUID userId,
            @Param("routeId") UUID routeId,
            @Param("status") BookingStatus status);

    @Query("SELECT b FROM BookingEntity b LEFT JOIN FETCH b.seats WHERE b.id = :id")
    Optional<BookingEntity> findByIdWithSeats(@Param("id") UUID id);

    @Query("SELECT COUNT(b) > 0 FROM BookingEntity b "
            + "WHERE b.userId = :userId AND b.status IN :statuses")
    boolean existsActiveByUserId(
            @Param("userId") UUID userId, @Param("statuses") List<BookingStatus> statuses);

    @Query("SELECT b FROM BookingEntity b LEFT JOIN FETCH b.seats "
            + "WHERE b.status = 'HELD' AND b.checkoutSessionId IS NOT NULL "
            + "AND b.createdAt < :threshold")
    List<BookingEntity> findStaleHoldsWithCheckoutSession(@Param("threshold") Instant threshold);
}

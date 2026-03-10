package io.github.phunguy65.ttbs.backend.booking.infrastructure.persistence;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface BookingJpaRepository extends JpaRepository<BookingEntity, UUID> {

    Optional<BookingEntity> findByIdempotencyKey(String idempotencyKey);

    boolean existsByUserIdAndStatusIn(UUID userId, Collection<String> statuses);

    @Query(
            "SELECT e FROM BookingEntity e WHERE e.userId = :userId AND e.routeId = :routeId AND e.status = 'HELD'")
    Optional<BookingEntity> findByUserIdAndRouteIdAndStatusHeld(
            @Param("userId") UUID userId, @Param("routeId") UUID routeId);

    @Query("SELECT e FROM BookingEntity e WHERE e.status = 'HELD' AND e.paymentDeadline < :now")
    List<BookingEntity> findByStatusHeldAndPaymentDeadlineBefore(@Param("now") Instant now);
}

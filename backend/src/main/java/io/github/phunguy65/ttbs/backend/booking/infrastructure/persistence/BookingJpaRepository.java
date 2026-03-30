package io.github.phunguy65.ttbs.backend.booking.infrastructure.persistence;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface BookingJpaRepository extends JpaRepository<BookingEntity, UUID> {

    @Query("""
            SELECT e.id AS id,
                   e.userId AS userId,
                   e.scheduledTripId AS scheduledTripId,
                   e.userInfoSnapshot AS userInfoSnapshot,
                   e.totalPrice AS totalPrice,
                   e.currency AS currency,
                   e.status AS status,
                   e.paymentDeadline AS paymentDeadline,
                   e.createdAt AS createdAt
            FROM BookingEntity e
            WHERE e.id = :id
            """)
    Optional<BookingSummaryView> findSummaryById(@Param("id") UUID id);

    Optional<BookingEntity> findByIdempotencyKey(String idempotencyKey);

    boolean existsByUserIdAndStatusIn(UUID userId, Collection<String> statuses);

    @Query(
            "SELECT e FROM BookingEntity e WHERE e.userId = :userId AND e.scheduledTripId = :scheduledTripId AND e.status = 'HELD'")
    Optional<BookingEntity> findByUserIdAndScheduledTripIdAndStatusHeld(
            @Param("userId") UUID userId, @Param("scheduledTripId") UUID scheduledTripId);

    @Query("SELECT e FROM BookingEntity e WHERE e.status = 'HELD' AND e.paymentDeadline < :now")
    List<BookingEntity> findByStatusHeldAndPaymentDeadlineBefore(@Param("now") Instant now);

    @Query("SELECT e.id AS id, e.status AS status FROM BookingEntity e "
            + "WHERE e.id IN :bookingIds AND e.status IN ('HELD', 'CONFIRMED')")
    List<BookingCancellationCandidateView> findCancellationCandidatesByIds(
            @Param("bookingIds") List<UUID> bookingIds);

    @Modifying
    @Query("UPDATE BookingEntity e SET e.status = 'CANCELLED' "
            + "WHERE e.id IN :bookingIds AND e.status IN ('HELD', 'CONFIRMED')")
    int cancelByIds(@Param("bookingIds") List<UUID> bookingIds);
}

interface BookingCancellationCandidateView {
    UUID getId();

    String getStatus();
}

interface BookingSummaryView {
    UUID getId();

    UUID getUserId();

    UUID getScheduledTripId();

    BookingUserInfoSnapshotJson getUserInfoSnapshot();

    long getTotalPrice();

    String getCurrency();

    String getStatus();

    Instant getPaymentDeadline();

    Instant getCreatedAt();
}

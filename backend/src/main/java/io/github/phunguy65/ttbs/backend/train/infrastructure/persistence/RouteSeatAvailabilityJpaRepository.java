package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface RouteSeatAvailabilityJpaRepository
        extends JpaRepository<RouteSeatAvailabilityEntity, RouteSeatAvailabilityId> {

    /**
     * Returns seats that are effectively available for a route:
     * - status = 'AVAILABLE', OR
     * - status = 'HELD' but the associated booking's payment_deadline has passed (lazy expiry)
     */
    @Query(
            value = "SELECT rsa.* FROM trip_seat_availability rsa "
                    + "LEFT JOIN bookings b ON b.id = rsa.booking_id "
                    + "WHERE rsa.scheduled_trip_id = :scheduledTripId "
                    + "AND (rsa.status = 'AVAILABLE' "
                    + "     OR (rsa.status = 'HELD' AND b.payment_deadline < CURRENT_TIMESTAMP))",
            nativeQuery = true)
    List<RouteSeatAvailabilityEntity> findAvailableByScheduledTripId(
            @Param("scheduledTripId") java.util.UUID scheduledTripId);

    @Query("SELECT e FROM RouteSeatAvailabilityEntity e "
            + "WHERE e.id.scheduledTripId = :scheduledTripId "
            + "ORDER BY e.id.seatId ASC")
    List<RouteSeatAvailabilityEntity> findAllByScheduledTripId(
            @Param("scheduledTripId") java.util.UUID scheduledTripId);

    @Query(
            "SELECT e FROM RouteSeatAvailabilityEntity e WHERE e.id.scheduledTripId = :scheduledTripId AND e.id.seatId = :seatId")
    Optional<RouteSeatAvailabilityEntity> findByScheduledTripIdAndSeatId(
            @Param("scheduledTripId") java.util.UUID scheduledTripId,
            @Param("seatId") java.util.UUID seatId);

    /**
     * Fetches the specified seats for a given route without locking.
     * Optimistic locking via {@code @Version} handles concurrent modification detection.
     * Seats are returned in ascending seatId order.
     */
    @Query("SELECT e FROM RouteSeatAvailabilityEntity e "
            + "WHERE e.id.scheduledTripId = :scheduledTripId AND e.id.seatId IN :seatIds "
            + "ORDER BY e.id.seatId ASC")
    List<RouteSeatAvailabilityEntity> findByScheduledTripIdAndSeatIds(
            @Param("scheduledTripId") java.util.UUID scheduledTripId,
            @Param("seatIds") List<java.util.UUID> seatIds);

    @Query("SELECT e FROM RouteSeatAvailabilityEntity e WHERE e.bookingId = :bookingId")
    List<RouteSeatAvailabilityEntity> findByBookingId(@Param("bookingId") UUID bookingId);

    @Query(
            "SELECT COUNT(e) > 0 FROM RouteSeatAvailabilityEntity e WHERE e.id.seatId = :seatId AND e.status IN ('HELD', 'BOOKED')")
    boolean existsActiveBySeatId(@Param("seatId") java.util.UUID seatId);

    @Query(
            "SELECT COUNT(e) > 0 FROM RouteSeatAvailabilityEntity e WHERE e.id.seatId IN :seatIds AND e.status IN ('HELD', 'BOOKED')")
    boolean existsActiveByAnyOfSeatIds(@Param("seatIds") List<java.util.UUID> seatIds);

    @Query("SELECT DISTINCT e.bookingId FROM RouteSeatAvailabilityEntity e "
            + "WHERE e.id.seatId = :seatId AND e.status IN ('HELD', 'BOOKED') "
            + "AND e.bookingId IS NOT NULL")
    List<UUID> findActiveBookingIdsBySeatId(@Param("seatId") java.util.UUID seatId);

    @Query("SELECT DISTINCT e.bookingId FROM RouteSeatAvailabilityEntity e "
            + "WHERE e.id.seatId IN :seatIds AND e.status IN ('HELD', 'BOOKED') "
            + "AND e.bookingId IS NOT NULL")
    List<UUID> findDistinctActiveBookingIdsBySeatIds(
            @Param("seatIds") List<java.util.UUID> seatIds);

    @Modifying
    @Query(
            value =
                    "DELETE FROM trip_seat_availability WHERE scheduled_trip_id IN :scheduledTripIds",
            nativeQuery = true)
    void hardDeleteByScheduledTripIds(@Param("scheduledTripIds") List<UUID> scheduledTripIds);

    @Modifying
    @Query(
            value = "DELETE FROM trip_seat_availability WHERE seat_id IN :seatIds",
            nativeQuery = true)
    void hardDeleteBySeatIds(@Param("seatIds") List<UUID> seatIds);
}

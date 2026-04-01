package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
            value =
                    "SELECT DISTINCT c.id AS id, c.car_number AS carNumber, c.total_seats AS totalSeats "
                            + "FROM trip_seat_availability tsa "
                            + "JOIN seats s ON s.id = tsa.seat_id AND s.deleted_at IS NULL "
                            + "JOIN coaches c ON c.id = s.coach_id AND c.deleted_at IS NULL "
                            + "WHERE tsa.scheduled_trip_id = :scheduledTripId "
                            + "ORDER BY c.car_number ASC, c.id ASC",
            countQuery = "SELECT COUNT(DISTINCT c.id) "
                    + "FROM trip_seat_availability tsa "
                    + "JOIN seats s ON s.id = tsa.seat_id AND s.deleted_at IS NULL "
                    + "JOIN coaches c ON c.id = s.coach_id AND c.deleted_at IS NULL "
                    + "WHERE tsa.scheduled_trip_id = :scheduledTripId",
            nativeQuery = true)
    Page<CoachSeatMapCoachSummaryView> findCoachSummariesByScheduledTripId(
            @Param("scheduledTripId") UUID scheduledTripId, Pageable pageable);

    @Query(
            value =
                    "SELECT s.id AS id, c.id AS coachId, s.seat_number AS seatNumber, tsa.status AS status "
                            + "FROM trip_seat_availability tsa "
                            + "JOIN seats s ON s.id = tsa.seat_id AND s.deleted_at IS NULL "
                            + "JOIN coaches c ON c.id = s.coach_id AND c.deleted_at IS NULL "
                            + "WHERE tsa.scheduled_trip_id = :scheduledTripId "
                            + "AND c.id IN :coachIds "
                            + "ORDER BY c.car_number ASC, c.id ASC, s.seat_number ASC, s.id ASC",
            nativeQuery = true)
    List<CoachSeatMapSeatSummaryView> findSeatSummariesByScheduledTripIdAndCoachIds(
            @Param("scheduledTripId") UUID scheduledTripId, @Param("coachIds") List<UUID> coachIds);

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

interface CoachSeatMapCoachSummaryView {
    UUID getId();

    int getCarNumber();

    int getTotalSeats();
}

interface CoachSeatMapSeatSummaryView {
    UUID getId();

    UUID getCoachId();

    String getSeatNumber();

    String getStatus();
}

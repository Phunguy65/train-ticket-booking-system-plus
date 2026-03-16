package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SeatJpaRepository extends JpaRepository<SeatEntity, UUID> {

    @Query("SELECT s FROM SeatEntity s WHERE s.coachId = :coachId AND s.deletedAt IS NULL")
    List<SeatEntity> findByCoachId(@Param("coachId") UUID coachId);

    @Query("SELECT s.id FROM SeatEntity s WHERE s.coachId IN :coachIds AND s.deletedAt IS NULL")
    List<UUID> findActiveIdsByCoachIds(@Param("coachIds") List<UUID> coachIds);

    boolean existsByCoachIdAndSeatNumber(UUID coachId, String seatNumber);

    @Query(
            "SELECT s FROM SeatEntity s JOIN CoachEntity c ON s.coachId = c.id WHERE c.trainId = :trainId AND s.deletedAt IS NULL")
    Page<SeatEntity> findAllActiveByTrainId(@Param("trainId") UUID trainId, Pageable pageable);

    @Query(
            value = "SELECT s.* FROM seats s "
                    + "JOIN route_seat_availability rsa ON rsa.seat_id = s.id "
                    + "LEFT JOIN bookings b ON b.id = rsa.booking_id "
                    + "WHERE rsa.route_id = :routeId "
                    + "AND (rsa.status = 'AVAILABLE' "
                    + "     OR (rsa.status = 'HELD' AND b.payment_deadline < CURRENT_TIMESTAMP)) "
                    + "AND s.deleted_at IS NULL",
            nativeQuery = true,
            countQuery = "SELECT COUNT(*) FROM seats s "
                    + "JOIN route_seat_availability rsa ON rsa.seat_id = s.id "
                    + "LEFT JOIN bookings b ON b.id = rsa.booking_id "
                    + "WHERE rsa.route_id = :routeId "
                    + "AND (rsa.status = 'AVAILABLE' "
                    + "     OR (rsa.status = 'HELD' AND b.payment_deadline < CURRENT_TIMESTAMP)) "
                    + "AND s.deleted_at IS NULL")
    Page<SeatEntity> findAllAvailableByRouteId(@Param("routeId") UUID routeId, Pageable pageable);

    @Query(
            "SELECT COUNT(s) FROM SeatEntity s JOIN CoachEntity c ON s.coachId = c.id WHERE c.trainId = :trainId AND s.deletedAt IS NULL")
    int countActiveByTrainId(@Param("trainId") UUID trainId);

    @Query("SELECT COUNT(s) FROM SeatEntity s WHERE s.coachId = :coachId AND s.deletedAt IS NULL")
    int countActiveByCoachId(@Param("coachId") UUID coachId);

    @Query(
            "SELECT DISTINCT c.trainId FROM SeatEntity s JOIN CoachEntity c ON s.coachId = c.id WHERE s.id IN :seatIds")
    List<UUID> findDistinctTrainIdsBySeatIds(@Param("seatIds") List<UUID> seatIds);

    @Query("SELECT DISTINCT s.coachId FROM SeatEntity s WHERE s.id IN :seatIds")
    List<UUID> findDistinctCoachIdsBySeatIds(@Param("seatIds") List<UUID> seatIds);

    @Query("SELECT s FROM SeatEntity s WHERE s.id = :id AND s.deletedAt IS NULL")
    Optional<SeatEntity> findActiveById(@Param("id") UUID id);

    @Modifying
    @Query(
            "UPDATE SeatEntity s SET s.deletedAt = :deletedAt WHERE s.id = :id AND s.deletedAt IS NULL")
    void softDeleteById(@Param("id") UUID id, @Param("deletedAt") Instant deletedAt);

    @Modifying
    @Query(
            "UPDATE SeatEntity s SET s.deletedAt = :deletedAt WHERE s.id IN :ids AND s.deletedAt IS NULL")
    int softDeleteByIds(@Param("ids") List<UUID> ids, @Param("deletedAt") Instant deletedAt);
}

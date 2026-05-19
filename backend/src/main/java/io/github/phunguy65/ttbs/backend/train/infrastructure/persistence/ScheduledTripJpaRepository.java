package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.train.domain.projection.ScheduledTripSummary;
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

interface ScheduledTripJpaRepository extends JpaRepository<ScheduledTripEntity, UUID> {

    @Query("SELECT e FROM ScheduledTripEntity e WHERE e.id = :id AND e.deletedAt IS NULL")
    Optional<ScheduledTripEntity> findActiveById(@Param("id") UUID id);

    @Query(
            value = "SELECT st.id AS id, "
                    + "st.route_template_id AS routeTemplateId, "
                    + "st.train_id AS trainId, "
                    + "st.departure_time AS departureTime, "
                    + "st.arrival_time AS arrivalTime, "
                    + "st.status AS status, "
                    + "st.created_at AS createdAt, "
                    + "CAST(EXTRACT(EPOCH FROM (st.arrival_time - st.departure_time)) / 60 AS BIGINT) AS durationMinutes, "
                    + "(SELECT COUNT(*) "
                    + " FROM trip_seat_availability tsa "
                    + " JOIN seats s ON s.id = tsa.seat_id AND s.deleted_at IS NULL "
                    + " JOIN coaches c ON c.id = s.coach_id AND c.deleted_at IS NULL "
                    + " LEFT JOIN bookings b ON b.id = tsa.booking_id "
                    + " WHERE tsa.scheduled_trip_id = st.id "
                    + "   AND (tsa.status = 'AVAILABLE' "
                    + "        OR (tsa.status = 'HELD' AND b.payment_deadline < CURRENT_TIMESTAMP))) AS availableSeatCount, "
                    + "t.train_number AS trainNumber, "
                    + "t.name AS trainName, "
                    + "t.total_seats AS trainTotalSeats, "
                    + "os.id AS originStationId, "
                    + "os.code AS originStationCode, "
                    + "os.name AS originStationName, "
                    + "os.city AS originStationCity, "
                    + "ds.id AS destinationStationId, "
                    + "ds.code AS destinationStationCode, "
                    + "ds.name AS destinationStationName, "
                    + "ds.city AS destinationStationCity, "
                    + "rt.base_price AS routeBasePrice, "
                    + "'VND' AS routeCurrency "
                    + "FROM scheduled_trips st "
                    + "JOIN route_templates rt ON rt.id = st.route_template_id AND rt.deleted_at IS NULL "
                    + "LEFT JOIN trains t ON t.id = st.train_id AND t.deleted_at IS NULL "
                    + "JOIN stations os ON os.id = rt.origin_station_id AND os.deleted_at IS NULL "
                    + "JOIN stations ds ON ds.id = rt.destination_station_id AND ds.deleted_at IS NULL "
                    + "WHERE st.id = :id AND st.deleted_at IS NULL",
            nativeQuery = true)
    Optional<ScheduledTripEnrichedSummaryView> findEnrichedById(@Param("id") UUID id);

    @Query(
            value = "SELECT st.id AS id, "
                    + "st.route_template_id AS routeTemplateId, "
                    + "st.train_id AS trainId, "
                    + "st.departure_time AS departureTime, "
                    + "st.arrival_time AS arrivalTime, "
                    + "st.status AS status, "
                    + "st.created_at AS createdAt, "
                    + "CAST(EXTRACT(EPOCH FROM (st.arrival_time - st.departure_time)) / 60 AS BIGINT) AS durationMinutes, "
                    + "(SELECT COUNT(*) "
                    + " FROM trip_seat_availability tsa "
                    + " JOIN seats s ON s.id = tsa.seat_id AND s.deleted_at IS NULL "
                    + " JOIN coaches c ON c.id = s.coach_id AND c.deleted_at IS NULL "
                    + " LEFT JOIN bookings b ON b.id = tsa.booking_id "
                    + " WHERE tsa.scheduled_trip_id = st.id "
                    + "   AND (tsa.status = 'AVAILABLE' "
                    + "        OR (tsa.status = 'HELD' AND b.payment_deadline < CURRENT_TIMESTAMP))) AS availableSeatCount, "
                    + "t.train_number AS trainNumber, "
                    + "t.name AS trainName, "
                    + "t.total_seats AS trainTotalSeats, "
                    + "os.id AS originStationId, "
                    + "os.code AS originStationCode, "
                    + "os.name AS originStationName, "
                    + "os.city AS originStationCity, "
                    + "ds.id AS destinationStationId, "
                    + "ds.code AS destinationStationCode, "
                    + "ds.name AS destinationStationName, "
                    + "ds.city AS destinationStationCity, "
                    + "rt.base_price AS routeBasePrice, "
                    + "'VND' AS routeCurrency "
                    + "FROM scheduled_trips st "
                    + "JOIN route_templates rt ON rt.id = st.route_template_id AND rt.deleted_at IS NULL "
                    + "LEFT JOIN trains t ON t.id = st.train_id AND t.deleted_at IS NULL "
                    + "JOIN stations os ON os.id = rt.origin_station_id AND os.deleted_at IS NULL "
                    + "JOIN stations ds ON ds.id = rt.destination_station_id AND ds.deleted_at IS NULL "
                    + "WHERE st.id = :id",
            nativeQuery = true)
    Optional<ScheduledTripEnrichedSummaryView> findEnrichedByIdIncludingDeleted(
            @Param("id") UUID id);

    @Query("""
            SELECT new io.github.phunguy65.ttbs.backend.train.domain.projection.ScheduledTripSummary(
                e.id,
                e.routeTemplateId,
                e.trainId,
                e.departureTime,
                e.arrivalTime,
                e.status,
                e.createdAt
            ) FROM ScheduledTripEntity e WHERE e.id = :id AND e.deletedAt IS NULL
            """)
    Optional<ScheduledTripSummary> findSummaryById(@Param("id") UUID id);

    @Query("SELECT e FROM ScheduledTripEntity e WHERE e.deletedAt IS NULL")
    Page<ScheduledTripEntity> findAllActive(Pageable pageable);

    @Query(
            value = "SELECT st.id AS id, "
                    + "st.route_template_id AS routeTemplateId, "
                    + "st.train_id AS trainId, "
                    + "st.departure_time AS departureTime, "
                    + "st.arrival_time AS arrivalTime, "
                    + "st.status AS status, "
                    + "st.created_at AS createdAt, "
                    + "CAST(EXTRACT(EPOCH FROM (st.arrival_time - st.departure_time)) / 60 AS BIGINT) AS durationMinutes, "
                    + "(SELECT COUNT(*) "
                    + " FROM trip_seat_availability tsa "
                    + " JOIN seats s ON s.id = tsa.seat_id AND s.deleted_at IS NULL "
                    + " JOIN coaches c ON c.id = s.coach_id AND c.deleted_at IS NULL "
                    + " LEFT JOIN bookings b ON b.id = tsa.booking_id "
                    + " WHERE tsa.scheduled_trip_id = st.id "
                    + "   AND (tsa.status = 'AVAILABLE' "
                    + "        OR (tsa.status = 'HELD' AND b.payment_deadline < CURRENT_TIMESTAMP))) AS availableSeatCount, "
                    + "t.train_number AS trainNumber, "
                    + "t.name AS trainName, "
                    + "t.total_seats AS trainTotalSeats, "
                    + "os.id AS originStationId, "
                    + "os.code AS originStationCode, "
                    + "os.name AS originStationName, "
                    + "os.city AS originStationCity, "
                    + "ds.id AS destinationStationId, "
                    + "ds.code AS destinationStationCode, "
                    + "ds.name AS destinationStationName, "
                    + "ds.city AS destinationStationCity, "
                    + "rt.base_price AS routeBasePrice, "
                    + "'VND' AS routeCurrency "
                    + "FROM scheduled_trips st "
                    + "JOIN route_templates rt ON rt.id = st.route_template_id AND rt.deleted_at IS NULL "
                    + "LEFT JOIN trains t ON t.id = st.train_id AND t.deleted_at IS NULL "
                    + "JOIN stations os ON os.id = rt.origin_station_id AND os.deleted_at IS NULL "
                    + "JOIN stations ds ON ds.id = rt.destination_station_id AND ds.deleted_at IS NULL "
                    + "WHERE st.deleted_at IS NULL "
                    + "ORDER BY st.departure_time ASC, st.id ASC",
            countQuery = "SELECT COUNT(*) FROM scheduled_trips st "
                    + "JOIN route_templates rt ON rt.id = st.route_template_id AND rt.deleted_at IS NULL "
                    + "JOIN stations os ON os.id = rt.origin_station_id AND os.deleted_at IS NULL "
                    + "JOIN stations ds ON ds.id = rt.destination_station_id AND ds.deleted_at IS NULL "
                    + "WHERE st.deleted_at IS NULL",
            nativeQuery = true)
    Page<ScheduledTripEnrichedSummaryView> findAllEnrichedSummaries(Pageable pageable);

    @Query("""
            SELECT new io.github.phunguy65.ttbs.backend.train.domain.projection.ScheduledTripSummary(
                e.id,
                e.routeTemplateId,
                e.trainId,
                e.departureTime,
                e.arrivalTime,
                e.status,
                e.createdAt
            ) FROM ScheduledTripEntity e WHERE e.deletedAt IS NULL
            """)
    Page<ScheduledTripSummary> findAllSummaries(Pageable pageable);

    @Query(
            "SELECT COUNT(e) > 0 FROM ScheduledTripEntity e WHERE e.id = :id AND e.deletedAt IS NULL")
    boolean existsActiveById(@Param("id") UUID id);

    @Query(
            "SELECT e.id FROM ScheduledTripEntity e WHERE e.trainId IN :trainIds AND e.deletedAt IS NULL")
    List<UUID> findActiveIdsByTrainIds(@Param("trainIds") List<UUID> trainIds);

    @Query(
            "SELECT e.id FROM ScheduledTripEntity e WHERE e.routeTemplateId = :routeTemplateId AND e.deletedAt IS NULL")
    List<UUID> findActiveIdsByRouteTemplateId(@Param("routeTemplateId") UUID routeTemplateId);

    @Modifying
    @Query(
            "UPDATE ScheduledTripEntity e SET e.deletedAt = :deletedAt WHERE e.id = :id AND e.deletedAt IS NULL")
    void softDeleteById(@Param("id") UUID id, @Param("deletedAt") Instant deletedAt);

    @Modifying
    @Query(
            "UPDATE ScheduledTripEntity e SET e.deletedAt = :deletedAt WHERE e.id IN :ids AND e.deletedAt IS NULL")
    int softDeleteByIds(@Param("ids") List<UUID> ids, @Param("deletedAt") Instant deletedAt);
}

interface ScheduledTripEnrichedSummaryView {
    UUID getId();

    UUID getRouteTemplateId();

    UUID getTrainId();

    Instant getDepartureTime();

    Instant getArrivalTime();

    String getStatus();

    Instant getCreatedAt();

    long getDurationMinutes();

    long getAvailableSeatCount();

    String getTrainNumber();

    String getTrainName();

    Integer getTrainTotalSeats();

    UUID getOriginStationId();

    String getOriginStationCode();

    String getOriginStationName();

    String getOriginStationCity();

    UUID getDestinationStationId();

    String getDestinationStationCode();

    String getDestinationStationName();

    String getDestinationStationCity();

    long getRouteBasePrice();

    String getRouteCurrency();
}

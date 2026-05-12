package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.shared.domain.SliceResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.SortOrder;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.cursor.InvalidCursorException;
import io.github.phunguy65.ttbs.backend.train.application.port.ScheduledTripSearchPort;
import io.github.phunguy65.ttbs.backend.train.application.query.ScheduledTripSearchSortField;
import io.github.phunguy65.ttbs.backend.train.application.query.SearchScheduledTripsCursor;
import io.github.phunguy65.ttbs.backend.train.application.query.SearchScheduledTripsQuery;
import io.github.phunguy65.ttbs.backend.train.domain.projection.ScheduledTripEnrichedSummary;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class ScheduledTripSearchReader implements ScheduledTripSearchPort {

    private static final String AVAILABLE_SEAT_COUNT_SQL = "(SELECT COUNT(*) "
            + " FROM trip_seat_availability tsa "
            + " JOIN seats s ON s.id = tsa.seat_id AND s.deleted_at IS NULL "
            + " JOIN coaches c ON c.id = s.coach_id AND c.deleted_at IS NULL "
            + " LEFT JOIN bookings b ON b.id = tsa.booking_id "
            + " WHERE tsa.scheduled_trip_id = st.id "
            + "   AND (tsa.status = 'AVAILABLE' "
            + "        OR (tsa.status = 'HELD' AND b.payment_deadline < CURRENT_TIMESTAMP)))";

    private static final RowMapper<ScheduledTripEnrichedSummary> ROW_MAPPER =
            ScheduledTripSearchReader::mapRow;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    ScheduledTripSearchReader(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public SliceResponse<ScheduledTripEnrichedSummary> search(
            SearchScheduledTripsQuery query, SearchScheduledTripsCursor cursor) {
        String sortExpression = sortExpression(query.sortBy());
        String operator = query.sortDirection() == SortOrder.Direction.ASC ? ">" : "<";
        String direction = query.sortDirection().name();

        StringBuilder sql = new StringBuilder("SELECT st.id AS id, "
                + "st.route_template_id AS routeTemplateId, "
                + "st.train_id AS trainId, "
                + "st.departure_time AS departureTime, "
                + "st.arrival_time AS arrivalTime, "
                + "st.status AS status, "
                + "st.created_at AS createdAt, "
                + "CAST(EXTRACT(EPOCH FROM (st.arrival_time - st.departure_time)) / 60 AS BIGINT) AS durationMinutes, "
                + AVAILABLE_SEAT_COUNT_SQL
                + " AS availableSeatCount, "
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
                + "WHERE st.deleted_at IS NULL ");

        MapSqlParameterSource params = new MapSqlParameterSource();
        appendFilters(sql, params, query);
        appendCursor(sql, params, query, cursor, sortExpression, operator);

        sql.append(" ORDER BY ")
                .append(sortExpression)
                .append(' ')
                .append(direction)
                .append(", st.id ")
                .append(direction)
                .append(" LIMIT :limit");
        params.addValue("limit", query.size() + 1);

        List<ScheduledTripEnrichedSummary> rows =
                jdbcTemplate.query(sql.toString(), params, ROW_MAPPER);
        boolean hasNext = rows.size() > query.size();
        List<ScheduledTripEnrichedSummary> content = hasNext ? rows.subList(0, query.size()) : rows;
        return SliceResponse.of(content, query.size(), hasNext, null);
    }

    private void appendFilters(
            StringBuilder sql, MapSqlParameterSource params, SearchScheduledTripsQuery query) {
        if (query.originStationId() != null) {
            sql.append(" AND rt.origin_station_id = :originStationId");
            params.addValue("originStationId", query.originStationId());
        }
        if (query.destinationStationId() != null) {
            sql.append(" AND rt.destination_station_id = :destinationStationId");
            params.addValue("destinationStationId", query.destinationStationId());
        }
        if (query.departureDate() != null) {
            Instant startOfDay =
                    query.departureDate().atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant endOfDay = query.departureDate()
                    .plusDays(1)
                    .atStartOfDay(ZoneOffset.UTC)
                    .toInstant();
            sql.append(" AND st.departure_time >= :departureStart");
            sql.append(" AND st.departure_time < :departureEnd");
            params.addValue("departureStart", startOfDay.atOffset(ZoneOffset.UTC));
            params.addValue("departureEnd", endOfDay.atOffset(ZoneOffset.UTC));
        }
        if (query.status() != null) {
            sql.append(" AND st.status = :status");
            params.addValue("status", query.status());
        }
        if (query.availableOnly()) {
            sql.append(" AND ").append(AVAILABLE_SEAT_COUNT_SQL).append(" > 0");
        }
        if (query.minPrice() != null) {
            sql.append(" AND rt.base_price >= :minPrice");
            params.addValue("minPrice", query.minPrice());
        }
        if (query.maxPrice() != null) {
            sql.append(" AND rt.base_price <= :maxPrice");
            params.addValue("maxPrice", query.maxPrice());
        }
    }

    private void appendCursor(
            StringBuilder sql,
            MapSqlParameterSource params,
            SearchScheduledTripsQuery query,
            SearchScheduledTripsCursor cursor,
            String sortExpression,
            String operator) {
        if (cursor == null) {
            return;
        }

        sql.append(" AND ((")
                .append(sortExpression)
                .append(' ')
                .append(operator)
                .append(" :cursorSortValue) OR (")
                .append(sortExpression)
                .append(" = :cursorSortValue AND st.id ")
                .append(operator)
                .append(" :cursorId))");
        params.addValue("cursorSortValue", cursorValue(query.sortBy(), cursor));
        params.addValue("cursorId", cursor.id());
    }

    private Object cursorValue(
            ScheduledTripSearchSortField sortField, SearchScheduledTripsCursor cursor) {
        try {
            return switch (sortField) {
                case DEPARTURE_TIME -> Instant.parse(cursor.sortValue());
                case PRICE, DURATION, AVAILABLE_SEATS -> Long.parseLong(cursor.sortValue());
            };
        } catch (RuntimeException ex) {
            throw new InvalidCursorException("The supplied cursor is invalid", ex);
        }
    }

    private String sortExpression(ScheduledTripSearchSortField sortField) {
        return switch (sortField) {
            case DEPARTURE_TIME -> "st.departure_time";
            case PRICE -> "rt.base_price";
            case DURATION ->
                "CAST(EXTRACT(EPOCH FROM (st.arrival_time - st.departure_time)) / 60 AS BIGINT)";
            case AVAILABLE_SEATS -> AVAILABLE_SEAT_COUNT_SQL;
        };
    }

    private static ScheduledTripEnrichedSummary mapRow(ResultSet rs, int rowNum)
            throws SQLException {
        return new ScheduledTripEnrichedSummary(
                rs.getObject("id", UUID.class),
                rs.getObject("routeTemplateId", UUID.class),
                rs.getObject("trainId", UUID.class),
                rs.getTimestamp("departureTime").toInstant(),
                rs.getTimestamp("arrivalTime").toInstant(),
                rs.getString("status"),
                rs.getTimestamp("createdAt").toInstant(),
                rs.getLong("durationMinutes"),
                rs.getLong("availableSeatCount"),
                rs.getString("trainNumber"),
                rs.getString("trainName"),
                (Integer) rs.getObject("trainTotalSeats"),
                rs.getObject("originStationId", UUID.class),
                rs.getString("originStationCode"),
                rs.getString("originStationName"),
                rs.getString("originStationCity"),
                rs.getObject("destinationStationId", UUID.class),
                rs.getString("destinationStationCode"),
                rs.getString("destinationStationName"),
                rs.getString("destinationStationCity"),
                rs.getLong("routeBasePrice"),
                rs.getString("routeCurrency"));
    }
}

package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.phunguy65.ttbs.backend.shared.domain.SortOrder;
import io.github.phunguy65.ttbs.backend.train.application.query.ScheduledTripSearchSortField;
import io.github.phunguy65.ttbs.backend.train.application.query.SearchScheduledTripsCursor;
import io.github.phunguy65.ttbs.backend.train.application.query.SearchScheduledTripsQuery;
import io.github.phunguy65.ttbs.backend.train.domain.projection.ScheduledTripEnrichedSummary;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class ScheduledTripSearchReaderTest {

    private final NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
    private final ScheduledTripSearchReader reader = new ScheduledTripSearchReader(jdbcTemplate);

    @Test
    void searchBuildsExpectedSqlForFiltersAndCursor() {
        SearchScheduledTripsQuery query = new SearchScheduledTripsQuery(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                LocalDate.parse("2026-05-01"),
                "SCHEDULED",
                true,
                100000L,
                900000L,
                ScheduledTripSearchSortField.PRICE,
                SortOrder.Direction.DESC,
                null,
                2);
        SearchScheduledTripsCursor cursor = new SearchScheduledTripsCursor(
                "650000", UUID.fromString("33333333-3333-3333-3333-333333333333"));
        when(jdbcTemplate.query(
                        anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of(summary("44444444-4444-4444-4444-444444444444")));

        reader.search(query, cursor);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MapSqlParameterSource> paramsCaptor =
                ArgumentCaptor.forClass(MapSqlParameterSource.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<RowMapper<ScheduledTripEnrichedSummary>> mapperCaptor =
                ArgumentCaptor.forClass(RowMapper.class);
        org.mockito.Mockito.verify(jdbcTemplate)
                .query(sqlCaptor.capture(), paramsCaptor.capture(), mapperCaptor.capture());

        String sql = sqlCaptor.getValue();
        MapSqlParameterSource params = paramsCaptor.getValue();
        assertThat(sql).contains("rt.origin_station_id = :originStationId");
        assertThat(sql).contains("rt.destination_station_id = :destinationStationId");
        assertThat(sql).contains("st.departure_time >= :departureStart");
        assertThat(sql).contains("st.departure_time < :departureEnd");
        assertThat(sql).contains("st.status = :status");
        assertThat(sql).contains("rt.base_price >= :minPrice");
        assertThat(sql).contains("rt.base_price <= :maxPrice");
        assertThat(sql).contains("ORDER BY rt.base_price DESC, st.id DESC");
        assertThat(sql).contains("st.id < :cursorId");
        assertThat(params.getValue("cursorSortValue")).isEqualTo(650000L);
        assertThat(params.getValue("limit")).isEqualTo(3);
    }

    @Test
    void searchTrimsToSliceSizeWhenMoreRowsExist() {
        SearchScheduledTripsQuery query = new SearchScheduledTripsQuery(
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                ScheduledTripSearchSortField.DEPARTURE_TIME,
                SortOrder.Direction.ASC,
                null,
                2);
        when(jdbcTemplate.query(
                        anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of(
                        summary("11111111-1111-1111-1111-111111111111"),
                        summary("22222222-2222-2222-2222-222222222222"),
                        summary("33333333-3333-3333-3333-333333333333")));

        var result = reader.search(query, null);

        assertThat(result.content()).hasSize(2);
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    void searchBuildsCorrectSqlForDurationSorting() {
        SearchScheduledTripsQuery query = new SearchScheduledTripsQuery(
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                ScheduledTripSearchSortField.DURATION,
                SortOrder.Direction.ASC,
                null,
                2);
        when(jdbcTemplate.query(
                        anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of(summary("11111111-1111-1111-1111-111111111111")));

        reader.search(query, null);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(jdbcTemplate)
                .query(sqlCaptor.capture(), any(MapSqlParameterSource.class), any(RowMapper.class));
        assertThat(sqlCaptor.getValue())
                .contains(
                        "ORDER BY CAST(EXTRACT(EPOCH FROM (st.arrival_time - st.departure_time)) / 60 AS BIGINT) ASC");
    }

    @Test
    void searchBuildsCorrectSqlForAvailableSeatsSorting() {
        SearchScheduledTripsQuery query = new SearchScheduledTripsQuery(
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                ScheduledTripSearchSortField.AVAILABLE_SEATS,
                SortOrder.Direction.ASC,
                null,
                2);
        when(jdbcTemplate.query(
                        anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of(summary("11111111-1111-1111-1111-111111111111")));

        reader.search(query, null);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(jdbcTemplate)
                .query(sqlCaptor.capture(), any(MapSqlParameterSource.class), any(RowMapper.class));
        assertThat(sqlCaptor.getValue()).contains("ORDER BY (SELECT COUNT(*)");
    }

    @Test
    void searchBuildsAscendingCursorSqlForPriceSorting() {
        SearchScheduledTripsQuery query = new SearchScheduledTripsQuery(
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                ScheduledTripSearchSortField.PRICE,
                SortOrder.Direction.ASC,
                null,
                2);
        SearchScheduledTripsCursor cursor = new SearchScheduledTripsCursor(
                "650000", UUID.fromString("33333333-3333-3333-3333-333333333333"));
        when(jdbcTemplate.query(
                        anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of(summary("44444444-4444-4444-4444-444444444444")));

        reader.search(query, cursor);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(jdbcTemplate)
                .query(sqlCaptor.capture(), any(MapSqlParameterSource.class), any(RowMapper.class));
        assertThat(sqlCaptor.getValue()).contains("rt.base_price > :cursorSortValue");
        assertThat(sqlCaptor.getValue()).contains("st.id > :cursorId");
        assertThat(sqlCaptor.getValue()).contains("ORDER BY rt.base_price ASC, st.id ASC");
    }

    @Test
    void searchRejectsNonNumericCursorForNumericSortField() {
        SearchScheduledTripsQuery query = new SearchScheduledTripsQuery(
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                ScheduledTripSearchSortField.PRICE,
                SortOrder.Direction.ASC,
                null,
                2);
        SearchScheduledTripsCursor cursor = new SearchScheduledTripsCursor(
                "abc123", UUID.fromString("33333333-3333-3333-3333-333333333333"));

        assertThatThrownBy(() -> reader.search(query, cursor))
                .isInstanceOf(
                        io.github.phunguy65.ttbs.backend.shared.infrastructure.cursor
                                .InvalidCursorException.class)
                .hasMessage("The supplied cursor is invalid");
    }

    @Test
    void searchBuildsDescendingCursorSqlForDepartureTimeSorting() {
        SearchScheduledTripsQuery query = new SearchScheduledTripsQuery(
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                ScheduledTripSearchSortField.DEPARTURE_TIME,
                SortOrder.Direction.DESC,
                null,
                2);
        SearchScheduledTripsCursor cursor = new SearchScheduledTripsCursor(
                "2026-05-01T01:00:00Z", UUID.fromString("33333333-3333-3333-3333-333333333333"));
        when(jdbcTemplate.query(
                        anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of(summary("44444444-4444-4444-4444-444444444444")));

        reader.search(query, cursor);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MapSqlParameterSource> paramsCaptor =
                ArgumentCaptor.forClass(MapSqlParameterSource.class);
        org.mockito.Mockito.verify(jdbcTemplate)
                .query(sqlCaptor.capture(), paramsCaptor.capture(), any(RowMapper.class));
        assertThat(sqlCaptor.getValue()).contains("st.departure_time < :cursorSortValue");
        assertThat(sqlCaptor.getValue()).contains("st.id < :cursorId");
        assertThat(sqlCaptor.getValue()).contains("ORDER BY st.departure_time DESC, st.id DESC");
        assertThat(paramsCaptor.getValue().getValue("cursorSortValue"))
                .isEqualTo(java.time.Instant.parse("2026-05-01T01:00:00Z"));
    }

    @Test
    void searchBuildsDescendingCursorSqlForDurationSorting() {
        SearchScheduledTripsQuery query = new SearchScheduledTripsQuery(
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                ScheduledTripSearchSortField.DURATION,
                SortOrder.Direction.DESC,
                null,
                2);
        SearchScheduledTripsCursor cursor = new SearchScheduledTripsCursor(
                "600", UUID.fromString("33333333-3333-3333-3333-333333333333"));
        when(jdbcTemplate.query(
                        anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of(summary("44444444-4444-4444-4444-444444444444")));

        reader.search(query, cursor);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(jdbcTemplate)
                .query(sqlCaptor.capture(), any(MapSqlParameterSource.class), any(RowMapper.class));
        assertThat(sqlCaptor.getValue())
                .contains(
                        "CAST(EXTRACT(EPOCH FROM (st.arrival_time - st.departure_time)) / 60 AS BIGINT) < :cursorSortValue");
        assertThat(sqlCaptor.getValue())
                .contains(
                        "ORDER BY CAST(EXTRACT(EPOCH FROM (st.arrival_time - st.departure_time)) / 60 AS BIGINT) DESC, st.id DESC");
    }

    @Test
    void searchBuildsDescendingCursorSqlForAvailableSeatsSorting() {
        SearchScheduledTripsQuery query = new SearchScheduledTripsQuery(
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                ScheduledTripSearchSortField.AVAILABLE_SEATS,
                SortOrder.Direction.DESC,
                null,
                2);
        SearchScheduledTripsCursor cursor = new SearchScheduledTripsCursor(
                "8", UUID.fromString("33333333-3333-3333-3333-333333333333"));
        when(jdbcTemplate.query(
                        anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of(summary("44444444-4444-4444-4444-444444444444")));

        reader.search(query, cursor);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(jdbcTemplate)
                .query(sqlCaptor.capture(), any(MapSqlParameterSource.class), any(RowMapper.class));
        assertThat(sqlCaptor.getValue()).contains("(SELECT COUNT(*)");
        assertThat(sqlCaptor.getValue()).contains("< :cursorSortValue");
        assertThat(sqlCaptor.getValue()).contains("ORDER BY (SELECT COUNT(*)");
        assertThat(sqlCaptor.getValue()).contains("st.id DESC");
    }

    @Test
    void searchRejectsInvalidInstantCursorForDepartureTimeSortField() {
        SearchScheduledTripsQuery query = new SearchScheduledTripsQuery(
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                ScheduledTripSearchSortField.DEPARTURE_TIME,
                SortOrder.Direction.ASC,
                null,
                2);
        SearchScheduledTripsCursor cursor = new SearchScheduledTripsCursor(
                "not-an-instant", UUID.fromString("33333333-3333-3333-3333-333333333333"));

        assertThatThrownBy(() -> reader.search(query, cursor))
                .isInstanceOf(
                        io.github.phunguy65.ttbs.backend.shared.infrastructure.cursor
                                .InvalidCursorException.class)
                .hasMessage("The supplied cursor is invalid");
    }

    private ScheduledTripEnrichedSummary summary(String id) {
        return new ScheduledTripEnrichedSummary(
                UUID.fromString(id),
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
                java.time.Instant.parse("2026-05-01T01:00:00Z"),
                java.time.Instant.parse("2026-05-01T11:00:00Z"),
                "SCHEDULED",
                java.time.Instant.parse("2026-03-01T00:00:00Z"),
                600,
                8,
                "SE1",
                "North South Express 1",
                8,
                UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"),
                "HNO",
                "Ha Noi",
                "Ha Noi",
                UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"),
                "DAD",
                "Da Nang",
                "Da Nang",
                650000,
                "VND");
    }
}

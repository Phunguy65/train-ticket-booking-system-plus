package io.github.phunguy65.ttbs.backend.station.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.phunguy65.ttbs.backend.station.application.query.SearchStationsQuery;
import io.github.phunguy65.ttbs.backend.station.domain.projection.StationSummary;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class StationSearchReaderTest {

    private final NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
    private final StationSearchReader reader = new StationSearchReader(jdbcTemplate);

    @Test
    void searchBuildsFuzzyMatchSqlWhenKeywordProvided() {
        SearchStationsQuery query = new SearchStationsQuery("ha noi", 10);
        when(jdbcTemplate.query(
                        anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of(new StationSummary(
                        UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        "HNO",
                        "Ha Noi",
                        "Ha Noi",
                        Instant.parse("2026-03-01T00:00:00Z"))));

        reader.search(query);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MapSqlParameterSource> paramsCaptor =
                ArgumentCaptor.forClass(MapSqlParameterSource.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<RowMapper<StationSummary>> mapperCaptor =
                ArgumentCaptor.forClass(RowMapper.class);
        org.mockito.Mockito.verify(jdbcTemplate)
                .query(sqlCaptor.capture(), paramsCaptor.capture(), mapperCaptor.capture());

        assertThat(sqlCaptor.getValue())
                .contains(
                        "COALESCE(s.code, '') || ' ' || COALESCE(s.name, '') || ' ' || COALESCE(s.city, '') ILIKE :pattern");
        assertThat(sqlCaptor.getValue()).contains("WHEN s.code ILIKE :prefix THEN 0");
        assertThat(paramsCaptor.getValue().getValue("pattern")).isEqualTo("%ha noi%");
        assertThat(paramsCaptor.getValue().getValue("prefix")).isEqualTo("ha noi%");
    }

    @Test
    void searchFallsBackToCodeOrderingWhenKeywordMissing() {
        SearchStationsQuery query = new SearchStationsQuery(null, 5);
        when(jdbcTemplate.query(
                        anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of());

        reader.search(query);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(jdbcTemplate)
                .query(sqlCaptor.capture(), any(MapSqlParameterSource.class), any(RowMapper.class));
        assertThat(sqlCaptor.getValue()).contains("ORDER BY s.code ASC, s.id ASC");
        assertThat(sqlCaptor.getValue()).doesNotContain("ILIKE :pattern");
    }

    @Test
    void searchFallsBackToCodeOrderingWhenKeywordIsWhitespace() {
        SearchStationsQuery query = new SearchStationsQuery("   ", 5);
        when(jdbcTemplate.query(
                        anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of());

        reader.search(query);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(jdbcTemplate)
                .query(sqlCaptor.capture(), any(MapSqlParameterSource.class), any(RowMapper.class));
        assertThat(sqlCaptor.getValue()).contains("ORDER BY s.code ASC, s.id ASC");
    }
}

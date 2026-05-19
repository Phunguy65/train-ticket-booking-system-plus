package io.github.phunguy65.ttbs.backend.station.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.station.application.port.StationSearchPort;
import io.github.phunguy65.ttbs.backend.station.application.query.SearchStationsQuery;
import io.github.phunguy65.ttbs.backend.station.domain.projection.StationSummary;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class StationSearchReader implements StationSearchPort {

    private static final String SEARCH_DOCUMENT =
            "COALESCE(s.code, '') || ' ' || COALESCE(s.name, '') || ' ' || COALESCE(s.city, '')";

    private static final RowMapper<StationSummary> ROW_MAPPER = StationSearchReader::mapRow;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    StationSearchReader(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<StationSummary> search(SearchStationsQuery query) {
        StringBuilder sql = new StringBuilder(
                "SELECT s.id AS id, s.code AS code, s.name AS name, s.city AS city, s.created_at AS createdAt "
                        + "FROM stations s WHERE s.deleted_at IS NULL ");
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (query.keyword() != null) {
            sql.append(" AND ")
                    .append(SEARCH_DOCUMENT)
                    .append(" ILIKE :pattern")
                    .append(" ORDER BY CASE ")
                    .append("WHEN s.code ILIKE :prefix THEN 0 ")
                    .append("WHEN s.name ILIKE :prefix THEN 1 ")
                    .append("WHEN s.city ILIKE :prefix THEN 2 ")
                    .append("ELSE 3 END, s.name ASC, s.id ASC");
            params.addValue("pattern", "%" + query.keyword() + "%");
            params.addValue("prefix", query.keyword() + "%");
        } else {
            sql.append(" ORDER BY s.code ASC, s.id ASC");
        }

        sql.append(" LIMIT :limit");
        params.addValue("limit", query.limit());
        return jdbcTemplate.query(sql.toString(), params, ROW_MAPPER);
    }

    private static StationSummary mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new StationSummary(
                rs.getObject("id", UUID.class),
                rs.getString("code"),
                rs.getString("name"),
                rs.getString("city"),
                rs.getTimestamp("createdAt").toInstant());
    }
}

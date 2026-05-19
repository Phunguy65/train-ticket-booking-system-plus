package io.github.phunguy65.ttbs.backend.station.application.query;

public record SearchStationsQuery(String keyword, int limit) {

    public SearchStationsQuery {
        keyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
    }

    public String cacheKey() {
        return (keyword == null ? "_" : keyword) + ":" + limit;
    }
}

package io.github.phunguy65.ttbs.backend.station.application.port;

import io.github.phunguy65.ttbs.backend.station.domain.model.StationId;

/**
 * Cross-module port for validating route template constraints before deleting a station.
 */
public interface StationUsageValidationPort {

    /**
     * Returns {@code true} if there are active route templates that reference the given station as
     * either origin or destination.
     */
    boolean hasActiveRouteTemplatesForStation(StationId stationId);
}

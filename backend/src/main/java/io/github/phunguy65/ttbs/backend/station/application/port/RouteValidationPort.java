package io.github.phunguy65.ttbs.backend.station.application.port;

import io.github.phunguy65.ttbs.backend.station.domain.model.StationId;

/**
 * Cross-module port for validating route constraints before deleting a station.
 *
 * <p>Owned by the {@code station} module — allows the {@code station} application layer to check
 * route dependencies without coupling to train JPA internals. The {@code train} module provides
 * the implementation via {@code RouteValidationPortAdapter}.
 */
public interface RouteValidationPort {

    /**
     * Returns {@code true} if there are active (non-deleted) routes that reference the given
     * station as either origin or destination.
     *
     * @param stationId the station to check
     * @return {@code true} if deletion is blocked
     */
    boolean hasActiveRoutesForStation(StationId stationId);
}

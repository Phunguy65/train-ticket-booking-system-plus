package io.github.phunguy65.ttbs.backend.train.application.port.validation;

import io.github.phunguy65.ttbs.backend.station.domain.model.StationId;

/**
 * Cross-module port for validating route constraints from the {@code station} module.
 *
 * <p>Exposed via the {@code train::validation} named interface — allows the {@code station} module
 * to check route dependencies before deleting a station without coupling to train JPA internals.
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

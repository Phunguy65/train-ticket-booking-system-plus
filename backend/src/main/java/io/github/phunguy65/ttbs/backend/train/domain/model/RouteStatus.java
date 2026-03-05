package io.github.phunguy65.ttbs.backend.train.domain.model;

/**
 * Status lifecycle for a {@link Route}.
 *
 * <p>Matches the {@code chk_routes_status} DB constraint:
 * SCHEDULED → BOARDING → DEPARTED → ARRIVED | CANCELLED
 */
public enum RouteStatus {
    SCHEDULED,
    BOARDING,
    DEPARTED,
    ARRIVED,
    CANCELLED
}

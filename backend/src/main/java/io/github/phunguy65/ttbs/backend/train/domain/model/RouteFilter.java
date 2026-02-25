package io.github.phunguy65.ttbs.backend.train.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Optional filter criteria for route list queries.
 *
 * <p>All fields are nullable — a {@code null} value means "no filter on this axis". Passed to
 * {@link io.github.phunguy65.ttbs.backend.train.domain.repository.RouteRepository#findAll} so
 * that the caller does not need to manage many optional parameters individually.
 */
public record RouteFilter(
        UUID originStationId,
        UUID destinationStationId,
        Instant departureDateFrom,
        Instant departureDateTo) {

    /** Convenience factory for an empty (unfiltered) query. */
    public static RouteFilter empty() {
        return new RouteFilter(null, null, null, null);
    }
}

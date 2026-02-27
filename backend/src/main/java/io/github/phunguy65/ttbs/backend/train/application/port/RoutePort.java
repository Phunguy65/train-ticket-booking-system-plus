package io.github.phunguy65.ttbs.backend.train.application.port;

import io.github.phunguy65.ttbs.backend.train.domain.model.Route;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import java.util.Optional;

/**
 * Cross-module port that allows the {@code booking} module to look up route data.
 *
 * <p>Exposed via the {@code train::port} named interface — only route data needed
 * for booking (e.g. base price) is surfaced here.
 */
public interface RoutePort {

    /**
     * Finds a route by its identifier.
     *
     * @param routeId the route to look up
     * @return the route if found
     */
    Optional<Route> findById(RouteId routeId);
}

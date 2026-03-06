package io.github.phunguy65.ttbs.backend.train.application.port;

import io.github.phunguy65.ttbs.backend.train.domain.model.Route;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import java.util.Optional;

/**
 * Cross-module port that allows other modules (e.g. {@code booking}) to query route data.
 *
 * <p>Exposed via the {@code train::port} named interface.
 */
public interface RouteQueryPort {

    Optional<Route> findById(RouteId routeId);
}

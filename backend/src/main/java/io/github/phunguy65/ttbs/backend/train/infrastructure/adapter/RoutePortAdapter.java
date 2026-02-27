package io.github.phunguy65.ttbs.backend.train.infrastructure.adapter;

import io.github.phunguy65.ttbs.backend.train.application.port.RoutePort;
import io.github.phunguy65.ttbs.backend.train.domain.model.Route;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Adapter implementing {@link RoutePort} — delegates to the internal {@link RouteRepository}.
 * Bridges the cross-module boundary while keeping JPA details inside the train module.
 */
@Component
public class RoutePortAdapter implements RoutePort {

    private final RouteRepository routeRepository;

    public RoutePortAdapter(RouteRepository routeRepository) {
        this.routeRepository = routeRepository;
    }

    @Override
    public Optional<Route> findById(RouteId routeId) {
        return routeRepository.findById(routeId);
    }
}

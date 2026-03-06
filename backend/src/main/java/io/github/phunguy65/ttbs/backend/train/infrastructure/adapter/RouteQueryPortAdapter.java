package io.github.phunguy65.ttbs.backend.train.infrastructure.adapter;

import io.github.phunguy65.ttbs.backend.train.application.port.RouteQueryPort;
import io.github.phunguy65.ttbs.backend.train.domain.model.Route;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Adapter implementing {@link RouteQueryPort} — delegates to {@link RouteRepository}.
 */
@Component
public class RouteQueryPortAdapter implements RouteQueryPort {

    private final RouteRepository routeRepository;

    public RouteQueryPortAdapter(RouteRepository routeRepository) {
        this.routeRepository = routeRepository;
    }

    @Override
    public Optional<Route> findById(RouteId routeId) {
        return routeRepository.findById(routeId);
    }
}

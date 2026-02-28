package io.github.phunguy65.ttbs.backend.train.infrastructure.adapter;

import io.github.phunguy65.ttbs.backend.station.domain.model.StationId;
import io.github.phunguy65.ttbs.backend.train.application.port.validation.RouteValidationPort;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteRepository;
import org.springframework.stereotype.Component;

/**
 * Adapter implementing {@link RouteValidationPort} — delegates to {@link RouteRepository}.
 * Bridges the cross-module boundary while keeping JPA details inside the train module.
 */
@Component
public class RouteValidationPortAdapter implements RouteValidationPort {

    private final RouteRepository routeRepository;

    public RouteValidationPortAdapter(RouteRepository routeRepository) {
        this.routeRepository = routeRepository;
    }

    @Override
    public boolean hasActiveRoutesForStation(StationId stationId) {
        return routeRepository.existsActiveByStationId(stationId);
    }
}

package io.github.phunguy65.ttbs.backend.train.infrastructure.adapter;

import io.github.phunguy65.ttbs.backend.station.application.port.StationUsageValidationPort;
import io.github.phunguy65.ttbs.backend.station.domain.model.StationId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteTemplateRepository;
import org.springframework.stereotype.Component;

/**
 * Adapter implementing {@link StationUsageValidationPort} against active route templates.
 */
@Component
public class StationUsageValidationPortAdapter implements StationUsageValidationPort {

    private final RouteTemplateRepository routeTemplateRepository;

    public StationUsageValidationPortAdapter(RouteTemplateRepository routeTemplateRepository) {
        this.routeTemplateRepository = routeTemplateRepository;
    }

    @Override
    public boolean hasActiveRouteTemplatesForStation(StationId stationId) {
        return routeTemplateRepository.existsActiveByStationId(stationId);
    }
}

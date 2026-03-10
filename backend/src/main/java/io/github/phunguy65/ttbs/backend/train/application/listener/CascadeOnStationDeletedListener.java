package io.github.phunguy65.ttbs.backend.train.application.listener;

import io.github.phunguy65.ttbs.backend.station.domain.event.StationDeleted;
import io.github.phunguy65.ttbs.backend.train.domain.event.RoutesDeleted;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

@Service
public class CascadeOnStationDeletedListener {

    private final RouteRepository routeRepository;
    private final ApplicationEventPublisher eventPublisher;

    public CascadeOnStationDeletedListener(
            RouteRepository routeRepository, ApplicationEventPublisher eventPublisher) {
        this.routeRepository = routeRepository;
        this.eventPublisher = eventPublisher;
    }

    @ApplicationModuleListener
    public void onStationDeleted(StationDeleted event) {
        List<RouteId> routeIds = routeRepository.findActiveIdsByStationId(event.stationId());
        if (routeIds.isEmpty()) {
            return;
        }

        Instant now = Instant.now();
        routeRepository.softDeleteByIds(routeIds, now);
        eventPublisher.publishEvent(RoutesDeleted.of(routeIds, now));
    }
}

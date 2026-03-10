package io.github.phunguy65.ttbs.backend.train.application.listener;

import io.github.phunguy65.ttbs.backend.train.domain.event.RoutesDeleted;
import io.github.phunguy65.ttbs.backend.train.domain.event.TrainsDeleted;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteRepository;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteSeatAvailabilityRepository;
import io.github.phunguy65.ttbs.backend.train.domain.repository.TrainRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

@Service
public class CascadeOnRoutesDeletedListener {

    private final RouteRepository routeRepository;
    private final TrainRepository trainRepository;
    private final RouteSeatAvailabilityRepository availabilityRepository;
    private final ApplicationEventPublisher eventPublisher;

    public CascadeOnRoutesDeletedListener(
            RouteRepository routeRepository,
            TrainRepository trainRepository,
            RouteSeatAvailabilityRepository availabilityRepository,
            ApplicationEventPublisher eventPublisher) {
        this.routeRepository = routeRepository;
        this.trainRepository = trainRepository;
        this.availabilityRepository = availabilityRepository;
        this.eventPublisher = eventPublisher;
    }

    @ApplicationModuleListener
    public void onRoutesDeleted(RoutesDeleted event) {
        availabilityRepository.hardDeleteByRouteIds(event.routeIds());

        List<TrainId> candidateTrainIds =
                routeRepository.findDistinctActiveTrainIdsByRouteIds(event.routeIds());
        if (candidateTrainIds.isEmpty()) {
            return;
        }

        List<TrainId> orphanedTrainIds = candidateTrainIds.stream()
                .filter(trainId -> routeRepository.countActiveByTrainId(trainId) == 0)
                .toList();

        if (orphanedTrainIds.isEmpty()) {
            return;
        }

        Instant now = Instant.now();
        trainRepository.softDeleteByIds(orphanedTrainIds, now);
        eventPublisher.publishEvent(TrainsDeleted.of(orphanedTrainIds, now));
    }
}

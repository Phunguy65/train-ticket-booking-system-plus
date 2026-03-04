package io.github.phunguy65.ttbs.backend.train.application.listener;

import io.github.phunguy65.ttbs.backend.train.domain.event.CoachesDeleted;
import io.github.phunguy65.ttbs.backend.train.domain.event.SeatsDeleted;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteSeatAvailabilityRepository;
import io.github.phunguy65.ttbs.backend.train.domain.repository.SeatRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

@Service
public class CascadeOnCoachesDeletedListener {

    private final SeatRepository seatRepository;
    private final RouteSeatAvailabilityRepository availabilityRepository;
    private final ApplicationEventPublisher eventPublisher;

    public CascadeOnCoachesDeletedListener(
            SeatRepository seatRepository,
            RouteSeatAvailabilityRepository availabilityRepository,
            ApplicationEventPublisher eventPublisher) {
        this.seatRepository = seatRepository;
        this.availabilityRepository = availabilityRepository;
        this.eventPublisher = eventPublisher;
    }

    @ApplicationModuleListener
    public void onCoachesDeleted(CoachesDeleted event) {
        List<SeatId> seatIds = seatRepository.findActiveIdsByCoachIds(event.coachIds());
        if (seatIds.isEmpty()) {
            return;
        }

        availabilityRepository.hardDeleteBySeatIds(seatIds);

        Instant now = Instant.now();
        seatRepository.softDeleteByIds(seatIds, now);
        eventPublisher.publishEvent(SeatsDeleted.of(seatIds, now));
    }
}

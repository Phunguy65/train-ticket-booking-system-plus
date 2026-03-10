package io.github.phunguy65.ttbs.backend.train.application.listener;

import io.github.phunguy65.ttbs.backend.train.domain.event.CoachesDeleted;
import io.github.phunguy65.ttbs.backend.train.domain.event.TrainsDeleted;
import io.github.phunguy65.ttbs.backend.train.domain.model.CoachId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.CoachRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

@Service
public class CascadeOnTrainsDeletedListener {

    private final CoachRepository coachRepository;
    private final ApplicationEventPublisher eventPublisher;

    public CascadeOnTrainsDeletedListener(
            CoachRepository coachRepository, ApplicationEventPublisher eventPublisher) {
        this.coachRepository = coachRepository;
        this.eventPublisher = eventPublisher;
    }

    @ApplicationModuleListener
    public void onTrainsDeleted(TrainsDeleted event) {
        List<CoachId> coachIds = coachRepository.findActiveIdsByTrainIds(event.trainIds());
        if (coachIds.isEmpty()) {
            return;
        }

        Instant now = Instant.now();
        coachRepository.softDeleteByIds(coachIds, now);
        eventPublisher.publishEvent(CoachesDeleted.of(coachIds, now));
    }
}

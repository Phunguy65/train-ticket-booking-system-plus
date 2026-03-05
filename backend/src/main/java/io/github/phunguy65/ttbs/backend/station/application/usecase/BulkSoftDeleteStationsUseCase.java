package io.github.phunguy65.ttbs.backend.station.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.station.application.command.BulkSoftDeleteStationsCommand;
import io.github.phunguy65.ttbs.backend.station.domain.error.StationError;
import io.github.phunguy65.ttbs.backend.station.domain.event.StationsDeleted;
import io.github.phunguy65.ttbs.backend.station.domain.repository.StationRepository;
import java.time.Instant;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BulkSoftDeleteStationsUseCase {

    private final StationRepository stationRepository;
    private final ApplicationEventPublisher eventPublisher;

    public BulkSoftDeleteStationsUseCase(
            StationRepository stationRepository, ApplicationEventPublisher eventPublisher) {
        this.stationRepository = stationRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Result<Integer, StationError> execute(BulkSoftDeleteStationsCommand command) {
        Instant now = Instant.now();
        int affected = stationRepository.softDeleteByIds(command.stationIds(), now);

        if (affected > 0) {
            eventPublisher.publishEvent(StationsDeleted.of(command.stationIds(), now));
        }

        return Result.success(affected);
    }
}

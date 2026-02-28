package io.github.phunguy65.ttbs.backend.station.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.station.application.command.BulkSoftDeleteStationsCommand;
import io.github.phunguy65.ttbs.backend.station.domain.errors.StationError;
import io.github.phunguy65.ttbs.backend.station.domain.event.StationDeleted;
import io.github.phunguy65.ttbs.backend.station.domain.model.StationId;
import io.github.phunguy65.ttbs.backend.station.domain.repository.StationRepository;
import io.github.phunguy65.ttbs.backend.train.application.port.validation.RouteValidationPort;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BulkSoftDeleteStationsUseCase {

    private final StationRepository stationRepository;
    private final RouteValidationPort routeValidationPort;
    private final ApplicationEventPublisher eventPublisher;

    public BulkSoftDeleteStationsUseCase(
            StationRepository stationRepository,
            RouteValidationPort routeValidationPort,
            ApplicationEventPublisher eventPublisher) {
        this.stationRepository = stationRepository;
        this.routeValidationPort = routeValidationPort;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Result<Integer, StationError> execute(BulkSoftDeleteStationsCommand command) {
        List<UUID> conflictingIds = command.stationIds().stream()
                .filter(stationId -> routeValidationPort.hasActiveRoutesForStation(stationId))
                .map(StationId::value)
                .toList();

        if (!conflictingIds.isEmpty()) {
            return Result.failure(new StationError.StationInUse(conflictingIds));
        }

        Instant now = Instant.now();
        int affected = stationRepository.softDeleteByIds(command.stationIds(), now);

        for (StationId stationId : command.stationIds()) {
            eventPublisher.publishEvent(StationDeleted.of(stationId));
        }

        return Result.success(affected);
    }
}

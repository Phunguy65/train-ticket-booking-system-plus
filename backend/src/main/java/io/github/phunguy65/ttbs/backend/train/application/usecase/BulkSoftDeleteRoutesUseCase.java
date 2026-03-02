package io.github.phunguy65.ttbs.backend.train.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.command.BulkSoftDeleteRoutesCommand;
import io.github.phunguy65.ttbs.backend.train.domain.errors.RouteError;
import io.github.phunguy65.ttbs.backend.train.domain.event.RouteDeleted;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BulkSoftDeleteRoutesUseCase {

    private final RouteRepository routeRepository;
    private final ApplicationEventPublisher eventPublisher;

    public BulkSoftDeleteRoutesUseCase(
            RouteRepository routeRepository, ApplicationEventPublisher eventPublisher) {
        this.routeRepository = routeRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Result<Integer, RouteError> execute(BulkSoftDeleteRoutesCommand command) {
        List<UUID> missingIds = command.routeIds().stream()
                .filter(routeId -> !routeRepository.existsById(routeId))
                .map(RouteId::value)
                .toList();

        if (!missingIds.isEmpty()) {
            return Result.failure(new RouteError.RoutesNotFound(missingIds));
        }

        Instant now = Instant.now();
        int affected = routeRepository.softDeleteByIds(command.routeIds(), now);

        for (RouteId routeId : command.routeIds()) {
            eventPublisher.publishEvent(RouteDeleted.of(routeId));
        }

        return Result.success(affected);
    }
}

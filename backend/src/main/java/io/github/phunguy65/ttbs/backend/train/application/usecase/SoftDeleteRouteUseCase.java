package io.github.phunguy65.ttbs.backend.train.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.command.SoftDeleteRouteCommand;
import io.github.phunguy65.ttbs.backend.train.domain.errors.RouteError;
import io.github.phunguy65.ttbs.backend.train.domain.model.Route;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteRepository;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SoftDeleteRouteUseCase {

    private final RouteRepository routeRepository;
    private final ApplicationEventPublisher eventPublisher;

    public SoftDeleteRouteUseCase(
            RouteRepository routeRepository, ApplicationEventPublisher eventPublisher) {
        this.routeRepository = routeRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Result<Void, RouteError> execute(SoftDeleteRouteCommand command) {
        Optional<Route> found = routeRepository.findById(command.routeId());
        if (found.isEmpty()) {
            return Result.failure(new RouteError.RouteNotFound());
        }

        Route route = found.get();

        if (route.isDeleted()) {
            return Result.success();
        }

        route.softDelete();
        routeRepository.save(route);

        for (DomainEvent event : route.getDomainEvents()) {
            eventPublisher.publishEvent(event);
        }
        route.clearDomainEvents();

        return Result.success();
    }
}

package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteSeatAvailability;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteSeatAvailabilityStatus;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import org.springframework.stereotype.Component;

@Component
class RouteSeatAvailabilityEntityMapper {

    RouteSeatAvailability toDomain(RouteSeatAvailabilityEntity entity) {
        return RouteSeatAvailability.reconstitute(
                RouteId.of(entity.getId().getRouteId()),
                SeatId.of(entity.getId().getSeatId()),
                RouteSeatAvailabilityStatus.valueOf(entity.getStatus()),
                entity.getVersion());
    }

    RouteSeatAvailabilityEntity toEntity(RouteSeatAvailability domain) {
        RouteSeatAvailabilityEntity entity = new RouteSeatAvailabilityEntity();
        entity.setId(new RouteSeatAvailabilityId(
                domain.getRouteId().value(), domain.getSeatId().value()));
        entity.setStatus(domain.getStatus().name());
        entity.setVersion(domain.getVersion());
        return entity;
    }
}

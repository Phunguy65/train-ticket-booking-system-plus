package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.train.domain.model.RouteSeatAvailability;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteSeatAvailabilityStatus;
import io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripId;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import org.springframework.stereotype.Component;

@Component
class RouteSeatAvailabilityEntityMapper {

    RouteSeatAvailability toDomain(RouteSeatAvailabilityEntity entity) {
        return RouteSeatAvailability.reconstitute(
                ScheduledTripId.of(entity.getId().getScheduledTripId()),
                SeatId.of(entity.getId().getSeatId()),
                RouteSeatAvailabilityStatus.valueOf(entity.getStatus()),
                entity.getBookingId(),
                entity.getVersion());
    }

    RouteSeatAvailabilityEntity toEntity(RouteSeatAvailability domain) {
        RouteSeatAvailabilityEntity entity = new RouteSeatAvailabilityEntity();
        entity.setId(new RouteSeatAvailabilityId(
                domain.getScheduledTripId().value(), domain.getSeatId().value()));
        entity.setStatus(domain.getStatus().name());
        entity.setBookingId(domain.getBookingId());
        entity.setVersion(domain.getVersion());
        return entity;
    }
}

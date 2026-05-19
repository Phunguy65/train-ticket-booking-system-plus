package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.train.domain.model.RouteTemplateId;
import io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTrip;
import io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripId;
import io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripStatus;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import org.springframework.stereotype.Component;

@Component
class ScheduledTripEntityMapper {

    ScheduledTrip toDomain(ScheduledTripEntity entity) {
        return ScheduledTrip.reconstitute(
                ScheduledTripId.of(entity.getId()),
                RouteTemplateId.of(entity.getRouteTemplateId()),
                entity.getTrainId() == null ? null : TrainId.of(entity.getTrainId()),
                entity.getDepartureTime(),
                entity.getArrivalTime(),
                ScheduledTripStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getDeletedAt());
    }

    ScheduledTripEntity toEntity(ScheduledTrip scheduledTrip) {
        ScheduledTripEntity entity = new ScheduledTripEntity();
        entity.setId(scheduledTrip.getId().value());
        entity.setRouteTemplateId(scheduledTrip.getRouteTemplateId().value());
        entity.setTrainId(
                scheduledTrip.getTrainId() == null
                        ? null
                        : scheduledTrip.getTrainId().value());
        entity.setDepartureTime(scheduledTrip.getDepartureTime());
        entity.setArrivalTime(scheduledTrip.getArrivalTime());
        entity.setStatus(scheduledTrip.getStatus().name());
        entity.setCreatedAt(scheduledTrip.getCreatedAt());
        entity.setDeletedAt(scheduledTrip.getDeletedAt());
        return entity;
    }
}

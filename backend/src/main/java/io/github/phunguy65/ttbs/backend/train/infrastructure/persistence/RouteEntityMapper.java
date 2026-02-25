package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.station.domain.model.StationId;
import io.github.phunguy65.ttbs.backend.train.domain.model.Route;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteStatus;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
class RouteEntityMapper {

    Route toDomain(RouteEntity entity) {
        return Route.reconstitute(
                RouteId.of(entity.getId()),
                TrainId.of(entity.getTrainId()),
                StationId.of(entity.getOriginStationId()),
                StationId.of(entity.getDestinationStationId()),
                entity.getDepartureTime(),
                entity.getArrivalTime(),
                entity.getBasePrice(),
                RouteStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt() != null ? entity.getCreatedAt() : Instant.now());
    }

    RouteEntity toEntity(Route route) {
        RouteEntity entity = new RouteEntity();
        entity.setId(route.getId().value());
        entity.setTrainId(route.getTrainId().value());
        entity.setOriginStationId(route.getOriginStationId().value());
        entity.setDestinationStationId(route.getDestinationStationId().value());
        entity.setDepartureTime(route.getDepartureTime());
        entity.setArrivalTime(route.getArrivalTime());
        entity.setBasePrice(route.getBasePrice());
        entity.setStatus(route.getStatus().name());
        entity.setCreatedAt(route.getCreatedAt());
        return entity;
    }
}

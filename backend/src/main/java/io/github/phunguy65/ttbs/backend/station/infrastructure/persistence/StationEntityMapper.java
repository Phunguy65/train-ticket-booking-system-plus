package io.github.phunguy65.ttbs.backend.station.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.station.domain.model.Station;
import io.github.phunguy65.ttbs.backend.station.domain.model.StationId;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
class StationEntityMapper {

    Station toDomain(StationEntity entity) {
        return Station.reconstitute(
                StationId.of(entity.getId()),
                entity.getCode(),
                entity.getName(),
                entity.getCity(),
                entity.getCreatedAt() != null ? entity.getCreatedAt() : Instant.now());
    }

    StationEntity toEntity(Station station) {
        StationEntity entity = new StationEntity();
        entity.setId(station.getId().value());
        entity.setCode(station.getCode());
        entity.setName(station.getName());
        entity.setCity(station.getCity());
        entity.setCreatedAt(station.getCreatedAt());
        return entity;
    }
}

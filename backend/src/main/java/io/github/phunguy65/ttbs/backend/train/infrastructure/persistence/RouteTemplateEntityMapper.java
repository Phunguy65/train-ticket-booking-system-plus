package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.shared.domain.Money;
import io.github.phunguy65.ttbs.backend.station.domain.model.StationId;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteTemplate;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteTemplateId;
import org.springframework.stereotype.Component;

@Component
class RouteTemplateEntityMapper {

    RouteTemplate toDomain(RouteTemplateEntity entity) {
        return RouteTemplate.reconstitute(
                RouteTemplateId.of(entity.getId()),
                StationId.of(entity.getOriginStationId()),
                StationId.of(entity.getDestinationStationId()),
                Money.vnd(entity.getBasePrice()),
                entity.getCreatedAt(),
                entity.getDeletedAt());
    }

    RouteTemplateEntity toEntity(RouteTemplate template) {
        RouteTemplateEntity entity = new RouteTemplateEntity();
        entity.setId(template.getId().value());
        entity.setOriginStationId(template.getOriginStationId().value());
        entity.setDestinationStationId(template.getDestinationStationId().value());
        entity.setBasePrice(template.getBasePrice().toLong());
        entity.setCreatedAt(template.getCreatedAt());
        entity.setDeletedAt(template.getDeletedAt());
        return entity;
    }
}

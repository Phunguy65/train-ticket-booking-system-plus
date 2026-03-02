package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.train.domain.model.Train;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import org.springframework.stereotype.Component;

@Component
class TrainEntityMapper {

    Train toDomain(TrainEntity entity) {
        return Train.reconstitute(
                TrainId.of(entity.getId()),
                entity.getTrainNumber(),
                entity.getName(),
                entity.getTotalSeats(),
                entity.getCreatedAt(),
                entity.getDeletedAt());
    }

    TrainEntity toEntity(Train train) {
        TrainEntity entity = new TrainEntity();
        entity.setId(train.getId().value());
        entity.setTrainNumber(train.getTrainNumber());
        entity.setName(train.getName());
        entity.setTotalSeats(train.getTotalSeats());
        entity.setCreatedAt(train.getCreatedAt());
        entity.setDeletedAt(train.getDeletedAt());
        return entity;
    }
}

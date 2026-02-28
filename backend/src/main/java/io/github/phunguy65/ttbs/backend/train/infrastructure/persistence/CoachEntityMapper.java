package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.train.domain.model.Coach;
import io.github.phunguy65.ttbs.backend.train.domain.model.CoachId;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
class CoachEntityMapper {

    Coach toDomain(CoachEntity entity) {
        return Coach.reconstitute(
                CoachId.of(entity.getId()),
                TrainId.of(entity.getTrainId()),
                entity.getCarNumber(),
                entity.getTotalSeats(),
                entity.getCreatedAt() != null ? entity.getCreatedAt() : Instant.now(),
                entity.getDeletedAt());
    }

    CoachEntity toEntity(Coach coach) {
        CoachEntity entity = new CoachEntity();
        entity.setId(coach.getId().value());
        entity.setTrainId(coach.getTrainId().value());
        entity.setCarNumber(coach.getCarNumber());
        entity.setTotalSeats(coach.getTotalSeats());
        entity.setCreatedAt(coach.getCreatedAt());
        entity.setDeletedAt(coach.getDeletedAt());
        return entity;
    }
}

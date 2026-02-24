package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.train.domain.model.Seat;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatClass;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
class SeatEntityMapper {

    Seat toDomain(SeatEntity entity) {
        return Seat.reconstitute(
                SeatId.of(entity.getId()),
                TrainId.of(entity.getTrainId()),
                entity.getSeatNumber(),
                SeatClass.valueOf(entity.getSeatClass()),
                entity.getCreatedAt() != null ? entity.getCreatedAt() : Instant.now());
    }

    SeatEntity toEntity(Seat seat) {
        SeatEntity entity = new SeatEntity();
        entity.setId(seat.getId().value());
        entity.setTrainId(seat.getTrainId().value());
        entity.setSeatNumber(seat.getSeatNumber());
        entity.setSeatClass(seat.getSeatClass().name());
        entity.setCreatedAt(seat.getCreatedAt());
        return entity;
    }
}

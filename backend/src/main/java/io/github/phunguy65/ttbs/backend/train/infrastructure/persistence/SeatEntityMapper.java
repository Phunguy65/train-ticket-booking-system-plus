package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.train.domain.model.CoachId;
import io.github.phunguy65.ttbs.backend.train.domain.model.Seat;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
class SeatEntityMapper {

    Seat toDomain(SeatEntity entity) {
        return Seat.reconstitute(
                SeatId.of(entity.getId()),
                CoachId.of(entity.getCoachId()),
                entity.getSeatNumber(),
                entity.getCreatedAt() != null ? entity.getCreatedAt() : Instant.now(),
                entity.getDeletedAt());
    }

    SeatEntity toEntity(Seat seat) {
        SeatEntity entity = new SeatEntity();
        entity.setId(seat.getId().value());
        entity.setCoachId(seat.getCoachId().value());
        entity.setSeatNumber(seat.getSeatNumber());
        entity.setCreatedAt(seat.getCreatedAt());
        entity.setDeletedAt(seat.getDeletedAt());
        return entity;
    }
}

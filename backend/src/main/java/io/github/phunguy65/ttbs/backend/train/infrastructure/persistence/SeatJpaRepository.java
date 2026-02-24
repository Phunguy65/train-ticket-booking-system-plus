package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SeatJpaRepository extends JpaRepository<SeatEntity, UUID> {

    List<SeatEntity> findByTrainId(UUID trainId);

    boolean existsByTrainIdAndSeatNumber(UUID trainId, String seatNumber);
}

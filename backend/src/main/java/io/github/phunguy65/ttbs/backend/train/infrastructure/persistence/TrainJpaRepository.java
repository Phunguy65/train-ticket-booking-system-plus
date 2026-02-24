package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface TrainJpaRepository extends JpaRepository<TrainEntity, UUID> {

    boolean existsByTrainNumber(String trainNumber);
}

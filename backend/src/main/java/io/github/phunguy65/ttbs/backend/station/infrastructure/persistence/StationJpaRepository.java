package io.github.phunguy65.ttbs.backend.station.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface StationJpaRepository extends JpaRepository<StationEntity, UUID> {

    boolean existsByCode(String code);
}

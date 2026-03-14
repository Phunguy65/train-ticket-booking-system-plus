package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface TrainJpaRepository extends JpaRepository<TrainEntity, UUID> {

    boolean existsByTrainNumber(String trainNumber);

    @Query("SELECT t FROM TrainEntity t WHERE t.id = :id AND t.deletedAt IS NULL")
    Optional<TrainEntity> findActiveById(@Param("id") UUID id);

    @Query("SELECT t FROM TrainEntity t WHERE t.deletedAt IS NULL")
    Page<TrainEntity> findAllActive(Pageable pageable);

    @Modifying
    @Query(
            "UPDATE TrainEntity t SET t.deletedAt = :deletedAt WHERE t.id = :id AND t.deletedAt IS NULL")
    void softDeleteById(@Param("id") UUID id, @Param("deletedAt") Instant deletedAt);

    @Modifying
    @Query(
            "UPDATE TrainEntity t SET t.deletedAt = :deletedAt WHERE t.id IN :ids AND t.deletedAt IS NULL")
    int softDeleteByIds(@Param("ids") List<UUID> ids, @Param("deletedAt") Instant deletedAt);
}

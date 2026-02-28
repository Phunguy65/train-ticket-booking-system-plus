package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SeatJpaRepository extends JpaRepository<SeatEntity, UUID> {

    @Query("SELECT s FROM SeatEntity s WHERE s.trainId = :trainId AND s.deletedAt IS NULL")
    List<SeatEntity> findByTrainId(@Param("trainId") UUID trainId);

    boolean existsByTrainIdAndSeatNumber(UUID trainId, String seatNumber);

    @Query("SELECT s FROM SeatEntity s WHERE s.id = :id AND s.deletedAt IS NULL")
    Optional<SeatEntity> findActiveById(@Param("id") UUID id);

    @Modifying
    @Query(
            "UPDATE SeatEntity s SET s.deletedAt = :deletedAt WHERE s.id = :id AND s.deletedAt IS NULL")
    void softDeleteById(@Param("id") UUID id, @Param("deletedAt") Instant deletedAt);

    @Modifying
    @Query(
            "UPDATE SeatEntity s SET s.deletedAt = :deletedAt WHERE s.id IN :ids AND s.deletedAt IS NULL")
    int softDeleteByIds(@Param("ids") List<UUID> ids, @Param("deletedAt") Instant deletedAt);
}

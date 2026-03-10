package io.github.phunguy65.ttbs.backend.user.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

    @Query("SELECT u FROM UserEntity u WHERE u.email = :email AND u.deletedAt IS NULL")
    Optional<UserEntity> findByEmail(@Param("email") String email);

    @Query("SELECT u FROM UserEntity u WHERE u.id = :id AND u.deletedAt IS NULL")
    Optional<UserEntity> findActiveById(@Param("id") UUID id);

    @Query("SELECT u FROM UserEntity u WHERE u.deletedAt IS NULL")
    Slice<UserEntity> findAllActive(Pageable pageable);

    @Modifying
    @Query(
            "UPDATE UserEntity u SET u.deletedAt = :deletedAt WHERE u.id = :id AND u.deletedAt IS NULL")
    void softDeleteById(@Param("id") UUID id, @Param("deletedAt") Instant deletedAt);

    @Modifying
    @Query(
            "UPDATE UserEntity u SET u.deletedAt = :deletedAt WHERE u.id IN :ids AND u.deletedAt IS NULL")
    int softDeleteByIds(@Param("ids") List<UUID> ids, @Param("deletedAt") Instant deletedAt);
}

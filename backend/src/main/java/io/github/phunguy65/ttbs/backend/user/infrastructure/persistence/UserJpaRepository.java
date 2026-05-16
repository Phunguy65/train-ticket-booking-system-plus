package io.github.phunguy65.ttbs.backend.user.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.user.domain.projection.UserSummary;
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

interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

    @Query("SELECT u FROM UserEntity u WHERE u.email = :email AND u.deletedAt IS NULL")
    Optional<UserEntity> findByEmail(@Param("email") String email);

    @Query("SELECT u FROM UserEntity u WHERE u.id = :id AND u.deletedAt IS NULL")
    Optional<UserEntity> findActiveById(@Param("id") UUID id);

    @Query("SELECT u FROM UserEntity u WHERE u.id = :id")
    Optional<UserEntity> findByIdIncludingDeleted(@Param("id") UUID id);

    @Query("SELECT u FROM UserEntity u WHERE u.deletedAt IS NULL")
    Page<UserEntity> findAllActive(Pageable pageable);

    @Query("""
            SELECT new io.github.phunguy65.ttbs.backend.user.domain.projection.UserSummary(
                u.id,
                u.email,
                u.fullName,
                u.phone,
                u.dateOfBirth,
                u.gender,
                u.idDocumentNumber,
                u.addressLine,
                u.role,
                u.createdAt
            ) FROM UserEntity u WHERE u.id = :id AND u.deletedAt IS NULL
            """)
    Optional<UserSummary> findSummaryById(@Param("id") UUID id);

    @Query("""
            SELECT new io.github.phunguy65.ttbs.backend.user.domain.projection.UserSummary(
                u.id,
                u.email,
                u.fullName,
                u.phone,
                u.dateOfBirth,
                u.gender,
                u.idDocumentNumber,
                u.addressLine,
                u.role,
                u.createdAt
            ) FROM UserEntity u WHERE u.deletedAt IS NULL
            """)
    Page<UserSummary> findAllSummaries(Pageable pageable);

    @Modifying
    @Query(
            "UPDATE UserEntity u SET u.deletedAt = :deletedAt WHERE u.id = :id AND u.deletedAt IS NULL")
    void softDeleteById(@Param("id") UUID id, @Param("deletedAt") Instant deletedAt);

    @Modifying
    @Query(
            "UPDATE UserEntity u SET u.deletedAt = :deletedAt WHERE u.id IN :ids AND u.deletedAt IS NULL")
    int softDeleteByIds(@Param("ids") List<UUID> ids, @Param("deletedAt") Instant deletedAt);
}

package io.github.phunguy65.ttbs.backend.user.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.shared.domain.UserId;
import io.github.phunguy65.ttbs.backend.user.domain.model.User;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
class UserEntityMapper {

    User toDomain(UserEntity entity) {
        return User.reconstitute(
                UserId.of(entity.getId()),
                entity.getEmail(),
                entity.getPasswordHash(),
                entity.getFullName(),
                entity.getPhone(),
                entity.getRole(),
                entity.getCreatedAt() != null ? entity.getCreatedAt() : Instant.now(),
                entity.getUpdatedAt() != null ? entity.getUpdatedAt() : Instant.now());
    }

    UserEntity toEntity(User user) {
        UserEntity entity = new UserEntity();
        entity.setId(user.getId().value());
        entity.setEmail(user.getEmail());
        entity.setPasswordHash(user.getPasswordHash());
        entity.setFullName(user.getFullName());
        entity.setPhone(user.getPhone());
        entity.setRole(user.getRole());
        entity.setCreatedAt(user.getCreatedAt());
        entity.setUpdatedAt(user.getUpdatedAt());
        return entity;
    }
}

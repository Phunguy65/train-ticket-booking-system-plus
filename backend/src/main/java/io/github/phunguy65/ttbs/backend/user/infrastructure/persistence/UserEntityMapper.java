package io.github.phunguy65.ttbs.backend.user.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.user.domain.model.User;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserRole;
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
                UserRole.valueOf(entity.getRole()),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getDeletedAt());
    }

    UserEntity toEntity(User user) {
        UserEntity entity = new UserEntity();
        entity.setId(user.getId().value());
        entity.setEmail(user.getEmail());
        entity.setPasswordHash(user.getPasswordHash());
        entity.setFullName(user.getFullName());
        entity.setPhone(user.getPhone());
        entity.setRole(user.getRole().name());
        entity.setCreatedAt(user.getCreatedAt());
        entity.setUpdatedAt(user.getUpdatedAt());
        entity.setDeletedAt(user.getDeletedAt());
        return entity;
    }
}

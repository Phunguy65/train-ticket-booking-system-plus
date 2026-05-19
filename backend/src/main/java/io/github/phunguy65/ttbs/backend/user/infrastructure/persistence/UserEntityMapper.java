package io.github.phunguy65.ttbs.backend.user.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.shared.domain.AddressLine;
import io.github.phunguy65.ttbs.backend.shared.domain.EmailAddress;
import io.github.phunguy65.ttbs.backend.shared.domain.Gender;
import io.github.phunguy65.ttbs.backend.shared.domain.IdDocumentNumber;
import io.github.phunguy65.ttbs.backend.shared.domain.PasswordHash;
import io.github.phunguy65.ttbs.backend.shared.domain.PersonName;
import io.github.phunguy65.ttbs.backend.shared.domain.PhoneNumber;
import io.github.phunguy65.ttbs.backend.user.domain.model.User;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserRole;
import org.springframework.stereotype.Component;

@Component
class UserEntityMapper {

    User toDomain(UserEntity entity) {
        return User.reconstitute(
                UserId.of(entity.getId()),
                EmailAddress.of(entity.getEmail()),
                PasswordHash.of(entity.getPasswordHash()),
                PersonName.of(entity.getFullName()),
                PhoneNumber.ofNullable(entity.getPhone()),
                entity.getDateOfBirth(),
                Gender.ofNullable(entity.getGender()),
                IdDocumentNumber.ofNullable(entity.getIdDocumentNumber()),
                AddressLine.ofNullable(entity.getAddressLine()),
                UserRole.valueOf(entity.getRole()),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getDeletedAt());
    }

    UserEntity toEntity(User user) {
        UserEntity entity = new UserEntity();
        entity.setId(user.getId().value());
        entity.setEmail(user.getEmail().value());
        entity.setPasswordHash(user.getPasswordHash().value());
        entity.setFullName(user.getFullName().value());
        entity.setPhone(user.getPhone().map(PhoneNumber::value).orElse(null));
        entity.setDateOfBirth(user.getDateOfBirth().orElse(null));
        entity.setGender(user.getGender().map(Gender::value).orElse(null));
        entity.setIdDocumentNumber(
                user.getIdDocumentNumber().map(IdDocumentNumber::value).orElse(null));
        entity.setAddressLine(user.getAddressLine().map(AddressLine::value).orElse(null));
        entity.setRole(user.getRole().name());
        entity.setCreatedAt(user.getCreatedAt());
        entity.setUpdatedAt(user.getUpdatedAt());
        entity.setDeletedAt(user.getDeletedAt());
        return entity;
    }
}

package io.github.phunguy65.ttbs.backend.user.application.response;

import io.github.phunguy65.ttbs.backend.shared.domain.AddressLine;
import io.github.phunguy65.ttbs.backend.shared.domain.Gender;
import io.github.phunguy65.ttbs.backend.shared.domain.IdDocumentNumber;
import io.github.phunguy65.ttbs.backend.shared.domain.PhoneNumber;
import io.github.phunguy65.ttbs.backend.user.domain.model.User;
import io.github.phunguy65.ttbs.backend.user.domain.projection.UserSummary;
import org.springframework.stereotype.Component;

@Component
public class UserResponseMapper {

    public UserResponse fromUser(User user) {
        return new UserResponse(
                user.getId().value(),
                user.getEmail().value(),
                user.getFullName().value(),
                user.getPhone().map(PhoneNumber::value).orElse(null),
                user.getDateOfBirth().orElse(null),
                user.getGender().map(Gender::value).orElse(null),
                user.getIdDocumentNumber().map(IdDocumentNumber::value).orElse(null),
                user.getAddressLine().map(AddressLine::value).orElse(null),
                user.getRole().name(),
                user.getCreatedAt());
    }

    public UserResponse fromSummary(UserSummary summary) {
        return new UserResponse(
                summary.id(),
                summary.email(),
                summary.fullName(),
                summary.phone(),
                summary.dateOfBirth(),
                summary.gender(),
                summary.idDocumentNumber(),
                summary.addressLine(),
                summary.role(),
                summary.createdAt());
    }
}

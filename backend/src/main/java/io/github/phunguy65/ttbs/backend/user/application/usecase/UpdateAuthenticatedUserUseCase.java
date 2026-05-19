package io.github.phunguy65.ttbs.backend.user.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.AddressLine;
import io.github.phunguy65.ttbs.backend.shared.domain.EmailAddress;
import io.github.phunguy65.ttbs.backend.shared.domain.Gender;
import io.github.phunguy65.ttbs.backend.shared.domain.IdDocumentNumber;
import io.github.phunguy65.ttbs.backend.shared.domain.PersonName;
import io.github.phunguy65.ttbs.backend.shared.domain.PhoneNumber;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.user.application.command.UpdateUserCommand;
import io.github.phunguy65.ttbs.backend.user.application.response.UserResponse;
import io.github.phunguy65.ttbs.backend.user.domain.error.UserError;
import io.github.phunguy65.ttbs.backend.user.domain.model.User;
import io.github.phunguy65.ttbs.backend.user.domain.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateAuthenticatedUserUseCase {

    private final UserRepository userRepository;

    public UpdateAuthenticatedUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public Result<UserResponse, UserError> execute(UpdateUserCommand command) {
        User user = userRepository.findById(command.userId()).orElse(null);
        if (user == null) {
            return Result.failure(new UserError.UserNotFound());
        }

        EmailAddress newEmail = EmailAddress.of(command.email());
        EmailAddress currentEmail = user.getEmail();
        if (!newEmail.value().equals(currentEmail.value())) {
            boolean takenByOther = userRepository
                    .findByEmail(newEmail.value())
                    .filter(other -> !other.getId().equals(user.getId()))
                    .isPresent();
            if (takenByOther) {
                return Result.failure(new UserError.EmailAlreadyExists());
            }
        }

        PersonName newFullName = PersonName.of(command.fullName());
        PhoneNumber newPhone = PhoneNumber.ofNullable(command.phone());
        LocalDate newDateOfBirth = command.dateOfBirth();
        Gender newGender = Gender.ofNullable(command.gender());
        IdDocumentNumber newIdDocumentNumber =
                IdDocumentNumber.ofNullable(command.idDocumentNumber());
        AddressLine newAddressLine = AddressLine.ofNullable(command.addressLine());

        User updated = User.reconstitute(
                user.getId(),
                newEmail,
                user.getPasswordHash(),
                newFullName,
                newPhone,
                newDateOfBirth,
                newGender,
                newIdDocumentNumber,
                newAddressLine,
                user.getRole(),
                user.getCreatedAt(),
                Instant.now(),
                user.getDeletedAt());

        User saved = userRepository.save(updated);
        return Result.success(new UserResponse(
                saved.getId().value(),
                saved.getEmail().value(),
                saved.getFullName().value(),
                saved.getPhone().map(PhoneNumber::value).orElse(null),
                saved.getDateOfBirth().orElse(null),
                saved.getGender().map(Gender::value).orElse(null),
                saved.getIdDocumentNumber().map(IdDocumentNumber::value).orElse(null),
                saved.getAddressLine().map(AddressLine::value).orElse(null),
                saved.getRole().name(),
                saved.getCreatedAt()));
    }
}

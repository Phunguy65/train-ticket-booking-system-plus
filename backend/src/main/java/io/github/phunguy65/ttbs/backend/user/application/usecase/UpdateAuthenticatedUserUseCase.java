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
import io.github.phunguy65.ttbs.backend.user.application.response.UserResponseMapper;
import io.github.phunguy65.ttbs.backend.user.domain.error.UserError;
import io.github.phunguy65.ttbs.backend.user.domain.model.User;
import io.github.phunguy65.ttbs.backend.user.domain.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateAuthenticatedUserUseCase {

    private final UserRepository userRepository;
    private final UserResponseMapper userResponseMapper;

    public UpdateAuthenticatedUserUseCase(
            UserRepository userRepository, UserResponseMapper userResponseMapper) {
        this.userRepository = userRepository;
        this.userResponseMapper = userResponseMapper;
    }

    @Transactional
    public Result<UserResponse, UserError> execute(UpdateUserCommand command) {
        User user = userRepository.findById(command.userId()).orElse(null);
        if (user == null) {
            return Result.failure(new UserError.UserNotFound());
        }

        JsonNullable<String> emailField = command.email();
        EmailAddress currentEmail = user.getEmail();
        if (emailField.isPresent()) {
            EmailAddress newEmail = EmailAddress.of(emailField.get());
            if (!newEmail.value().equals(currentEmail.value())) {
                boolean takenByOther = userRepository
                        .findByEmail(newEmail.value())
                        .filter(other -> !other.getId().equals(user.getId()))
                        .isPresent();
                if (takenByOther) {
                    return Result.failure(new UserError.EmailAlreadyExists());
                }
            }
        }

        PersonName newFullName = command.fullName().isPresent()
                ? PersonName.of(command.fullName().get())
                : user.getFullName();
        EmailAddress newEmail =
                emailField.isPresent() ? EmailAddress.of(emailField.get()) : currentEmail;
        PhoneNumber newPhone = command.phone().isPresent()
                ? PhoneNumber.ofNullable(command.phone().get())
                : user.getPhone().orElse(null);
        LocalDate newDateOfBirth = command.dateOfBirth().isPresent()
                ? command.dateOfBirth().get()
                : user.getDateOfBirth().orElse(null);
        Gender newGender = command.gender().isPresent()
                ? Gender.ofNullable(command.gender().get())
                : user.getGender().orElse(null);
        IdDocumentNumber newIdDocumentNumber = command.idDocumentNumber().isPresent()
                ? IdDocumentNumber.ofNullable(command.idDocumentNumber().get())
                : user.getIdDocumentNumber().orElse(null);
        AddressLine newAddressLine = command.addressLine().isPresent()
                ? AddressLine.ofNullable(command.addressLine().get())
                : user.getAddressLine().orElse(null);

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
        return Result.success(userResponseMapper.fromUser(saved));
    }
}

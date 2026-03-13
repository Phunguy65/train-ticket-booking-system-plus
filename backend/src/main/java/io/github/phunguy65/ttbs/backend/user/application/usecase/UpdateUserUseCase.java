package io.github.phunguy65.ttbs.backend.user.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.user.application.command.UpdateUserCommand;
import io.github.phunguy65.ttbs.backend.user.application.response.UserDto;
import io.github.phunguy65.ttbs.backend.user.domain.error.UserError;
import io.github.phunguy65.ttbs.backend.user.domain.model.User;
import io.github.phunguy65.ttbs.backend.user.domain.repository.UserRepository;
import java.time.Instant;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateUserUseCase {

    private final UserRepository userRepository;

    public UpdateUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public Result<UserDto, UserError> execute(UpdateUserCommand command) {
        User user = userRepository.findById(command.userId()).orElse(null);
        if (user == null) {
            return Result.failure(new UserError.UserNotFound());
        }

        JsonNullable<String> emailField = command.email();
        if (emailField.isPresent()) {
            String newEmail = emailField.get();
            if (newEmail != null && !newEmail.equalsIgnoreCase(user.getEmail())) {
                boolean takenByOther = userRepository
                        .findByEmail(newEmail)
                        .filter(other -> !other.getId().equals(user.getId()))
                        .isPresent();
                if (takenByOther) {
                    return Result.failure(new UserError.EmailAlreadyExists());
                }
            }
        }

        String newFullName =
                command.fullName().isPresent() ? command.fullName().get() : user.getFullName();
        String newEmail = emailField.isPresent() ? emailField.get() : user.getEmail();
        String newPhone = command.phone().isPresent() ? command.phone().get() : user.getPhone();

        User updated = User.reconstitute(
                user.getId(),
                newEmail,
                user.getPasswordHash(),
                newFullName,
                newPhone,
                user.getRole(),
                user.getCreatedAt(),
                Instant.now(),
                user.getDeletedAt());

        User saved = userRepository.save(updated);
        return Result.success(toDto(saved));
    }

    private UserDto toDto(User user) {
        return new UserDto(
                user.getId().value(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getRole(),
                user.getCreatedAt());
    }
}

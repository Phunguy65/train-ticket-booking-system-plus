package io.github.phunguy65.ttbs.backend.user.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.shared.domain.UserId;
import io.github.phunguy65.ttbs.backend.user.application.dto.UserDto;
import io.github.phunguy65.ttbs.backend.user.domain.errors.UserError;
import io.github.phunguy65.ttbs.backend.user.domain.model.User;
import io.github.phunguy65.ttbs.backend.user.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetUserByIdUseCase {

    private final UserRepository userRepository;

    public GetUserByIdUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Result<UserDto, UserError> execute(UserId userId) {
        return userRepository
                .findById(userId)
                .map(user -> Result.<UserDto, UserError>success(toDto(user)))
                .orElseGet(() -> Result.failure(new UserError.UserNotFound()));
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

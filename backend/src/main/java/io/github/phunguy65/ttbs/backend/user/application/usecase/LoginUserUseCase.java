package io.github.phunguy65.ttbs.backend.user.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.user.application.command.LoginCommand;
import io.github.phunguy65.ttbs.backend.user.application.port.PasswordEncoder;
import io.github.phunguy65.ttbs.backend.user.application.port.RefreshTokenManager;
import io.github.phunguy65.ttbs.backend.user.application.response.LoginResultResponse;
import io.github.phunguy65.ttbs.backend.user.application.response.UserResponse;
import io.github.phunguy65.ttbs.backend.user.domain.error.UserError;
import io.github.phunguy65.ttbs.backend.user.domain.model.User;
import io.github.phunguy65.ttbs.backend.user.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenManager refreshTokenManager;

    public LoginUserUseCase(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            RefreshTokenManager refreshTokenManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenManager = refreshTokenManager;
    }

    @Transactional
    public Result<LoginResultResponse, UserError> execute(LoginCommand command) {
        var userOpt = userRepository.findByEmail(command.email());
        if (userOpt.isEmpty()) {
            return Result.failure(new UserError.InvalidCredentials());
        }

        User user = userOpt.get();
        if (!passwordEncoder.matches(command.password(), user.getPasswordHash())) {
            return Result.failure(new UserError.InvalidCredentials());
        }

        RefreshTokenManager.TokenPair tokens = refreshTokenManager.generateAndSaveTokens(user);

        return Result.success(
                new LoginResultResponse(tokens.accessToken(), tokens.refreshToken(), toDto(user)));
    }

    private UserResponse toDto(User user) {
        return new UserResponse(
                user.getId().value(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getRole().name(),
                user.getCreatedAt());
    }
}

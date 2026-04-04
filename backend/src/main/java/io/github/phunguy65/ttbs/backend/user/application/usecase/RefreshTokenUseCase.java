package io.github.phunguy65.ttbs.backend.user.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.AddressLine;
import io.github.phunguy65.ttbs.backend.shared.domain.Gender;
import io.github.phunguy65.ttbs.backend.shared.domain.IdDocumentNumber;
import io.github.phunguy65.ttbs.backend.shared.domain.PhoneNumber;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.user.application.command.RefreshTokenCommand;
import io.github.phunguy65.ttbs.backend.user.application.port.RefreshTokenManager;
import io.github.phunguy65.ttbs.backend.user.application.response.LoginResultResponse;
import io.github.phunguy65.ttbs.backend.user.application.response.UserResponse;
import io.github.phunguy65.ttbs.backend.user.domain.error.UserError;
import io.github.phunguy65.ttbs.backend.user.domain.model.User;
import io.github.phunguy65.ttbs.backend.user.domain.repository.RefreshTokenRepository;
import io.github.phunguy65.ttbs.backend.user.domain.repository.RefreshTokenRepository.RefreshTokenData;
import io.github.phunguy65.ttbs.backend.user.domain.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenUseCase {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final RefreshTokenManager refreshTokenManager;

    public RefreshTokenUseCase(
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository,
            RefreshTokenManager refreshTokenManager) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.refreshTokenManager = refreshTokenManager;
    }

    @Transactional
    public Result<LoginResultResponse, UserError> execute(RefreshTokenCommand command) {
        String incomingHash = refreshTokenManager.hashToken(command.refreshToken());

        Optional<RefreshTokenData> tokenData =
                refreshTokenRepository.findActiveByTokenHash(incomingHash);

        if (tokenData.isEmpty()) {
            return Result.failure(new UserError.InvalidRefreshToken());
        }

        RefreshTokenData token = tokenData.get();

        if (token.expiresAt().isBefore(Instant.now())) {
            refreshTokenRepository.revokeById(token.id());
            return Result.failure(new UserError.InvalidRefreshToken());
        }

        Optional<User> userOpt = userRepository.findById(token.userId());
        if (userOpt.isEmpty()) {
            return Result.failure(new UserError.InvalidRefreshToken());
        }

        User user = userOpt.get();

        refreshTokenRepository.revokeById(token.id());

        RefreshTokenManager.TokenPair tokens = refreshTokenManager.generateAndSaveTokens(user);

        return Result.success(new LoginResultResponse(
                tokens.accessToken(),
                tokens.refreshToken(),
                new UserResponse(
                        user.getId().value(),
                        user.getEmail().value(),
                        user.getFullName().value(),
                        user.getPhone().map(PhoneNumber::value).orElse(null),
                        user.getDateOfBirth().orElse(null),
                        user.getGender().map(Gender::value).orElse(null),
                        user.getIdDocumentNumber().map(IdDocumentNumber::value).orElse(null),
                        user.getAddressLine().map(AddressLine::value).orElse(null),
                        user.getRole().name(),
                        user.getCreatedAt())));
    }
}

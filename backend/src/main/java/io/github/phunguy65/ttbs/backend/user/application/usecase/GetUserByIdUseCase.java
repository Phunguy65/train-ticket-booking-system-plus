package io.github.phunguy65.ttbs.backend.user.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.user.application.response.UserResponse;
import io.github.phunguy65.ttbs.backend.user.application.response.UserResponseMapper;
import io.github.phunguy65.ttbs.backend.user.domain.error.UserError;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import io.github.phunguy65.ttbs.backend.user.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetUserByIdUseCase {

    private final UserRepository userRepository;
    private final UserResponseMapper userResponseMapper;

    public GetUserByIdUseCase(
            UserRepository userRepository, UserResponseMapper userResponseMapper) {
        this.userRepository = userRepository;
        this.userResponseMapper = userResponseMapper;
    }

    @Transactional(readOnly = true)
    public Result<UserResponse, UserError> execute(UserId userId) {
        return userRepository
                .findSummaryById(userId)
                .map(s ->
                        Result.<UserResponse, UserError>success(userResponseMapper.fromSummary(s)))
                .orElseGet(() -> Result.failure(new UserError.UserNotFound()));
    }
}

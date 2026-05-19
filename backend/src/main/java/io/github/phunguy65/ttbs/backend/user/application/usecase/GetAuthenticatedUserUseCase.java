package io.github.phunguy65.ttbs.backend.user.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.user.application.query.GetUserByIdQuery;
import io.github.phunguy65.ttbs.backend.user.application.response.UserResponse;
import io.github.phunguy65.ttbs.backend.user.domain.error.UserError;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import io.github.phunguy65.ttbs.backend.user.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetAuthenticatedUserUseCase {

    private final UserRepository userRepository;

    public GetAuthenticatedUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Result<UserResponse, UserError> execute(GetUserByIdQuery query) {
        return userRepository
                .findSummaryById(UserId.of(query.userId()))
                .map(s -> Result.<UserResponse, UserError>success(new UserResponse(
                        s.id(),
                        s.email(),
                        s.fullName(),
                        s.phone(),
                        s.dateOfBirth(),
                        s.gender(),
                        s.idDocumentNumber(),
                        s.addressLine(),
                        s.role(),
                        s.createdAt())))
                .orElseGet(() -> Result.failure(new UserError.UserNotFound()));
    }
}

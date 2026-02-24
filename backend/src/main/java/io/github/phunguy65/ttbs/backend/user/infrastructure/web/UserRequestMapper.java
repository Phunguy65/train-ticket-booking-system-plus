package io.github.phunguy65.ttbs.backend.user.infrastructure.web;

import io.github.phunguy65.ttbs.backend.shared.domain.UserId;
import io.github.phunguy65.ttbs.backend.user.application.command.CreateUserCommand;
import io.github.phunguy65.ttbs.backend.user.application.command.UpdateUserCommand;
import io.github.phunguy65.ttbs.backend.user.application.dto.CreateUserResult;
import io.github.phunguy65.ttbs.backend.user.application.dto.UserDto;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class UserRequestMapper {

    UserHttpResponse toResponse(UserDto dto) {
        return new UserHttpResponse(
                dto.id(),
                dto.email(),
                dto.fullName(),
                dto.phone(),
                dto.role().name(),
                dto.createdAt());
    }

    CreateUserCommand toCommand(CreateUserHttpRequest request) {
        return new CreateUserCommand(request.email(), request.fullName(), request.phone());
    }

    UpdateUserCommand toUpdateCommand(UUID userId, UpdateUserHttpRequest request) {
        return new UpdateUserCommand(
                UserId.of(userId), request.fullName(), request.email(), request.phone());
    }

    CreateUserHttpResponse toCreateResponse(CreateUserResult result) {
        UserDto user = result.user();
        return new CreateUserHttpResponse(
                user.id(),
                user.email(),
                user.fullName(),
                user.phone(),
                user.role().name(),
                user.createdAt(),
                result.temporaryPassword());
    }
}

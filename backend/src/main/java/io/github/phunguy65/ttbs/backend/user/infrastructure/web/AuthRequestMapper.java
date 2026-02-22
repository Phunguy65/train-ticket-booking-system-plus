package io.github.phunguy65.ttbs.backend.user.infrastructure.web;

import io.github.phunguy65.ttbs.backend.user.application.command.LoginCommand;
import io.github.phunguy65.ttbs.backend.user.application.command.RefreshTokenCommand;
import io.github.phunguy65.ttbs.backend.user.application.command.RegisterUserCommand;
import io.github.phunguy65.ttbs.backend.user.application.dto.LoginResultDto;
import io.github.phunguy65.ttbs.backend.user.application.dto.UserDto;
import org.springframework.stereotype.Component;

@Component
class AuthRequestMapper {

    RegisterUserCommand toCommand(RegisterHttpRequest request) {
        return new RegisterUserCommand(
                request.email(), request.password(), request.fullName(), request.phone());
    }

    LoginCommand toCommand(LoginHttpRequest request) {
        return new LoginCommand(request.email(), request.password());
    }

    RefreshTokenCommand toCommand(RefreshTokenHttpRequest request) {
        return new RefreshTokenCommand(request.refreshToken());
    }

    UserHttpResponse toResponse(UserDto dto) {
        return new UserHttpResponse(
                dto.id(),
                dto.email(),
                dto.fullName(),
                dto.phone(),
                dto.role().name(),
                dto.createdAt());
    }

    LoginHttpResponse toLoginResponse(LoginResultDto dto) {
        return new LoginHttpResponse(dto.accessToken(), dto.refreshToken(), toResponse(dto.user()));
    }
}

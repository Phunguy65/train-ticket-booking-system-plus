package io.github.phunguy65.ttbs.backend.user.infrastructure.web;

import io.github.phunguy65.ttbs.backend.user.application.command.RefreshTokenCommand;
import jakarta.validation.constraints.NotBlank;

record RefreshTokenHttpRequest(
        @NotBlank(message = "Refresh token is required") String refreshToken) {

    RefreshTokenCommand toCommand() {
        return new RefreshTokenCommand(refreshToken);
    }
}

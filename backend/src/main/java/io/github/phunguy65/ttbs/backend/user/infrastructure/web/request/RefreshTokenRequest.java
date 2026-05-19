package io.github.phunguy65.ttbs.backend.user.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.user.application.command.LogoutUserCommand;
import io.github.phunguy65.ttbs.backend.user.application.command.RefreshTokenCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Refresh-token payload used to rotate or revoke a session.")
public record RefreshTokenRequest(
        @Schema(
                description =
                        "Refresh token issued during login. The value is accepted on write and never returned.",
                writeOnly = true,
                example = "redacted-refresh-token")
        @NotBlank(message = "Refresh token is required") String refreshToken) {

    public RefreshTokenCommand toCommand() {
        return new RefreshTokenCommand(refreshToken);
    }

    public LogoutUserCommand toLogoutCommand() {
        return new LogoutUserCommand(refreshToken);
    }
}

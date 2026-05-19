package io.github.phunguy65.ttbs.backend.user.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.user.application.command.LoginCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Customer login payload.")
public record LoginRequest(
        @Schema(description = "Email address used to sign in.", example = "customer@example.com")
        @Email(message = "Must be a valid email address") @NotBlank(message = "Email is required") String email,

        @Schema(
                description =
                        "Account password. The value is accepted on write and never returned.",
                writeOnly = true,
                example = "********")
        @NotBlank(message = "Password is required") String password) {

    public LoginCommand toCommand() {
        return new LoginCommand(email, password);
    }
}

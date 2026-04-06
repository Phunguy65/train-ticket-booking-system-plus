package io.github.phunguy65.ttbs.backend.user.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.user.application.command.RegisterUserCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Customer registration payload.")
public record RegisterRequest(
        @Schema(description = "Email address used to sign in.", example = "customer@example.com")
        @Email(message = "Must be a valid email address") @NotBlank(message = "Email is required") String email,

        @Schema(
                description =
                        "Account password. The value is accepted on write and never returned.",
                writeOnly = true,
                example = "********")
        @NotBlank(message = "Password is required") @Size(min = 8, message = "Password must be at least 8 characters") String password,

        @Schema(description = "Customer full name.", example = "Nguyen Phuong")
        @NotBlank(message = "Full name is required") String fullName) {

    public RegisterUserCommand toCommand() {
        return new RegisterUserCommand(email, password, fullName);
    }
}

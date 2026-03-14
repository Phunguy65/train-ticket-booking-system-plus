package io.github.phunguy65.ttbs.backend.user.infrastructure.web;

import io.github.phunguy65.ttbs.backend.user.application.command.RegisterUserCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record RegisterHttpRequest(
        @Email(message = "Must be a valid email address") @NotBlank(message = "Email is required") String email,

        @NotBlank(message = "Password is required") @Size(min = 8, message = "Password must be at least 8 characters") String password,

        @NotBlank(message = "Full name is required") String fullName,

        String phone) {

    RegisterUserCommand toCommand() {
        return new RegisterUserCommand(email, password, fullName, phone);
    }
}

package io.github.phunguy65.ttbs.backend.user.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.user.application.command.CreateUserCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record CreateUserRequest(
        @Email(message = "Must be a valid email address") @NotBlank(message = "Email is required") String email,

        @NotBlank(message = "Full name is required") String fullName,

        String phone,

        LocalDate dateOfBirth,

        String gender,

        String idDocumentNumber,

        String addressLine) {

    public CreateUserCommand toCommand() {
        return new CreateUserCommand(
                email, fullName, phone, dateOfBirth, gender, idDocumentNumber, addressLine);
    }
}

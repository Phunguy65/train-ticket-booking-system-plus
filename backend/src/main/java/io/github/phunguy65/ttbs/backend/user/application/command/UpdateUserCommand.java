package io.github.phunguy65.ttbs.backend.user.application.command;

import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import java.time.LocalDate;

public record UpdateUserCommand(
        UserId userId,
        String fullName,
        String email,
        String phone,
        LocalDate dateOfBirth,
        String gender,
        String idDocumentNumber,
        String addressLine) {}

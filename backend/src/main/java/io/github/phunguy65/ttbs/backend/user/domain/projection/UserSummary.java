package io.github.phunguy65.ttbs.backend.user.domain.projection;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record UserSummary(
        UUID id,
        String email,
        String fullName,
        String phone,
        LocalDate dateOfBirth,
        String gender,
        String idDocumentNumber,
        String addressLine,
        String role,
        Instant createdAt) {}

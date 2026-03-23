package io.github.phunguy65.ttbs.backend.user.domain.projection;

import java.time.Instant;
import java.util.UUID;

public record UserSummary(
        UUID id, String email, String fullName, String phone, String role, Instant createdAt) {}

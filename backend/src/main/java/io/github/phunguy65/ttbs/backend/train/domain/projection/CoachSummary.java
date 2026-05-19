package io.github.phunguy65.ttbs.backend.train.domain.projection;

import java.time.Instant;
import java.util.UUID;

public record CoachSummary(
        UUID id, UUID trainId, int carNumber, int totalSeats, Instant createdAt) {}

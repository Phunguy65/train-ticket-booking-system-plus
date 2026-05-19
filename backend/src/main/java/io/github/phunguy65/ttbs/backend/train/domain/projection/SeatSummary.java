package io.github.phunguy65.ttbs.backend.train.domain.projection;

import java.time.Instant;
import java.util.UUID;

public record SeatSummary(UUID id, UUID coachId, String seatNumber, Instant createdAt) {}

package io.github.phunguy65.ttbs.backend.train.application.response;

import java.time.Instant;
import java.util.UUID;

public record SeatResponse(UUID id, UUID coachId, String seatNumber, Instant createdAt) {}

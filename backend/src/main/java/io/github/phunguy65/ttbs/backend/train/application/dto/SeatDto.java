package io.github.phunguy65.ttbs.backend.train.application.dto;

import java.time.Instant;
import java.util.UUID;

public record SeatDto(UUID id, UUID coachId, String seatNumber, Instant createdAt) {}

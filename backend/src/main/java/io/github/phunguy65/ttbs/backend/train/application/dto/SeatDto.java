package io.github.phunguy65.ttbs.backend.train.application.dto;

import java.time.Instant;
import java.util.UUID;

public record SeatDto(UUID id, UUID trainId, String seatNumber, Instant createdAt) {}

package io.github.phunguy65.ttbs.backend.train.application.dto;

import io.github.phunguy65.ttbs.backend.train.domain.model.SeatClass;
import java.time.Instant;
import java.util.UUID;

public record SeatDto(
        UUID id, UUID trainId, String seatNumber, SeatClass seatClass, Instant createdAt) {}

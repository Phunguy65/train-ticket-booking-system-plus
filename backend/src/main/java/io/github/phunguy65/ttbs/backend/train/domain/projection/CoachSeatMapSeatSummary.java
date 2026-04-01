package io.github.phunguy65.ttbs.backend.train.domain.projection;

import io.github.phunguy65.ttbs.backend.train.domain.model.RouteSeatAvailabilityStatus;
import java.util.UUID;

public record CoachSeatMapSeatSummary(
        UUID id, UUID coachId, String seatNumber, RouteSeatAvailabilityStatus status) {}

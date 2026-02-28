package io.github.phunguy65.ttbs.backend.train.application.command;

import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import java.util.List;

public record BulkSoftDeleteSeatsCommand(List<SeatId> seatIds) {}

package io.github.phunguy65.ttbs.backend.train.application.command;

import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;

public record SoftDeleteSeatCommand(SeatId seatId) {}

package io.github.phunguy65.ttbs.backend.train.application.command;

import java.util.UUID;

public record CreateSeatCommand(UUID coachId, String seatNumber) {}

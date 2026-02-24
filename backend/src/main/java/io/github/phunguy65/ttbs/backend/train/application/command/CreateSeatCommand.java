package io.github.phunguy65.ttbs.backend.train.application.command;

import io.github.phunguy65.ttbs.backend.train.domain.model.SeatClass;
import java.util.UUID;

public record CreateSeatCommand(UUID trainId, String seatNumber, SeatClass seatClass) {}

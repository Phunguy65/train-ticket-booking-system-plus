package io.github.phunguy65.ttbs.backend.train.application.command;

import io.github.phunguy65.ttbs.backend.train.domain.model.SeatClass;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import org.openapitools.jackson.nullable.JsonNullable;

public record UpdateSeatCommand(
        SeatId seatId, JsonNullable<String> seatNumber, JsonNullable<SeatClass> seatClass) {}

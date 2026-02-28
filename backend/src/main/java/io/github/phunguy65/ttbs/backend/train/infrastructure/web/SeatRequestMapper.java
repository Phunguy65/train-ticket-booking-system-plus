package io.github.phunguy65.ttbs.backend.train.infrastructure.web;

import io.github.phunguy65.ttbs.backend.train.application.command.CreateSeatCommand;
import io.github.phunguy65.ttbs.backend.train.application.command.UpdateSeatCommand;
import io.github.phunguy65.ttbs.backend.train.application.dto.SeatDto;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class SeatRequestMapper {

    CreateSeatCommand toCommand(UUID coachId, CreateSeatHttpRequest request) {
        return new CreateSeatCommand(coachId, request.seatNumber());
    }

    SeatHttpResponse toResponse(SeatDto dto) {
        return new SeatHttpResponse(dto.id(), dto.coachId(), dto.seatNumber(), dto.createdAt());
    }

    UpdateSeatCommand toUpdateCommand(UUID id, UpdateSeatHttpRequest request) {
        return new UpdateSeatCommand(SeatId.of(id), request.seatNumber());
    }
}

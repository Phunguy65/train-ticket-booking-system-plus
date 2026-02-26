package io.github.phunguy65.ttbs.backend.train.infrastructure.web;

import io.github.phunguy65.ttbs.backend.train.application.command.CreateSeatCommand;
import io.github.phunguy65.ttbs.backend.train.application.command.UpdateSeatCommand;
import io.github.phunguy65.ttbs.backend.train.application.dto.SeatDto;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatClass;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class SeatRequestMapper {

    CreateSeatCommand toCommand(UUID trainId, CreateSeatHttpRequest request) {
        return new CreateSeatCommand(
                trainId, request.seatNumber(), SeatClass.valueOf(request.seatClass()));
    }

    SeatHttpResponse toResponse(SeatDto dto) {
        return new SeatHttpResponse(
                dto.id(), dto.trainId(), dto.seatNumber(), dto.seatClass().name(), dto.createdAt());
    }

    UpdateSeatCommand toUpdateCommand(UUID id, UpdateSeatHttpRequest request) {
        return new UpdateSeatCommand(SeatId.of(id), request.seatNumber(), request.seatClass());
    }
}

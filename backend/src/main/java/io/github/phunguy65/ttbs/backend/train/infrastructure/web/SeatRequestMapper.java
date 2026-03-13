package io.github.phunguy65.ttbs.backend.train.infrastructure.web;

import io.github.phunguy65.ttbs.backend.train.application.command.BulkCreateSeatsCommand;
import io.github.phunguy65.ttbs.backend.train.application.command.CreateSeatCommand;
import io.github.phunguy65.ttbs.backend.train.application.response.SeatResponse;
import io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.BulkCreateSeatsHttpRequest;
import io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.CreateSeatHttpRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class SeatRequestMapper {

    CreateSeatCommand toCommand(UUID coachId, CreateSeatHttpRequest request) {
        return new CreateSeatCommand(coachId, request.seatNumber());
    }

    BulkCreateSeatsCommand toBulkCommand(UUID coachId, BulkCreateSeatsHttpRequest request) {
        List<BulkCreateSeatsCommand.SeatItem> items = request.seats().stream()
                .map(s -> new BulkCreateSeatsCommand.SeatItem(s.seatNumber()))
                .toList();
        return new BulkCreateSeatsCommand(coachId, items);
    }

    SeatHttpResponse toResponse(SeatResponse dto) {
        return new SeatHttpResponse(dto.id(), dto.coachId(), dto.seatNumber(), dto.createdAt());
    }

    List<SeatHttpResponse> toResponseList(List<SeatResponse> dtos) {
        return dtos.stream().map(this::toResponse).toList();
    }
}

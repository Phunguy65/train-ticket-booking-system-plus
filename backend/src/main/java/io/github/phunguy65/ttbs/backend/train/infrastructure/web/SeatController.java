package io.github.phunguy65.ttbs.backend.train.infrastructure.web;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.ErrorCode;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.FailData;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.JsendResponse;
import io.github.phunguy65.ttbs.backend.train.application.response.SeatResponse;
import io.github.phunguy65.ttbs.backend.train.application.usecase.GetAvailableSeatsForScheduledTripUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.GetCoachSeatMapByScheduledTripUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.GetSeatsByTrainUseCase;
import io.github.phunguy65.ttbs.backend.train.domain.error.ScheduledTripError;
import io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Seats")
class SeatController {

    private final GetSeatsByTrainUseCase getSeatsByTrainUseCase;
    private final GetAvailableSeatsForScheduledTripUseCase getAvailableSeatsForScheduledTripUseCase;
    private final GetCoachSeatMapByScheduledTripUseCase getCoachSeatMapByScheduledTripUseCase;

    SeatController(
            GetSeatsByTrainUseCase getSeatsByTrainUseCase,
            GetAvailableSeatsForScheduledTripUseCase getAvailableSeatsForScheduledTripUseCase,
            GetCoachSeatMapByScheduledTripUseCase getCoachSeatMapByScheduledTripUseCase) {
        this.getSeatsByTrainUseCase = getSeatsByTrainUseCase;
        this.getAvailableSeatsForScheduledTripUseCase = getAvailableSeatsForScheduledTripUseCase;
        this.getCoachSeatMapByScheduledTripUseCase = getCoachSeatMapByScheduledTripUseCase;
    }

    @Operation(operationId = "getTrainSeats", summary = "List seats for a train")
    @GetMapping(value = "/{version}/trains/{trainId}/seats", version = "1.0")
    ResponseEntity<JsendResponse<?>> getSeatsByTrain(
            @PathVariable UUID trainId, @ModelAttribute @Valid GetSeatsRequest request) {
        PageResponse<SeatResponse> result =
                getSeatsByTrainUseCase.execute(request.toQuery(trainId));
        return ResponseEntity.ok(JsendResponse.success(result));
    }

    @Operation(
            operationId = "getAvailableSeats",
            summary = "List available seats for a scheduled trip")
    @GetMapping(
            value = "/{version}/scheduled-trips/{scheduledTripId}/seats/available",
            version = "1.0")
    ResponseEntity<JsendResponse<?>> getAvailableSeats(
            @PathVariable UUID scheduledTripId,
            @ModelAttribute @Valid GetAvailableSeatsRequest request) {
        PageResponse<SeatResponse> result =
                getAvailableSeatsForScheduledTripUseCase.execute(request.toQuery(scheduledTripId));
        return ResponseEntity.ok(JsendResponse.success(result));
    }

    @Operation(operationId = "getCoachSeatMap", summary = "Get the seat map for a scheduled trip")
    @GetMapping(value = "/{version}/scheduled-trips/{scheduledTripId}/coach-seats", version = "1.0")
    ResponseEntity<JsendResponse<?>> getCoachSeatMap(
            @PathVariable UUID scheduledTripId,
            @ModelAttribute @Valid GetCoachSeatMapRequest request) {
        return getCoachSeatMapByScheduledTripUseCase
                .execute(request.toQuery(scheduledTripId))
                .fold(
                        dto -> ResponseEntity.ok(JsendResponse.success(dto)),
                        this::scheduledTripErrorResponse);
    }

    private ResponseEntity<JsendResponse<?>> scheduledTripErrorResponse(ScheduledTripError error) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(JsendResponse.fail(new FailData(
                        error.message(), ErrorCode.SCHEDULED_TRIP_NOT_FOUND, List.of())));
    }
}

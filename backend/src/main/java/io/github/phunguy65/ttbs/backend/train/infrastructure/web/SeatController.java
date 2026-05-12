package io.github.phunguy65.ttbs.backend.train.infrastructure.web;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.ErrorCode;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.FailData;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.JsendResponse;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.SuccessPayload;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.SuccessResponseKind;
import io.github.phunguy65.ttbs.backend.train.application.response.CoachSeatMapResponse;
import io.github.phunguy65.ttbs.backend.train.application.response.SeatResponse;
import io.github.phunguy65.ttbs.backend.train.application.usecase.GetAvailableSeatsForScheduledTripUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.GetCoachSeatMapByScheduledTripUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.GetSeatsByTrainUseCase;
import io.github.phunguy65.ttbs.backend.train.domain.error.ScheduledTripError;
import io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Trains")
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
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Paged seats for the train"),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid pagination parameters",
                content =
                        @Content(schema = @Schema(ref = "#/components/schemas/JsendFailResponse")))
    })
    @SuccessPayload(value = SeatResponse.class, kind = SuccessResponseKind.PAGE)
    @GetMapping(value = "/{version}/trains/{trainId}/seats", version = "1.0")
    ResponseEntity<JsendResponse<?>> getSeatsByTrain(
            @Parameter(description = "Train identifier") @PathVariable UUID trainId,
            @ParameterObject @Valid GetSeatsRequest request) {
        PageResponse<SeatResponse> result =
                getSeatsByTrainUseCase.execute(request.toQuery(trainId));
        return ResponseEntity.ok(JsendResponse.success(result));
    }

    @Operation(
            operationId = "getAvailableSeats",
            summary = "List available seats for a scheduled trip")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Paged available seats"),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid seat filter or pagination parameters",
                content =
                        @Content(schema = @Schema(ref = "#/components/schemas/JsendFailResponse"))),
        @ApiResponse(
                responseCode = "404",
                description = "Scheduled trip not found",
                content =
                        @Content(schema = @Schema(ref = "#/components/schemas/JsendFailResponse")))
    })
    @SuccessPayload(value = SeatResponse.class, kind = SuccessResponseKind.PAGE)
    @GetMapping(
            value = "/{version}/scheduled-trips/{scheduledTripId}/seats/available",
            version = "1.0")
    ResponseEntity<JsendResponse<?>> getAvailableSeats(
            @Parameter(description = "Scheduled trip identifier") @PathVariable
                    UUID scheduledTripId,
            @ParameterObject @Valid GetAvailableSeatsRequest request) {
        PageResponse<SeatResponse> result =
                getAvailableSeatsForScheduledTripUseCase.execute(request.toQuery(scheduledTripId));
        return ResponseEntity.ok(JsendResponse.success(result));
    }

    @Operation(operationId = "getCoachSeatMap", summary = "Get the seat map for a scheduled trip")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Coach seat map for the scheduled trip"),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid coach selection parameters",
                content =
                        @Content(schema = @Schema(ref = "#/components/schemas/JsendFailResponse"))),
        @ApiResponse(
                responseCode = "404",
                description = "Scheduled trip not found",
                content =
                        @Content(schema = @Schema(ref = "#/components/schemas/JsendFailResponse")))
    })
    @SuccessPayload(value = CoachSeatMapResponse.class, kind = SuccessResponseKind.PAGE)
    @GetMapping(value = "/{version}/scheduled-trips/{scheduledTripId}/coach-seats", version = "1.0")
    ResponseEntity<JsendResponse<?>> getCoachSeatMap(
            @Parameter(description = "Scheduled trip identifier") @PathVariable
                    UUID scheduledTripId,
            @ParameterObject @Valid GetCoachSeatMapRequest request) {
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

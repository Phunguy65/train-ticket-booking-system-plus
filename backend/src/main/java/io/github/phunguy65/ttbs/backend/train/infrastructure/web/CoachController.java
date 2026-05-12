package io.github.phunguy65.ttbs.backend.train.infrastructure.web;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.ErrorCode;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.FailData;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.JsendResponse;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.SuccessPayload;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.SuccessResponseKind;
import io.github.phunguy65.ttbs.backend.train.application.response.CoachResponse;
import io.github.phunguy65.ttbs.backend.train.application.usecase.GetCoachByIdUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.GetCoachesByTrainUseCase;
import io.github.phunguy65.ttbs.backend.train.domain.error.CoachError;
import io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.GetCoachByIdRequest;
import io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.GetCoachesRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Trains")
class CoachController {

    private final GetCoachByIdUseCase getCoachByIdUseCase;
    private final GetCoachesByTrainUseCase getCoachesByTrainUseCase;

    CoachController(
            GetCoachByIdUseCase getCoachByIdUseCase,
            GetCoachesByTrainUseCase getCoachesByTrainUseCase) {

        this.getCoachByIdUseCase = getCoachByIdUseCase;
        this.getCoachesByTrainUseCase = getCoachesByTrainUseCase;
    }

    @Operation(operationId = "getTrainCoaches", summary = "List coaches for a train")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Paged coaches for the train"),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid pagination parameters",
                content =
                        @Content(schema = @Schema(ref = "#/components/schemas/JsendFailResponse"))),
        @ApiResponse(
                responseCode = "404",
                description = "Train not found",
                content =
                        @Content(schema = @Schema(ref = "#/components/schemas/JsendFailResponse")))
    })
    @SuccessPayload(value = CoachResponse.class, kind = SuccessResponseKind.PAGE)
    @GetMapping(value = "/{version}/trains/{trainId}/coaches", version = "1.0")
    ResponseEntity<JsendResponse<?>> getCoachesByTrain(
            @Parameter(description = "Train identifier") @PathVariable UUID trainId,
            @ParameterObject @Valid GetCoachesRequest request) {
        PageResponse<CoachResponse> result =
                getCoachesByTrainUseCase.execute(request.toQuery(trainId));
        return ResponseEntity.ok(JsendResponse.success(result));
    }

    @Operation(operationId = "getTrainCoach", summary = "Get a coach by id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Coach detail"),
        @ApiResponse(
                responseCode = "404",
                description = "Coach or train not found",
                content =
                        @Content(schema = @Schema(ref = "#/components/schemas/JsendFailResponse")))
    })
    @SuccessPayload(CoachResponse.class)
    @GetMapping(value = "/{version}/trains/{trainId}/coaches/{id}", version = "1.0")
    ResponseEntity<JsendResponse<?>> getCoachById(
            @Parameter(description = "Train identifier") @PathVariable UUID trainId,
            @Parameter(description = "Coach identifier") @PathVariable UUID id,
            @ParameterObject GetCoachByIdRequest request) {
        return getCoachByIdUseCase
                .execute(request.toQuery(id, trainId))
                .fold(
                        dto -> ResponseEntity.ok(JsendResponse.success(dto)),
                        this::coachErrorResponse);
    }

    private ResponseEntity<JsendResponse<?>> coachErrorResponse(CoachError error) {
        HttpStatus status =
                switch (error) {
                    case CoachError.CoachNotFound e -> HttpStatus.NOT_FOUND;
                    case CoachError.CarNumberAlreadyExists e -> HttpStatus.CONFLICT;
                    case CoachError.TrainNotFound e -> HttpStatus.NOT_FOUND;
                    case CoachError.CoachInUse e -> HttpStatus.UNPROCESSABLE_CONTENT;
                    case CoachError.CarNumbersAlreadyExist e -> HttpStatus.CONFLICT;
                    case CoachError.DuplicateCarNumbersInRequest e ->
                        HttpStatus.UNPROCESSABLE_CONTENT;
                };
        ErrorCode code =
                switch (error) {
                    case CoachError.CoachNotFound e -> ErrorCode.COACH_NOT_FOUND;
                    case CoachError.CarNumberAlreadyExists e ->
                        ErrorCode.COACH_CAR_NUMBER_ALREADY_EXISTS;
                    case CoachError.TrainNotFound e -> ErrorCode.COACH_TRAIN_NOT_FOUND;
                    case CoachError.CoachInUse e -> ErrorCode.COACH_IN_USE;
                    case CoachError.CarNumbersAlreadyExist e ->
                        ErrorCode.COACH_CAR_NUMBERS_ALREADY_EXIST;
                    case CoachError.DuplicateCarNumbersInRequest e ->
                        ErrorCode.COACH_DUPLICATE_CAR_NUMBERS_IN_REQUEST;
                };

        return switch (error) {
            case CoachError.CoachInUse inUse ->
                ResponseEntity.status(status)
                        .body(JsendResponse.fail(Map.of(
                                "message", inUse.message(),
                                "code", code,
                                "conflictingIds", inUse.conflictingIds())));
            case CoachError.CarNumbersAlreadyExist conflict ->
                ResponseEntity.status(status)
                        .body(JsendResponse.fail(Map.of(
                                "message", conflict.message(),
                                "code", code,
                                "conflictingCarNumbers", conflict.conflictingCarNumbers())));
            case CoachError.DuplicateCarNumbersInRequest dup ->
                ResponseEntity.status(status)
                        .body(JsendResponse.fail(Map.of(
                                "message", dup.message(),
                                "code", code,
                                "duplicates", dup.duplicates())));
            default ->
                ResponseEntity.status(status)
                        .body(JsendResponse.fail(new FailData(error.message(), code, List.of())));
        };
    }
}

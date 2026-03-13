package io.github.phunguy65.ttbs.backend.train.infrastructure.web;

import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.ErrorCode;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.FailData;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.JsendResponse;
import io.github.phunguy65.ttbs.backend.train.application.command.BulkSoftDeleteSeatsCommand;
import io.github.phunguy65.ttbs.backend.train.application.command.SoftDeleteSeatCommand;
import io.github.phunguy65.ttbs.backend.train.application.usecase.BulkCreateSeatsUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.BulkSoftDeleteSeatsUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.CreateSeatUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.GetAvailableSeatsForRouteUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.GetSeatsByTrainUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.SoftDeleteSeatUseCase;
import io.github.phunguy65.ttbs.backend.train.domain.error.SeatError;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.BulkCreateSeatsHttpRequest;
import io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.BulkSoftDeleteSeatsHttpRequest;
import io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.CreateSeatHttpRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
class SeatController {

    private final CreateSeatUseCase createSeatUseCase;
    private final GetSeatsByTrainUseCase getSeatsByTrainUseCase;
    private final GetAvailableSeatsForRouteUseCase getAvailableSeatsForRouteUseCase;
    private final SoftDeleteSeatUseCase softDeleteSeatUseCase;
    private final BulkSoftDeleteSeatsUseCase bulkSoftDeleteSeatsUseCase;
    private final BulkCreateSeatsUseCase bulkCreateSeatsUseCase;
    private final SeatRequestMapper mapper;

    SeatController(
            CreateSeatUseCase createSeatUseCase,
            GetSeatsByTrainUseCase getSeatsByTrainUseCase,
            GetAvailableSeatsForRouteUseCase getAvailableSeatsForRouteUseCase,
            SoftDeleteSeatUseCase softDeleteSeatUseCase,
            BulkSoftDeleteSeatsUseCase bulkSoftDeleteSeatsUseCase,
            BulkCreateSeatsUseCase bulkCreateSeatsUseCase,
            SeatRequestMapper mapper) {
        this.createSeatUseCase = createSeatUseCase;
        this.getSeatsByTrainUseCase = getSeatsByTrainUseCase;
        this.getAvailableSeatsForRouteUseCase = getAvailableSeatsForRouteUseCase;
        this.softDeleteSeatUseCase = softDeleteSeatUseCase;
        this.bulkSoftDeleteSeatsUseCase = bulkSoftDeleteSeatsUseCase;
        this.bulkCreateSeatsUseCase = bulkCreateSeatsUseCase;
        this.mapper = mapper;
    }

    @PostMapping(value = "/{version}/trains/{trainId}/seats", version = "1.0")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<JsendResponse<?>> createSeat(
            @PathVariable UUID trainId, @Valid @RequestBody CreateSeatHttpRequest request) {
        return createSeatUseCase
                .execute(mapper.toCommand(trainId, request))
                .fold(
                        dto -> {
                            var location = ServletUriComponentsBuilder.fromCurrentRequest()
                                    .path("/{id}")
                                    .buildAndExpand(dto.id())
                                    .toUri();
                            return ResponseEntity.created(location)
                                    .body(JsendResponse.success(mapper.toResponse(dto)));
                        },
                        this::seatErrorResponse);
    }

    @GetMapping(value = "/{version}/trains/{trainId}/seats", version = "1.0")
    ResponseEntity<JsendResponse<?>> getSeatsByTrain(@PathVariable UUID trainId) {
        List<SeatHttpResponse> responses = getSeatsByTrainUseCase.execute(trainId).stream()
                .map(mapper::toResponse)
                .toList();
        return ResponseEntity.ok(JsendResponse.success(responses));
    }

    @GetMapping(value = "/{version}/routes/{routeId}/seats/available", version = "1.0")
    ResponseEntity<JsendResponse<?>> getAvailableSeats(@PathVariable UUID routeId) {
        List<SeatHttpResponse> responses =
                getAvailableSeatsForRouteUseCase.execute(routeId).stream()
                        .map(mapper::toResponse)
                        .toList();
        return ResponseEntity.ok(JsendResponse.success(responses));
    }

    @DeleteMapping(value = "/{version}/seats/{id}", version = "1.0")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<JsendResponse<?>> deleteById(@PathVariable UUID id) {
        return softDeleteSeatUseCase
                .execute(new SoftDeleteSeatCommand(SeatId.of(id)))
                .fold(v -> ResponseEntity.ok(JsendResponse.success()), this::seatErrorResponse);
    }

    @PostMapping(value = "/{version}/seats:bulkDelete", version = "1.0")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<JsendResponse<?>> bulkDelete(
            @Valid @RequestBody BulkSoftDeleteSeatsHttpRequest request) {
        List<SeatId> seatIds = request.seatIds().stream().map(SeatId::of).toList();
        return bulkSoftDeleteSeatsUseCase
                .execute(new BulkSoftDeleteSeatsCommand(seatIds))
                .fold(
                        deletedCount -> ResponseEntity.ok(
                                JsendResponse.success(Map.of("deletedCount", deletedCount))),
                        this::seatErrorResponse);
    }

    @PostMapping(value = "/{version}/coaches/{coachId}/seats:bulkCreate", version = "1.0")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<JsendResponse<?>> bulkCreateSeats(
            @PathVariable UUID coachId, @Valid @RequestBody BulkCreateSeatsHttpRequest request) {
        return bulkCreateSeatsUseCase
                .execute(mapper.toBulkCommand(coachId, request))
                .fold(
                        dtos -> ResponseEntity.status(HttpStatus.CREATED)
                                .body(JsendResponse.success(mapper.toResponseList(dtos))),
                        this::seatErrorResponse);
    }

    private ResponseEntity<JsendResponse<?>> seatErrorResponse(SeatError error) {
        HttpStatus status =
                switch (error) {
                    case SeatError.SeatNotFound e -> HttpStatus.NOT_FOUND;
                    case SeatError.TrainNotFound e -> HttpStatus.NOT_FOUND;
                    case SeatError.SeatNumberAlreadyExists e -> HttpStatus.CONFLICT;
                    case SeatError.SeatInUse e -> HttpStatus.UNPROCESSABLE_CONTENT;
                    case SeatError.CoachNotFound e -> HttpStatus.NOT_FOUND;
                    case SeatError.SeatNumbersAlreadyExist e -> HttpStatus.CONFLICT;
                    case SeatError.DuplicateSeatNumbersInRequest e ->
                        HttpStatus.UNPROCESSABLE_CONTENT;
                };
        ErrorCode code =
                switch (error) {
                    case SeatError.SeatNotFound e -> ErrorCode.SEAT_NOT_FOUND;
                    case SeatError.TrainNotFound e -> ErrorCode.TRAIN_NOT_FOUND;
                    case SeatError.SeatNumberAlreadyExists e ->
                        ErrorCode.SEAT_NUMBER_ALREADY_EXISTS;
                    case SeatError.SeatInUse e -> ErrorCode.SEAT_IN_USE;
                    case SeatError.CoachNotFound e -> ErrorCode.COACH_NOT_FOUND;
                    case SeatError.SeatNumbersAlreadyExist e ->
                        ErrorCode.SEAT_NUMBERS_ALREADY_EXIST;
                    case SeatError.DuplicateSeatNumbersInRequest e ->
                        ErrorCode.SEAT_DUPLICATE_SEAT_NUMBERS_IN_REQUEST;
                };

        if (error instanceof SeatError.SeatNumbersAlreadyExist conflict) {
            return ResponseEntity.status(status)
                    .body(JsendResponse.fail(Map.of(
                            "message", conflict.message(),
                            "code", code,
                            "conflictingNumbers", conflict.conflictingNumbers())));
        }

        if (error instanceof SeatError.DuplicateSeatNumbersInRequest dup) {
            return ResponseEntity.status(status)
                    .body(JsendResponse.fail(Map.of(
                            "message", dup.message(),
                            "code", code,
                            "duplicates", dup.duplicates())));
        }

        return ResponseEntity.status(status)
                .body(JsendResponse.fail(new FailData(error.message(), code, List.of())));
    }
}

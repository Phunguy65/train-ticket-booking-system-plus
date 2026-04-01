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
import io.github.phunguy65.ttbs.backend.train.domain.error.SeatError;
import io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
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

    @GetMapping(value = "/{version}/trains/{trainId}/seats", version = "1.0")
    ResponseEntity<JsendResponse<?>> getSeatsByTrain(
            @PathVariable UUID trainId, @ModelAttribute @Valid GetSeatsRequest request) {
        PageResponse<SeatResponse> result =
                getSeatsByTrainUseCase.execute(request.toQuery(trainId));
        return ResponseEntity.ok(JsendResponse.success(result));
    }

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

    private ResponseEntity<JsendResponse<?>> scheduledTripErrorResponse(ScheduledTripError error) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(JsendResponse.fail(new FailData(
                        error.message(), ErrorCode.SCHEDULED_TRIP_NOT_FOUND, List.of())));
    }
}

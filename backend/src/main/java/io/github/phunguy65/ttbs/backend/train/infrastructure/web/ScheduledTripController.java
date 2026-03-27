package io.github.phunguy65.ttbs.backend.train.infrastructure.web;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.ErrorCode;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.FailData;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.JsendResponse;
import io.github.phunguy65.ttbs.backend.train.application.response.ScheduledTripResponse;
import io.github.phunguy65.ttbs.backend.train.application.usecase.GetScheduledTripByIdUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.GetScheduledTripsUseCase;
import io.github.phunguy65.ttbs.backend.train.domain.error.ScheduledTripError;
import io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripId;
import io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.GetScheduledTripsRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ScheduledTripController {

    private final GetScheduledTripByIdUseCase getScheduledTripByIdUseCase;
    private final GetScheduledTripsUseCase getScheduledTripsUseCase;

    ScheduledTripController(
            GetScheduledTripByIdUseCase getScheduledTripByIdUseCase,
            GetScheduledTripsUseCase getScheduledTripsUseCase) {
        this.getScheduledTripByIdUseCase = getScheduledTripByIdUseCase;
        this.getScheduledTripsUseCase = getScheduledTripsUseCase;
    }

    @GetMapping(value = "/{version}/scheduled-trips", version = "1.0")
    ResponseEntity<JsendResponse<?>> list(@ModelAttribute @Valid GetScheduledTripsRequest request) {
        PageResponse<ScheduledTripResponse> result =
                getScheduledTripsUseCase.execute(request.toQuery());
        return ResponseEntity.ok(JsendResponse.success(result));
    }

    @GetMapping(value = "/{version}/scheduled-trips/{id}", version = "1.0")
    ResponseEntity<JsendResponse<?>> getById(@PathVariable UUID id) {
        return getScheduledTripByIdUseCase
                .execute(ScheduledTripId.of(id))
                .fold(dto -> ResponseEntity.ok(JsendResponse.success(dto)), this::errorResponse);
    }

    private ResponseEntity<JsendResponse<?>> errorResponse(ScheduledTripError error) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(JsendResponse.fail(new FailData(
                        error.message(), ErrorCode.SCHEDULED_TRIP_NOT_FOUND, List.of())));
    }
}

package io.github.phunguy65.ttbs.backend.train.infrastructure.web;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.SliceResponse;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.ErrorCode;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.FailData;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.JsendResponse;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.SuccessPayload;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.SuccessResponseKind;
import io.github.phunguy65.ttbs.backend.train.application.response.ScheduledTripResponse;
import io.github.phunguy65.ttbs.backend.train.application.response.SearchScheduledTripsResponse;
import io.github.phunguy65.ttbs.backend.train.application.usecase.GetScheduledTripByIdUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.GetScheduledTripsUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.SearchScheduledTripsUseCase;
import io.github.phunguy65.ttbs.backend.train.domain.error.ScheduledTripError;
import io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.GetScheduledTripByIdRequest;
import io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.GetScheduledTripsRequest;
import io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.SearchScheduledTripsRequest;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Trains")
class ScheduledTripController {

    private final GetScheduledTripByIdUseCase getScheduledTripByIdUseCase;
    private final GetScheduledTripsUseCase getScheduledTripsUseCase;
    private final SearchScheduledTripsUseCase searchScheduledTripsUseCase;

    ScheduledTripController(
            GetScheduledTripByIdUseCase getScheduledTripByIdUseCase,
            GetScheduledTripsUseCase getScheduledTripsUseCase,
            SearchScheduledTripsUseCase searchScheduledTripsUseCase) {
        this.getScheduledTripByIdUseCase = getScheduledTripByIdUseCase;
        this.getScheduledTripsUseCase = getScheduledTripsUseCase;
        this.searchScheduledTripsUseCase = searchScheduledTripsUseCase;
    }

    @Operation(operationId = "getScheduledTrips", summary = "List scheduled trips")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Paged scheduled trips"),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid pagination parameters",
                content =
                        @Content(schema = @Schema(ref = "#/components/schemas/JsendFailResponse")))
    })
    @SuccessPayload(value = ScheduledTripResponse.class, kind = SuccessResponseKind.PAGE)
    @GetMapping(value = "/{version}/scheduled-trips", version = "1.0")
    ResponseEntity<JsendResponse<?>> list(@ModelAttribute @Valid GetScheduledTripsRequest request) {
        PageResponse<ScheduledTripResponse> result =
                getScheduledTripsUseCase.execute(request.toQuery());
        return ResponseEntity.ok(JsendResponse.success(result));
    }

    @Operation(operationId = "filterScheduledTrips", summary = "Filter scheduled trips")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Cursor-paged scheduled trip search results"),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid scheduled trip filter parameters",
                content =
                        @Content(schema = @Schema(ref = "#/components/schemas/JsendFailResponse")))
    })
    @SuccessPayload(value = SearchScheduledTripsResponse.class, kind = SuccessResponseKind.SLICE)
    @GetMapping(value = "/{version}/scheduled-trips:filter", version = "1.0")
    ResponseEntity<JsendResponse<?>> filter(
            @ModelAttribute @Valid SearchScheduledTripsRequest request) {
        SliceResponse<SearchScheduledTripsResponse> result =
                searchScheduledTripsUseCase.execute(request.toQuery());
        String message = result.content().isEmpty()
                ? "No scheduled trips matched the selected filters."
                : null;
        return ResponseEntity.ok(new JsendResponse<>("success", result, message));
    }

    @Operation(operationId = "getScheduledTrip", summary = "Get a scheduled trip by id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Scheduled trip detail"),
        @ApiResponse(
                responseCode = "404",
                description = "Scheduled trip not found",
                content =
                        @Content(schema = @Schema(ref = "#/components/schemas/JsendFailResponse")))
    })
    @SuccessPayload(ScheduledTripResponse.class)
    @GetMapping(value = "/{version}/scheduled-trips/{id}", version = "1.0")
    ResponseEntity<JsendResponse<?>> getById(
            @Parameter(description = "Scheduled trip identifier") @PathVariable UUID id,
            @ModelAttribute GetScheduledTripByIdRequest request) {
        return getScheduledTripByIdUseCase
                .execute(request.toQuery(id))
                .fold(dto -> ResponseEntity.ok(JsendResponse.success(dto)), this::errorResponse);
    }

    private ResponseEntity<JsendResponse<?>> errorResponse(ScheduledTripError error) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(JsendResponse.fail(new FailData(
                        error.message(), ErrorCode.SCHEDULED_TRIP_NOT_FOUND, List.of())));
    }
}

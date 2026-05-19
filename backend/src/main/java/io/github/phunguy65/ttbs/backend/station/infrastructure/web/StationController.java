package io.github.phunguy65.ttbs.backend.station.infrastructure.web;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.ErrorCode;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.FailData;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.JsendResponse;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.SuccessPayload;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.SuccessResponseKind;
import io.github.phunguy65.ttbs.backend.station.application.response.StationResponse;
import io.github.phunguy65.ttbs.backend.station.application.response.StationSearchResponse;
import io.github.phunguy65.ttbs.backend.station.application.usecase.GetStationByIdUseCase;
import io.github.phunguy65.ttbs.backend.station.application.usecase.GetStationsUseCase;
import io.github.phunguy65.ttbs.backend.station.application.usecase.SearchStationsUseCase;
import io.github.phunguy65.ttbs.backend.station.domain.error.StationError;
import io.github.phunguy65.ttbs.backend.station.infrastructure.web.request.GetStationByIdRequest;
import io.github.phunguy65.ttbs.backend.station.infrastructure.web.request.GetStationsRequest;
import io.github.phunguy65.ttbs.backend.station.infrastructure.web.request.SearchStationsRequest;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Stations")
class StationController {

    private final GetStationByIdUseCase getStationByIdUseCase;
    private final GetStationsUseCase getStationsUseCase;
    private final SearchStationsUseCase searchStationsUseCase;

    StationController(
            GetStationByIdUseCase getStationByIdUseCase,
            GetStationsUseCase getStationsUseCase,
            SearchStationsUseCase searchStationsUseCase) {
        this.getStationByIdUseCase = getStationByIdUseCase;
        this.getStationsUseCase = getStationsUseCase;
        this.searchStationsUseCase = searchStationsUseCase;
    }

    @Operation(operationId = "getStations", summary = "List stations")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Paged station catalog"),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid pagination parameters",
                content =
                        @Content(schema = @Schema(ref = "#/components/schemas/JsendFailResponse")))
    })
    @SuccessPayload(value = StationResponse.class, kind = SuccessResponseKind.PAGE)
    @GetMapping(value = "/{version}/stations", version = "1.0")
    ResponseEntity<JsendResponse<?>> list(@ParameterObject @Valid GetStationsRequest request) {
        PageResponse<StationResponse> result = getStationsUseCase.execute(request.toQuery());

        return ResponseEntity.ok(JsendResponse.success(result));
    }

    @Operation(operationId = "searchStations", summary = "Search stations by keyword")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Matching station suggestions"),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid station search parameters",
                content =
                        @Content(schema = @Schema(ref = "#/components/schemas/JsendFailResponse")))
    })
    @SuccessPayload(value = StationSearchResponse.class, kind = SuccessResponseKind.ARRAY)
    @GetMapping(value = "/{version}/stations/search", version = "1.0")
    ResponseEntity<JsendResponse<?>> search(@ParameterObject @Valid SearchStationsRequest request) {
        List<StationSearchResponse> result = searchStationsUseCase.execute(request.toQuery());
        String message = result.isEmpty() ? "No stations matched your search." : null;
        return ResponseEntity.ok(new JsendResponse<>("success", result, message));
    }

    @Operation(operationId = "getStation", summary = "Get a station by id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Station detail"),
        @ApiResponse(
                responseCode = "404",
                description = "Station not found",
                content =
                        @Content(schema = @Schema(ref = "#/components/schemas/JsendFailResponse")))
    })
    @SuccessPayload(StationResponse.class)
    @GetMapping(value = "/{version}/stations/{id}", version = "1.0")
    ResponseEntity<JsendResponse<?>> getById(
            @Parameter(description = "Station identifier") @PathVariable UUID id,
            @ParameterObject GetStationByIdRequest request) {
        return getStationByIdUseCase
                .execute(request.toQuery(id))
                .fold(
                        dto -> ResponseEntity.ok(JsendResponse.success(dto)),
                        error -> errorResponse(error));
    }

    private ResponseEntity<JsendResponse<?>> errorResponse(StationError error) {
        HttpStatus status =
                switch (error) {
                    case StationError.StationNotFound e -> HttpStatus.NOT_FOUND;
                    case StationError.StationCodeAlreadyExists e -> HttpStatus.CONFLICT;
                    case StationError.StationInUse e -> HttpStatus.UNPROCESSABLE_CONTENT;
                };
        ErrorCode code =
                switch (error) {
                    case StationError.StationNotFound e -> ErrorCode.STATION_NOT_FOUND;
                    case StationError.StationCodeAlreadyExists e ->
                        ErrorCode.STATION_CODE_ALREADY_EXISTS;
                    case StationError.StationInUse e -> ErrorCode.STATION_IN_USE;
                };
        return ResponseEntity.status(status)
                .body(JsendResponse.fail(new FailData(error.message(), code, List.of())));
    }
}

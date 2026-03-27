package io.github.phunguy65.ttbs.backend.train.infrastructure.web;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.ErrorCode;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.FailData;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.JsendResponse;
import io.github.phunguy65.ttbs.backend.train.application.response.RouteResponse;
import io.github.phunguy65.ttbs.backend.train.application.usecase.GetRouteByIdUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.GetRoutesUseCase;
import io.github.phunguy65.ttbs.backend.train.domain.error.RouteError;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.GetRoutesRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
class RouteController {

    private final GetRouteByIdUseCase getRouteByIdUseCase;
    private final GetRoutesUseCase getRoutesUseCase;

    RouteController(GetRouteByIdUseCase getRouteByIdUseCase, GetRoutesUseCase getRoutesUseCase) {
        this.getRouteByIdUseCase = getRouteByIdUseCase;
        this.getRoutesUseCase = getRoutesUseCase;
    }

    @GetMapping(value = "/{version}/routes", version = "1.0")
    ResponseEntity<JsendResponse<?>> list(@ModelAttribute @Valid GetRoutesRequest request) {
        PageResponse<RouteResponse> result = getRoutesUseCase.execute(request.toQuery());

        return ResponseEntity.ok(JsendResponse.success(result));
    }

    @GetMapping(value = "/{version}/routes/{id}", version = "1.0")
    ResponseEntity<JsendResponse<?>> getById(@PathVariable UUID id) {
        return getRouteByIdUseCase
                .execute(RouteId.of(id))
                .fold(
                        dto -> ResponseEntity.ok(JsendResponse.success(dto)),
                        error -> errorResponse(error));
    }

    private ResponseEntity<JsendResponse<?>> errorResponse(RouteError error) {
        HttpStatus status =
                switch (error) {
                    case RouteError.RouteNotFound e -> HttpStatus.NOT_FOUND;
                    case RouteError.RoutesNotFound e -> HttpStatus.UNPROCESSABLE_CONTENT;
                };
        ErrorCode code =
                switch (error) {
                    case RouteError.RouteNotFound e -> ErrorCode.ROUTE_NOT_FOUND;
                    case RouteError.RoutesNotFound e -> ErrorCode.ROUTES_NOT_FOUND;
                };
        return ResponseEntity.status(status)
                .body(JsendResponse.fail(new FailData(error.message(), code, List.of())));
    }
}

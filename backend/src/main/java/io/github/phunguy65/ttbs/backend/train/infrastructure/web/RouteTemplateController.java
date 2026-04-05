package io.github.phunguy65.ttbs.backend.train.infrastructure.web;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.ErrorCode;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.FailData;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.JsendResponse;
import io.github.phunguy65.ttbs.backend.train.application.response.RouteTemplateResponse;
import io.github.phunguy65.ttbs.backend.train.application.usecase.GetRouteTemplateByIdUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.GetRouteTemplatesUseCase;
import io.github.phunguy65.ttbs.backend.train.domain.error.RouteTemplateError;
import io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.GetRouteTemplateByIdRequest;
import io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.GetRouteTemplatesRequest;
import io.swagger.v3.oas.annotations.Operation;
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
@Tag(name = "Route Templates")
class RouteTemplateController {

    private final GetRouteTemplateByIdUseCase getRouteTemplateByIdUseCase;
    private final GetRouteTemplatesUseCase getRouteTemplatesUseCase;

    RouteTemplateController(
            GetRouteTemplateByIdUseCase getRouteTemplateByIdUseCase,
            GetRouteTemplatesUseCase getRouteTemplatesUseCase) {
        this.getRouteTemplateByIdUseCase = getRouteTemplateByIdUseCase;
        this.getRouteTemplatesUseCase = getRouteTemplatesUseCase;
    }

    @Operation(operationId = "getRouteTemplates", summary = "List route templates")
    @GetMapping(value = "/{version}/route-templates", version = "1.0")
    ResponseEntity<JsendResponse<?>> list(@ModelAttribute @Valid GetRouteTemplatesRequest request) {
        PageResponse<RouteTemplateResponse> result =
                getRouteTemplatesUseCase.execute(request.toQuery());
        return ResponseEntity.ok(JsendResponse.success(result));
    }

    @Operation(operationId = "getRouteTemplate", summary = "Get a route template by id")
    @GetMapping(value = "/{version}/route-templates/{id}", version = "1.0")
    ResponseEntity<JsendResponse<?>> getById(
            @PathVariable UUID id, @ModelAttribute GetRouteTemplateByIdRequest request) {
        return getRouteTemplateByIdUseCase
                .execute(request.toQuery(id))
                .fold(dto -> ResponseEntity.ok(JsendResponse.success(dto)), this::errorResponse);
    }

    private ResponseEntity<JsendResponse<?>> errorResponse(RouteTemplateError error) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(JsendResponse.fail(new FailData(
                        error.message(), ErrorCode.ROUTE_TEMPLATE_NOT_FOUND, List.of())));
    }
}

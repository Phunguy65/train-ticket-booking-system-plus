package io.github.phunguy65.ttbs.backend.shared.infrastructure.web;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.SliceResponse;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.core.ResolvableType;
import org.springframework.web.method.HandlerMethod;

public final class JsendSuccessResponseCustomizer
        implements OperationCustomizer, OpenApiCustomizer {

    private static final String APPLICATION_JSON = "application/json";
    private static final String TECHNICAL_FAILURE_RESPONSE_CODE = "500";
    private static final String JSEND_ERROR_RESPONSE_REF =
            "#/components/schemas/JsendErrorResponse";

    // Springdoc invokes this singleton bean across operation scans before the final OpenAPI object
    // is built.
    // We accumulate referenced component schemas here, then register them in the OpenAPI customise
    // step.
    private final Map<String, Schema<?>> referencedSchemas = new LinkedHashMap<>();

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        if (operation.getResponses() == null) {
            return operation;
        }

        ensureTechnicalFailureResponse(operation);

        SuccessPayload successPayload = handlerMethod.getMethodAnnotation(SuccessPayload.class);
        if (successPayload == null) {
            return operation;
        }

        operation.getResponses().forEach((responseCode, response) -> {
            if (isTargetSuccessResponse(successPayload, responseCode)) {
                applySuccessSchema(response, successPayload);
            }
        });

        return operation;
    }

    @Override
    public void customise(OpenAPI openApi) {
        if (referencedSchemas.isEmpty()) {
            return;
        }

        Components components = openApi.getComponents();
        if (components == null) {
            components = new Components();
            openApi.setComponents(components);
        }
        if (components.getSchemas() == null) {
            components.setSchemas(new LinkedHashMap<>());
        }

        referencedSchemas.forEach(components.getSchemas()::putIfAbsent);
        referencedSchemas.clear();
    }

    private boolean isTargetSuccessResponse(SuccessPayload successPayload, String responseCode) {
        if (!responseCode.startsWith("2")) {
            return false;
        }
        return successPayload.responseCode().isBlank()
                || successPayload.responseCode().equals(responseCode);
    }

    private void applySuccessSchema(ApiResponse response, SuccessPayload successPayload) {
        if (successPayload.value() == Void.class) {
            response.setContent(null);
            return;
        }

        Schema<?> schema = resolveSchema(successPayload);
        Content content = response.getContent();
        if (content == null) {
            content = new Content();
            response.setContent(content);
        }

        MediaType mediaType = content.get(APPLICATION_JSON);
        if (mediaType == null) {
            mediaType = new MediaType();
            content.addMediaType(APPLICATION_JSON, mediaType);
        }
        mediaType.setSchema(schema);
    }

    private void ensureTechnicalFailureResponse(Operation operation) {
        operation
                .getResponses()
                .computeIfAbsent(TECHNICAL_FAILURE_RESPONSE_CODE, ignored -> new ApiResponse()
                        .description("Unexpected technical failure.")
                        .content(new Content()
                                .addMediaType(
                                        APPLICATION_JSON,
                                        new MediaType()
                                                .schema(new Schema<>()
                                                        .$ref(JSEND_ERROR_RESPONSE_REF)))));
    }

    private Schema<?> resolveSchema(SuccessPayload successPayload) {
        Type payloadType =
                switch (successPayload.kind()) {
                    case OBJECT -> successPayload.value();
                    case ARRAY -> successPayload.value();
                    case PAGE ->
                        ResolvableType.forClassWithGenerics(
                                        PageResponse.class, successPayload.value())
                                .getType();
                    case SLICE ->
                        ResolvableType.forClassWithGenerics(
                                        SliceResponse.class, successPayload.value())
                                .getType();
                };

        ResolvedSchema resolvedSchema = ModelConverters.getInstance()
                .resolveAsResolvedSchema(new AnnotatedType(payloadType).resolveAsRef(true));
        if (resolvedSchema.referencedSchemas != null) {
            resolvedSchema.referencedSchemas.forEach(referencedSchemas::putIfAbsent);
        }

        if (successPayload.kind() == SuccessResponseKind.ARRAY) {
            return new ArraySchema().items(resolvedSchema.schema);
        }

        return resolvedSchema.schema;
    }
}

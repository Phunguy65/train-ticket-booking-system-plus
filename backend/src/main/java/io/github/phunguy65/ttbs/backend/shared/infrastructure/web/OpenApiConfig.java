package io.github.phunguy65.ttbs.backend.shared.infrastructure.web;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.SliceResponse;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH_SCHEME = "bearerAuth";
    private static final String FAIL_DATA_SCHEMA = "FailData";
    private static final String PAGE_RESPONSE_SCHEMA = "PageResponse";
    private static final String SLICE_RESPONSE_SCHEMA = "SliceResponse";
    private static final String JSEND_FAIL_RESPONSE_SCHEMA = "JsendFailResponse";
    private static final String JSEND_SUCCESS_RESPONSE_SCHEMA = "JsendSuccessResponse";
    private static final String JSEND_ERROR_RESPONSE_SCHEMA = "JsendErrorResponse";

    @Bean
    OpenAPI customerOpenApi() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("TTBS Customer API")
                                .version("1.0")
                                .description(
                                        "Customer-facing API contract for the train ticket booking system."))
                .servers(List.of(new Server().url("/").description("Versioned customer API root")))
                .addTagsItem(new io.swagger.v3.oas.models.tags.Tag()
                        .name("Authentication")
                        .description("Customer identity, session, and profile endpoints."))
                .addTagsItem(new io.swagger.v3.oas.models.tags.Tag()
                        .name("Bookings")
                        .description(
                                "Customer booking creation, lookup, and cancellation endpoints."))
                .addTagsItem(new io.swagger.v3.oas.models.tags.Tag()
                        .name("Payments")
                        .description("Customer payment lookup endpoints."))
                .addTagsItem(new io.swagger.v3.oas.models.tags.Tag()
                        .name("Stations")
                        .description("Station browse and search endpoints."))
                .addTagsItem(new io.swagger.v3.oas.models.tags.Tag()
                        .name("Trains")
                        .description("Train, coach, seat, and route lookup endpoints."))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH_SCHEME))
                .components(customerComponents());
    }

    @Bean
    JsendSuccessResponseCustomizer jsendSuccessResponseCustomizer() {
        return new JsendSuccessResponseCustomizer();
    }

    @Bean
    GroupedOpenApi customerApi(JsendSuccessResponseCustomizer jsendSuccessResponseCustomizer) {
        return GroupedOpenApi.builder()
                .group("customer")
                .pathsToMatch("/api/**")
                .pathsToExclude("/api/sse/**", "/api/**/webhooks/**")
                .addOperationCustomizer(jsendSuccessResponseCustomizer)
                .addOpenApiCustomizer(jsendSuccessResponseCustomizer)
                .addOpenApiCustomizer(nullableResponseFieldsCustomizer())
                .addOpenApiCustomizer(versionPathCustomizer())
                .build();
    }

    private OpenApiCustomizer nullableResponseFieldsCustomizer() {
        return openApi -> {
            Components components = openApi.getComponents();
            if (components == null || components.getSchemas() == null) {
                return;
            }

            Map<String, Schema> schemas = components.getSchemas();
            markNullable(
                    schemas,
                    "UserResponse",
                    "phone",
                    "dateOfBirth",
                    "gender",
                    "idDocumentNumber",
                    "addressLine");
            markNullable(
                    schemas,
                    "PassengerInfoResponse",
                    "phone",
                    "dateOfBirth",
                    "gender",
                    "idDocumentNumber",
                    "addressLine");
            markNullable(schemas, "BookingDetailResponse", "trip", "payment");
            markNullable(schemas, "Trip", "train");
            markNullable(schemas, "PaymentDetailResponse", "checkoutUrl", "stripePaymentIntentId");
            markNullable(schemas, "PaymentResponse", "checkoutUrl");
            markNullable(schemas, "SliceResponseSearchScheduledTripsResponse", "nextCursor");
        };
    }

    private void markNullable(
            Map<String, Schema> schemas, String schemaName, String... propertyNames) {
        Schema schema = schemas.get(schemaName);
        if (schema == null || schema.getProperties() == null) {
            return;
        }

        for (String propertyName : propertyNames) {
            Schema property = (Schema) schema.getProperties().get(propertyName);
            if (property != null) {
                property.setNullable(true);
                if (property.getType() != null) {
                    property.setTypes(Set.of(property.getType(), "null"));
                    property.setType(null);
                }
            }
        }
    }

    private OpenApiCustomizer versionPathCustomizer() {
        return openApi -> {
            var paths = openApi.getPaths();
            if (paths == null) {
                return;
            }

            var rewritten = new Paths();
            paths.forEach((path, pathItem) ->
                    rewritten.addPathItem(path.replace("{version}", "v1"), pathItem));
            openApi.setPaths(rewritten);
        };
    }

    private Schema<?> jsendFailResponseSchema() {
        StringSchema statusSchema = new StringSchema();
        statusSchema.addEnumItemObject("fail");
        return new ObjectSchema()
                .description("Shared JSend fail envelope used for validation and domain errors.")
                .addProperty("status", statusSchema)
                .addProperty(
                        "data", new Schema<>().$ref("#/components/schemas/" + FAIL_DATA_SCHEMA));
    }

    private Schema<?> jsendSuccessResponseSchema() {
        StringSchema statusSchema = new StringSchema();
        statusSchema.addEnumItemObject("success");
        return new ObjectSchema()
                .description(
                        "JSend success envelope before endpoint-specific 2xx payload unwrapping.")
                .addProperty("status", statusSchema)
                .addProperty(
                        "data",
                        new ObjectSchema()
                                .description(
                                        "Endpoint-specific success payload. @SuccessPayload overrides this shape per operation."));
    }

    private Schema<?> jsendErrorResponseSchema() {
        StringSchema statusSchema = new StringSchema();
        statusSchema.addEnumItemObject("error");
        return new ObjectSchema()
                .description("Shared JSend error envelope used for unexpected technical failures.")
                .addProperty("status", statusSchema)
                .addProperty(
                        "message",
                        new StringSchema()
                                .description("Human-readable technical failure message."));
    }

    private Components customerComponents() {
        Components components = new Components()
                .addSecuritySchemes(
                        BEARER_AUTH_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT"));

        registerSchema(components, FAIL_DATA_SCHEMA, FailData.class, null);
        registerSchema(components, "Violation", Violation.class, null);
        registerSchema(components, "ErrorCode", ErrorCode.class, null);
        registerSchema(components, "ViolationCode", ViolationCode.class, null);
        registerSchema(
                components,
                PAGE_RESPONSE_SCHEMA,
                PageResponse.class,
                "Offset-based pagination wrapper with page, size, total, and navigation metadata.");
        registerSchema(
                components,
                SLICE_RESPONSE_SCHEMA,
                SliceResponse.class,
                "Cursor-based pagination wrapper with size, hasNext, and nextCursor metadata.");

        components.addSchemas(JSEND_FAIL_RESPONSE_SCHEMA, jsendFailResponseSchema());
        components.addSchemas(JSEND_SUCCESS_RESPONSE_SCHEMA, jsendSuccessResponseSchema());
        components.addSchemas(JSEND_ERROR_RESPONSE_SCHEMA, jsendErrorResponseSchema());
        return components;
    }

    private void registerSchema(
            Components components, String schemaName, Class<?> schemaType, String description) {
        ResolvedSchema resolvedSchema = ModelConverters.getInstance()
                .resolveAsResolvedSchema(new AnnotatedType(schemaType));
        Schema<?> rootSchema = resolvedSchema.schema;
        if (rootSchema == null) {
            return;
        }

        rootSchema.setName(schemaName);
        if (description != null && rootSchema.getDescription() == null) {
            rootSchema.setDescription(description);
        }
        components.addSchemas(schemaName, rootSchema);

        Map<String, Schema> referencedSchemas = resolvedSchema.referencedSchemas;
        if (referencedSchemas != null) {
            referencedSchemas.forEach((name, schema) -> {
                if (components.getSchemas() == null || !components.getSchemas().containsKey(name)) {
                    components.addSchemas(name, schema);
                }
            });
        }
    }
}

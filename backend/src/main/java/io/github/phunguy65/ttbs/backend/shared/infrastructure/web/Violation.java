package io.github.phunguy65.ttbs.backend.shared.infrastructure.web;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents a single field-level validation failure inside a {@link FailData#errors()} list.
 *
 * @param field   the name of the request field that failed validation
 * @param message a human-readable description of why validation failed
 * @param code    machine-readable category code for frontend i18n
 */
@Schema(description = "Field-level validation issue inside a fail response.")
public record Violation(
        @Schema(description = "Request field that failed validation.")
        String field,

        @Schema(description = "Human-readable validation message.")
        String message,

        @Schema(
                description = "Machine-readable validation category.",
                ref = "#/components/schemas/ViolationCode")
        ViolationCode code) {}

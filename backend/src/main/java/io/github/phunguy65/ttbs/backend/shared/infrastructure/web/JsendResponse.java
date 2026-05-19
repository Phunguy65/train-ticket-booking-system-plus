package io.github.phunguy65.ttbs.backend.shared.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * JSend-compliant response envelope.
 *
 * <ul>
 *   <li>{@code success} – 2xx: operation succeeded, {@code data} contains the result.
 *   <li>{@code fail} – 4xx: business/validation error, {@code data} contains error details.
 *   <li>{@code error} – 5xx: technical/server error, {@code message} describes the problem.
 * </ul>
 *
 * @see <a href="https://github.com/omniti-labs/jsend">JSend specification</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Runtime JSend response envelope used by the API implementation.")
public record JsendResponse<T>(
        @Schema(
                description = "JSend status value.",
                allowableValues = {"success", "fail", "error"})
        String status,

        @Schema(description = "Payload returned for the response status.")
        T data,

        @Schema(description = "Human-readable technical error message for error responses.")
        String message) {

    /** 2xx – successful operation with data payload. */
    public static <T> JsendResponse<T> success(T data) {
        return new JsendResponse<>("success", data, null);
    }

    /** 2xx – successful operation with no data (e.g. DELETE). */
    public static JsendResponse<Void> success() {
        return new JsendResponse<>("success", null, null);
    }

    /** 4xx – business/validation failure; {@code data} holds field-level or message detail. */
    public static <T> JsendResponse<T> fail(T data) {
        return new JsendResponse<>("fail", data, null);
    }

    /** 5xx – unexpected technical error; only a human-readable message is exposed. */
    public static JsendResponse<Void> error(String message) {
        return new JsendResponse<>("error", null, message);
    }
}

package io.github.phunguy65.ttbs.backend.shared.infrastructure.web;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Codes representing the category of a Bean Validation constraint failure.
 *
 * <p>Used inside {@link Violation} records to give the frontend a machine-readable signal about
 * <em>why</em> a field value was rejected, enabling frontend-side i18n without parsing message
 * strings.
 */
@Schema(description = "Machine-readable validation category attached to a field violation.")
public enum ViolationCode {

    /** Field was absent, blank, or null when a value is required. */
    REQUIRED,

    /** Value does not match the expected format (e.g. email, regex pattern). */
    INVALID_FORMAT,

    /** String is shorter than the allowed minimum length. */
    TOO_SHORT,

    /** String is longer than the allowed maximum length. */
    TOO_LONG,

    /** Numeric or other value falls outside the permitted range or set. */
    INVALID_VALUE
}

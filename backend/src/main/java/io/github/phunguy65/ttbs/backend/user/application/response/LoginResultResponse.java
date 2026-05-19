package io.github.phunguy65.ttbs.backend.user.application.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        description =
                "Authentication result containing rotated tokens and the authenticated customer profile.")
public record LoginResultResponse(
        @Schema(
                description = "Short-lived bearer access token.",
                accessMode = Schema.AccessMode.READ_ONLY,
                example = "redacted-access-token")
        String accessToken,

        @Schema(
                description = "Refresh token used to rotate the session.",
                accessMode = Schema.AccessMode.READ_ONLY,
                example = "redacted-refresh-token")
        String refreshToken,

        @Schema(description = "Authenticated customer profile.")
        UserResponse user) {}

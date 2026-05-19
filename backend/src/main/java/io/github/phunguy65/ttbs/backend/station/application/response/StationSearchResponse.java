package io.github.phunguy65.ttbs.backend.station.application.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Station suggestion returned by station search.")
public record StationSearchResponse(
        @Schema(description = "Station identifier.", format = "uuid")
        UUID id,

        @Schema(description = "Station code.", example = "HNO")
        String code,

        @Schema(description = "Station name.", example = "Ha Noi")
        String name,

        @Schema(description = "Station city.", example = "Ha Noi")
        String city) {}

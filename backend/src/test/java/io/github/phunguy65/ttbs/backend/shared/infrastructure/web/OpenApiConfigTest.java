package io.github.phunguy65.ttbs.backend.shared.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import org.junit.jupiter.api.Test;
import org.springdoc.core.models.GroupedOpenApi;

class OpenApiConfigTest {

    private final OpenApiConfig config = new OpenApiConfig();

    @Test
    void customerOpenApiRegistersMetadataSecurityAndSharedSchemas() {
        var openApi = config.customerOpenApi();

        assertThat(openApi.getInfo().getTitle()).isEqualTo("TTBS Customer API");
        assertThat(openApi.getInfo().getVersion()).isEqualTo("1.0");
        assertThat(openApi.getServers()).singleElement().extracting("url").isEqualTo("/");
        assertThat(openApi.getSecurity()).singleElement().satisfies(requirement -> assertThat(
                        requirement.containsKey("bearerAuth"))
                .isTrue());
        assertThat(openApi.getComponents().getSecuritySchemes()).containsKey("bearerAuth");
        assertThat(openApi.getComponents().getSchemas())
                .containsKeys(
                        "FailData",
                        "Violation",
                        "ErrorCode",
                        "ViolationCode",
                        "PageResponse",
                        "SliceResponse",
                        "JsendSuccessResponse",
                        "JsendFailResponse",
                        "JsendErrorResponse");
        assertThat(openApi.getTags())
                .extracting(tag -> tag.getName())
                .containsExactly("Authentication", "Bookings", "Payments", "Stations", "Trains");
    }

    @Test
    void customerGroupedApiTargetsPublicEndpointsAndInstallsCustomizers() {
        JsendSuccessResponseCustomizer customizer = config.jsendSuccessResponseCustomizer();

        GroupedOpenApi groupedOpenApi = config.customerApi(customizer);

        assertThat(groupedOpenApi.getGroup()).isEqualTo("customer");
        assertThat(groupedOpenApi.getPathsToMatch()).containsExactly("/api/**");
        assertThat(groupedOpenApi.getPathsToExclude())
                .containsExactly("/api/*/sse/**", "/api/**/webhooks/**");
        assertThat(groupedOpenApi.getOperationCustomizers()).contains(customizer);
        assertThat(groupedOpenApi.getOpenApiCustomizers()).contains(customizer);
        assertThat(groupedOpenApi.getOpenApiCustomizers()).hasSize(3);
    }

    @Test
    void customerGroupedApiRewritesVersionPathPlaceholder() {
        GroupedOpenApi groupedOpenApi = config.customerApi(config.jsendSuccessResponseCustomizer());
        OpenAPI openApi = new OpenAPI()
                .paths(new Paths()
                        .addPathItem("/api/{version}/stations/search", new PathItem())
                        .addPathItem("/api/health", new PathItem()));

        groupedOpenApi.getOpenApiCustomizers().forEach(customizer -> customizer.customise(openApi));

        assertThat(openApi.getPaths().keySet())
                .containsExactlyInAnyOrder("/api/v1/stations/search", "/api/health");
    }
}

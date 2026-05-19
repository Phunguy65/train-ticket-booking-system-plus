package io.github.phunguy65.ttbs.backend.shared.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CustomerOpenApiExporterTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @TempDir
    Path tempDir;

    @Test
    void writeArtifactsMirrorsYamlAndDebugJson() throws Exception {
        Path workspaceRoot = Files.createDirectories(tempDir.resolve("workspace"));
        Files.createDirectories(workspaceRoot.resolve("shared/api-contracts"));
        Path buildDir = Files.createDirectories(tempDir.resolve("build"));
        var root = OBJECT_MAPPER.readTree("""
                {
                  "openapi": "3.1.0",
                  "info": {"title": "TTBS Customer API", "version": "1.0"},
                  "paths": {
                    "/api/{version}/auth/login": {
                      "post": {"operationId": "login"}
                    }
                  }
                }
                """);

        CustomerOpenApiExporter.writeArtifacts(root, workspaceRoot, buildDir);

        Path debugJson = buildDir.resolve("reports/openapi/customer-openapi.json");
        Path mirroredYaml = workspaceRoot.resolve("shared/api-contracts/openapi.yaml");

        assertThat(debugJson).exists();
        assertThat(Files.readString(debugJson)).contains("\"openapi\" : \"3.1.0\"");

        assertThat(mirroredYaml).exists();
        assertThat(Files.readString(mirroredYaml))
                .contains("openapi: \"3.1.0\"")
                .contains("operationId: \"login\"");
    }
}

package io.github.phunguy65.ttbs.backend.shared.infrastructure.web;

import io.github.phunguy65.ttbs.backend.BackendApplication;
import io.github.phunguy65.ttbs.backend.TestContainerConfiguration;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public final class CustomerOpenApiExporter {

    private CustomerOpenApiExporter() {}

    public static void main(String[] args) throws Exception {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
                        BackendApplication.class, TestContainerConfiguration.class)
                .profiles("test")
                .properties("server.port=0")
                .run(args)) {
            int port = Integer.parseInt(
                    context.getEnvironment().getRequiredProperty("local.server.port"));

            var root = CustomerOpenApiContractSupport.fetchJson(port, "/v3/api-docs/customer");
            CustomerOpenApiContractSupport.assertRepresentativeContract(root);
            writeArtifacts(root, CustomerOpenApiContractSupport.workspaceRoot(), Path.of("build"));
        }
    }

    static void writeArtifacts(
            com.fasterxml.jackson.databind.JsonNode root, Path workspaceRoot, Path buildDir)
            throws Exception {
        Path debugJsonPath = buildDir.resolve("reports/openapi/customer-openapi.json");
        Files.createDirectories(debugJsonPath.getParent());
        Files.writeString(debugJsonPath, root.toPrettyString());

        Path outputPath = workspaceRoot.resolve("shared/api-contracts/openapi.yaml");
        Files.createDirectories(outputPath.getParent());
        Files.writeString(outputPath, CustomerOpenApiContractSupport.toYaml(root));
    }
}

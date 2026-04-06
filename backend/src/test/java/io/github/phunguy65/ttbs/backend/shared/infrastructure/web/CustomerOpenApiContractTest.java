package io.github.phunguy65.ttbs.backend.shared.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.github.phunguy65.ttbs.backend.TestContainerConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestContainerConfiguration.class)
class CustomerOpenApiContractTest {

    @Value("${local.server.port}")
    private int port;

    @Test
    void generatedCustomerContractMatchesRepresentativeEndpoints() throws Exception {
        var root = CustomerOpenApiContractSupport.fetchJson(port, "/v3/api-docs/customer");

        CustomerOpenApiContractSupport.assertRepresentativeContract(root);
    }

    @Test
    void generatedCustomerContractYamlEndpointIsAvailable() throws Exception {
        var root = CustomerOpenApiContractSupport.fetchJson(port, "/v3/api-docs/customer");
        String yaml = CustomerOpenApiContractSupport.toYaml(root);
        var parsedYaml = new YAMLMapper().readTree(yaml);

        assertThat(yaml).contains("openapi:");
        assertThat(yaml).contains("operationId: \"login\"");
        assertThat(yaml).doesNotContain("/webhooks/");
        assertThat(yaml).doesNotContain("/sse/");
        assertThat(parsedYaml.path("openapi").asText()).isEqualTo("3.1.0");
        assertThat(parsedYaml.path("info").path("title").asText()).isEqualTo("TTBS Customer API");
        assertThat(parsedYaml.path("paths").isObject()).isTrue();
        assertThat(parsedYaml.path("components").path("schemas").isObject()).isTrue();
    }
}

package io.github.phunguy65.ttbs.backend.shared.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

final class CustomerOpenApiContractSupport {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final YAMLMapper YAML_MAPPER = new YAMLMapper();

    private CustomerOpenApiContractSupport() {}

    static JsonNode fetchJson(int port, String path) throws IOException, InterruptedException {
        String responseBody = fetch(port, path);
        return OBJECT_MAPPER.readTree(responseBody);
    }

    static String fetchYaml(int port, String path) throws IOException, InterruptedException {
        return fetch(port, path);
    }

    static String toYaml(JsonNode root) throws IOException {
        return YAML_MAPPER.writeValueAsString(root);
    }

    static void assertRepresentativeContract(JsonNode root) {
        require(
                "TTBS Customer API".equals(root.path("info").path("title").asText()),
                "OpenAPI title mismatch");
        require(hasSchemaFamily(root, "PageResponse"), "Missing PageResponse schema family");
        require(hasSchemaFamily(root, "SliceResponse"), "Missing SliceResponse schema family");
        require(
                root.path("components").path("schemas").has("JsendFailResponse"),
                "Missing JsendFailResponse schema");
        require(
                root.path("components").path("schemas").has("JsendErrorResponse"),
                "Missing JsendErrorResponse schema");

        JsonNode login = operationById(root, "login");
        require(isUnauthenticated(login), "login should be unauthenticated");
        require(hasFailResponse(login, "400"), "login should reference JsendFailResponse for 400");

        JsonNode getAuthenticatedUser = operationById(root, "getAuthenticatedUser");
        require(
                hasBearerAuth(getAuthenticatedUser),
                "getAuthenticatedUser should require bearerAuth");

        OperationEntry updateAuthenticatedUser =
                operationEntryById(root, "updateAuthenticatedUser");
        require(
                "put".equals(updateAuthenticatedUser.method()),
                "updateAuthenticatedUser should use PUT rather than PATCH");
        require(
                hasBearerAuth(updateAuthenticatedUser.operation()),
                "updateAuthenticatedUser should require bearerAuth");
        require(
                hasFailResponse(updateAuthenticatedUser.operation(), "400"),
                "updateAuthenticatedUser should reference JsendFailResponse for 400");
        require(
                hasFailResponse(updateAuthenticatedUser.operation(), "409"),
                "updateAuthenticatedUser should reference JsendFailResponse for 409");

        JsonNode getUserBookings = operationById(root, "getUserBookings");
        require(hasBearerAuth(getUserBookings), "getUserBookings should require bearerAuth");
        require(
                successRefContains(getUserBookings, "200", "PageResponse"),
                "getUserBookings should expose a PageResponse success schema");

        JsonNode filterScheduledTrips = operationById(root, "filterScheduledTrips");
        require(
                successRefContains(filterScheduledTrips, "200", "SliceResponse"),
                "filterScheduledTrips should expose a SliceResponse success schema");

        JsonNode searchStations = operationById(root, "searchStations");
        JsonNode searchStationsSchema = responseSchema(searchStations, "200");
        require(
                "array".equals(searchStationsSchema.path("type").asText()),
                "searchStations should expose an array success schema");

        Iterator<String> pathNames = root.path("paths").fieldNames();
        while (pathNames.hasNext()) {
            String pathName = pathNames.next();
            require(
                    !pathName.contains("/sse/"),
                    "SSE endpoint leaked into customer contract: " + pathName);
            require(
                    !pathName.contains("/webhooks/"),
                    "Webhook endpoint leaked into customer contract: " + pathName);
        }
    }

    static Path workspaceRoot() throws IOException {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("shared/api-contracts"))
                    && Files.isDirectory(current.resolve("backend"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IOException("Unable to locate workspace root");
    }

    private static String fetch(int port, String path) throws IOException, InterruptedException {
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .GET()
                .build();
        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        require(
                response.statusCode() == 200,
                "Expected 200 from " + path + " but got " + response.statusCode());
        return response.body();
    }

    private static JsonNode operationById(JsonNode root, String operationId) {
        return operationEntryById(root, operationId).operation();
    }

    private static OperationEntry operationEntryById(JsonNode root, String operationId) {
        JsonNode paths = root.path("paths");
        Iterator<JsonNode> pathItems = paths.elements();
        while (pathItems.hasNext()) {
            JsonNode pathItem = pathItems.next();
            Iterator<String> methods = pathItem.fieldNames();
            while (methods.hasNext()) {
                String method = methods.next();
                JsonNode operation = pathItem.path(method);
                if (operationId.equals(operation.path("operationId").asText())) {
                    return new OperationEntry(method, operation);
                }
            }
        }
        throw new IllegalStateException("Operation not found: " + operationId);
    }

    private record OperationEntry(String method, JsonNode operation) {}

    private static boolean isUnauthenticated(JsonNode operation) {
        JsonNode security = operation.path("security");
        if (security.isMissingNode()) {
            return true;
        }
        if (security.isArray() && security.isEmpty()) {
            return true;
        }
        return security.isArray()
                && security.size() == 1
                && security.get(0).isObject()
                && !security.get(0).fieldNames().hasNext();
    }

    private static boolean hasBearerAuth(JsonNode operation) {
        JsonNode security = operation.path("security");
        if (!security.isArray()) {
            return false;
        }
        for (JsonNode requirement : security) {
            if (requirement.has("bearerAuth")) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasFailResponse(JsonNode operation, String responseCode) {
        return schemaContainsRef(
                responseSchema(operation, responseCode), componentSchemaRef("JsendFailResponse"));
    }

    private static boolean successRefContains(
            JsonNode operation, String responseCode, String expected) {
        return schemaRefStartsWith(
                responseSchema(operation, responseCode), componentSchemaRef(expected));
    }

    private static JsonNode responseSchema(JsonNode operation, String responseCode) {
        JsonNode content = operation.path("responses").path(responseCode).path("content");
        JsonNode jsonSchema = content.path("application/json").path("schema");
        if (!jsonSchema.isMissingNode()) {
            return jsonSchema;
        }

        JsonNode wildcardSchema = content.path("*/*").path("schema");
        if (!wildcardSchema.isMissingNode()) {
            return wildcardSchema;
        }

        Iterator<JsonNode> mediaTypes = content.elements();
        while (mediaTypes.hasNext()) {
            JsonNode schema = mediaTypes.next().path("schema");
            if (!schema.isMissingNode()) {
                return schema;
            }
        }

        return content.path("schema");
    }

    private static boolean hasSchemaFamily(JsonNode root, String prefix) {
        Iterator<String> schemaNames = root.path("components").path("schemas").fieldNames();
        while (schemaNames.hasNext()) {
            if (schemaNames.next().startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean schemaContainsRef(JsonNode schema, String expected) {
        if (expected.equals(schema.path("$ref").asText())) {
            return true;
        }

        Iterator<JsonNode> elements = schema.elements();
        while (elements.hasNext()) {
            if (schemaContainsRef(elements.next(), expected)) {
                return true;
            }
        }

        return false;
    }

    private static boolean schemaRefStartsWith(JsonNode schema, String expectedPrefix) {
        String ref = schema.path("$ref").asText();
        if (!ref.isBlank() && ref.startsWith(expectedPrefix)) {
            return true;
        }

        Iterator<JsonNode> elements = schema.elements();
        while (elements.hasNext()) {
            if (schemaRefStartsWith(elements.next(), expectedPrefix)) {
                return true;
            }
        }

        return false;
    }

    private static String componentSchemaRef(String schemaName) {
        return "#/components/schemas/" + schemaName;
    }

    private static void require(boolean condition, String message) {
        assertThat(condition).withFailMessage(message).isTrue();
    }
}

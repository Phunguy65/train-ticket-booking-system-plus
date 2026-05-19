package io.github.phunguy65.ttbs.backend.shared.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;

class JsendSuccessResponseCustomizerTest {

    private final JsendSuccessResponseCustomizer customizer = new JsendSuccessResponseCustomizer();

    @Test
    void customizesObjectPayloadSchemas() throws NoSuchMethodException {
        Operation operation = operationWithJsonResponse("200");

        customizer.customize(operation, handlerMethod("objectResponse"));

        assertThat(schemaRef(operation, "200")).isEqualTo("#/components/schemas/TestPayload");
        assertTechnicalFailureResponse(operation);
    }

    @Test
    void customizesArrayPayloadSchemasWithoutDoubleWrapping() throws NoSuchMethodException {
        Operation operation = operationWithJsonResponse("200");

        customizer.customize(operation, handlerMethod("arrayResponse"));

        assertThat(jsonSchema(operation, "200")).isInstanceOf(ArraySchema.class);
        ArraySchema schema = (ArraySchema) jsonSchema(operation, "200");
        assertThat(schema.getItems().get$ref()).isEqualTo("#/components/schemas/TestPayload");
        assertTechnicalFailureResponse(operation);
    }

    @Test
    void customizesPagePayloadSchemas() throws NoSuchMethodException {
        Operation operation = operationWithJsonResponse("200");

        customizer.customize(operation, handlerMethod("pageResponse"));

        assertThat(schemaRef(operation, "200")).contains("PageResponse");
        assertTechnicalFailureResponse(operation);
    }

    @Test
    void customizesSlicePayloadSchemas() throws NoSuchMethodException {
        Operation operation = operationWithJsonResponse("200");

        customizer.customize(operation, handlerMethod("sliceResponse"));

        assertThat(schemaRef(operation, "200")).contains("SliceResponse");
        assertTechnicalFailureResponse(operation);
    }

    @Test
    void clearsContentForVoidPayloads() throws NoSuchMethodException {
        Operation operation = operationWithJsonResponse("200");

        customizer.customize(operation, handlerMethod("voidResponse"));

        assertThat(operation.getResponses().get("200").getContent()).isNull();
        assertTechnicalFailureResponse(operation);
    }

    @Test
    void customizesOnlyMatchingResponseCodeWhenConfigured() throws NoSuchMethodException {
        Operation operation = new Operation()
                .responses(new ApiResponses()
                        .addApiResponse(
                                "200",
                                new ApiResponse()
                                        .content(new Content()
                                                .addMediaType("application/json", new MediaType())))
                        .addApiResponse(
                                "201",
                                new ApiResponse()
                                        .content(new Content()
                                                .addMediaType(
                                                        "application/json", new MediaType()))));

        customizer.customize(operation, handlerMethod("createdResponse"));

        assertThat(operation
                        .getResponses()
                        .get("200")
                        .getContent()
                        .get("application/json")
                        .getSchema())
                .isNull();
        assertThat(schemaRef(operation, "201")).isEqualTo("#/components/schemas/TestPayload");
        assertTechnicalFailureResponse(operation);
    }

    @Test
    void addsTechnicalFailureResponseToCustomizedOperations() throws NoSuchMethodException {
        Operation operation = operationWithJsonResponse("200");

        customizer.customize(operation, handlerMethod("objectResponse"));

        assertTechnicalFailureResponse(operation);
    }

    @Test
    void leavesNonAnnotatedSuccessResponsesUntouchedWhileAddingTechnicalFailureResponse()
            throws NoSuchMethodException {
        Operation operation = operationWithJsonResponse("200");

        customizer.customize(operation, handlerMethod("noAnnotation"));

        assertThat(jsonSchema(operation, "200")).isNull();
        assertTechnicalFailureResponse(operation);
    }

    @Test
    void ignoresOperationsWithoutResponses() throws NoSuchMethodException {
        Operation operation = new Operation();

        customizer.customize(operation, handlerMethod("objectResponse"));

        assertThat(operation.getResponses()).isNull();
    }

    @Test
    void preservesExistingTechnicalFailureResponse() throws NoSuchMethodException {
        ApiResponse existingFailure = new ApiResponse().description("Already documented");
        Operation operation = new Operation()
                .responses(new ApiResponses()
                        .addApiResponse(
                                "200",
                                new ApiResponse()
                                        .content(new Content()
                                                .addMediaType("application/json", new MediaType())))
                        .addApiResponse("500", existingFailure));

        customizer.customize(operation, handlerMethod("objectResponse"));

        assertThat(operation.getResponses().get("500")).isSameAs(existingFailure);
    }

    @Test
    void registersReferencedSchemasDuringOpenApiCustomization() throws NoSuchMethodException {
        Operation operation = operationWithJsonResponse("200");
        customizer.customize(operation, handlerMethod("pageResponse"));
        OpenAPI openApi = new OpenAPI();

        customizer.customise(openApi);

        assertThat(openApi.getComponents()).isNotNull();
        assertThat(openApi.getComponents().getSchemas()).containsKey("PageResponseTestPayload");
        assertThat(openApi.getComponents().getSchemas()).containsKey("TestPayload");

        OpenAPI secondOpenApi = new OpenAPI();
        customizer.customise(secondOpenApi);

        assertThat(secondOpenApi.getComponents()).isNull();
    }

    @Test
    void successPayloadAnnotationUsesExpectedDefaults() throws NoSuchMethodException {
        SuccessPayload annotation = TestController.class
                .getDeclaredMethod("defaultResponse")
                .getAnnotation(SuccessPayload.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo(Void.class);
        assertThat(annotation.kind()).isEqualTo(SuccessResponseKind.OBJECT);
        assertThat(annotation.responseCode()).isEmpty();
    }

    private HandlerMethod handlerMethod(String methodName) throws NoSuchMethodException {
        Method method = TestController.class.getDeclaredMethod(methodName);
        return new HandlerMethod(new TestController(), method);
    }

    private Operation operationWithJsonResponse(String responseCode) {
        return new Operation()
                .responses(new ApiResponses()
                        .addApiResponse(
                                responseCode,
                                new ApiResponse()
                                        .content(new Content()
                                                .addMediaType(
                                                        "application/json", new MediaType()))));
    }

    private io.swagger.v3.oas.models.media.Schema<?> jsonSchema(
            Operation operation, String responseCode) {
        return operation
                .getResponses()
                .get(responseCode)
                .getContent()
                .get("application/json")
                .getSchema();
    }

    private String schemaRef(Operation operation, String responseCode) {
        return jsonSchema(operation, responseCode).get$ref();
    }

    private void assertTechnicalFailureResponse(Operation operation) {
        assertThat(operation.getResponses()).containsKey("500");
        assertThat(operation.getResponses().get("500").getDescription())
                .isEqualTo("Unexpected technical failure.");
        assertThat(operation
                        .getResponses()
                        .get("500")
                        .getContent()
                        .get("application/json")
                        .getSchema())
                .extracting(Schema::get$ref)
                .isEqualTo("#/components/schemas/JsendErrorResponse");
    }

    static final class TestPayload {
        public String value;
    }

    static final class TestController {
        @SuccessPayload(TestPayload.class)
        void objectResponse() {}

        @SuccessPayload(value = TestPayload.class, kind = SuccessResponseKind.ARRAY)
        void arrayResponse() {}

        @SuccessPayload(value = TestPayload.class, kind = SuccessResponseKind.PAGE)
        void pageResponse() {}

        @SuccessPayload(value = TestPayload.class, kind = SuccessResponseKind.SLICE)
        void sliceResponse() {}

        @SuccessPayload
        void voidResponse() {}

        @SuccessPayload(value = TestPayload.class, responseCode = "201")
        void createdResponse() {}

        @SuccessPayload
        void defaultResponse() {}

        void noAnnotation() {}
    }
}

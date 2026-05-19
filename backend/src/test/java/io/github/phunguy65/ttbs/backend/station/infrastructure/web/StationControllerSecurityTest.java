package io.github.phunguy65.ttbs.backend.station.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.station.application.response.StationSearchResponse;
import io.github.phunguy65.ttbs.backend.station.application.usecase.GetStationByIdUseCase;
import io.github.phunguy65.ttbs.backend.station.application.usecase.GetStationsUseCase;
import io.github.phunguy65.ttbs.backend.station.application.usecase.SearchStationsUseCase;
import io.github.phunguy65.ttbs.backend.station.domain.error.StationError;
import io.github.phunguy65.ttbs.backend.station.infrastructure.web.request.GetStationByIdRequest;
import io.github.phunguy65.ttbs.backend.station.infrastructure.web.request.GetStationsRequest;
import io.github.phunguy65.ttbs.backend.station.infrastructure.web.request.SearchStationsRequest;
import jakarta.validation.Valid;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

@DisplayName("StationController security")
class StationControllerSecurityTest {

    private final GetStationByIdUseCase getStationByIdUseCase = mock(GetStationByIdUseCase.class);
    private final GetStationsUseCase getStationsUseCase = mock(GetStationsUseCase.class);
    private final SearchStationsUseCase searchStationsUseCase = mock(SearchStationsUseCase.class);
    private final StationController controller =
            new StationController(getStationByIdUseCase, getStationsUseCase, searchStationsUseCase);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Nested
    @DisplayName("pen-test search")
    class PenTestSearch {

        @Test
        @DisplayName("passes XSS payload through without sanitization")
        void search_passesXssPayloadThroughWithoutSanitization() {
            String xssPayload = "<script>alert('xss')</script>";
            SearchStationsRequest request = new SearchStationsRequest(xssPayload, 10);
            when(searchStationsUseCase.execute(request.toQuery()))
                    .thenReturn(List.of(new StationSearchResponse(
                            UUID.fromString("11111111-1111-1111-1111-111111111111"),
                            "TESTXSS",
                            xssPayload,
                            "Ha Noi")));

            JsonNode json = objectMapper.valueToTree(controller.search(request).getBody());

            assertThat(json.get("data").get(0).get("name").asText()).isEqualTo(xssPayload);
        }

        @Test
        @DisplayName("handles SQL injection payload safely")
        void search_handlesSqlInjectionPayloadSafely() {
            SearchStationsRequest request =
                    new SearchStationsRequest("'; DROP TABLE stations; --", 10);
            when(searchStationsUseCase.execute(request.toQuery())).thenReturn(List.of());

            var result = controller.search(request);

            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().status()).isEqualTo("success");
            assertThat((List<?>) result.getBody().data()).isEmpty();
        }
    }

    @Nested
    @DisplayName("pen-test get by id")
    class PenTestGetById {

        @Test
        @DisplayName("throws exception for malformed UUID")
        void getById_throwsExceptionForMalformedUuid() {
            UUID stationId = UUID.fromString("22222222-2222-2222-2222-222222222222");
            when(getStationByIdUseCase.execute(
                            new io.github.phunguy65.ttbs.backend.station.application.query
                                    .GetStationByIdQuery(stationId)))
                    .thenReturn(Result.failure(new StationError.StationNotFound()));

            var result = controller.getById(stationId, new GetStationByIdRequest());

            assertThat(result.getStatusCode().value()).isEqualTo(404);
        }

        @Test
        @DisplayName("handles null id gracefully")
        void getById_handlesNullIdGracefully() {
            assertThatThrownBy(() -> controller.getById(null, new GetStationByIdRequest()))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("annotation check")
    class AnnotationCheck {

        @Test
        @DisplayName("station controller is not annotated with PreAuthorize")
        void stationController_isNotAnnotatedWithPreAuthorize() {
            assertThat(StationController.class.isAnnotationPresent(PreAuthorize.class))
                    .isFalse();
        }

        @Test
        @DisplayName("list does not require authentication")
        void list_doesNotRequireAuthentication() throws Exception {
            assertThat(listMethod().isAnnotationPresent(PreAuthorize.class)).isFalse();
        }

        @Test
        @DisplayName("search does not require authentication")
        void search_doesNotRequireAuthentication() throws Exception {
            assertThat(searchMethod().isAnnotationPresent(PreAuthorize.class)).isFalse();
        }

        @Test
        @DisplayName("get by id does not require authentication")
        void getById_doesNotRequireAuthentication() throws Exception {
            assertThat(getByIdMethod().isAnnotationPresent(PreAuthorize.class)).isFalse();
        }

        @Test
        @DisplayName("list declares Valid on request")
        void list_declaresValidOnRequest() throws Exception {
            assertThat(parameterAnnotationNames(listMethod(), 0)).contains(Valid.class.getName());
        }

        @Test
        @DisplayName("search declares Valid on request")
        void search_declaresValidOnRequest() throws Exception {
            assertThat(parameterAnnotationNames(searchMethod(), 0)).contains(Valid.class.getName());
        }

        private Method listMethod() throws NoSuchMethodException {
            return StationController.class.getDeclaredMethod("list", GetStationsRequest.class);
        }

        private Method searchMethod() throws NoSuchMethodException {
            return StationController.class.getDeclaredMethod("search", SearchStationsRequest.class);
        }

        private Method getByIdMethod() throws NoSuchMethodException {
            return StationController.class.getDeclaredMethod(
                    "getById", UUID.class, GetStationByIdRequest.class);
        }

        private List<String> parameterAnnotationNames(Method method, int index) {
            return Arrays.stream(method.getParameterAnnotations()[index])
                    .map(annotation -> annotation.annotationType().getName())
                    .toList();
        }
    }
}

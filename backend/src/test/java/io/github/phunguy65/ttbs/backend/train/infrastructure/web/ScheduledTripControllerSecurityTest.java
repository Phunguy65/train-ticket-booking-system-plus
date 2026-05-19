package io.github.phunguy65.ttbs.backend.train.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.shared.domain.SliceResponse;
import io.github.phunguy65.ttbs.backend.train.application.response.SearchScheduledTripsResponse;
import io.github.phunguy65.ttbs.backend.train.application.usecase.GetScheduledTripByIdUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.GetScheduledTripsUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.SearchScheduledTripsUseCase;
import io.github.phunguy65.ttbs.backend.train.domain.error.ScheduledTripError;
import io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripStatus;
import io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.GetScheduledTripByIdRequest;
import io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.GetScheduledTripsRequest;
import io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.SearchScheduledTripsRequest;
import jakarta.validation.Valid;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

@DisplayName("ScheduledTripController security")
class ScheduledTripControllerSecurityTest {

    private final GetScheduledTripByIdUseCase getScheduledTripByIdUseCase =
            mock(GetScheduledTripByIdUseCase.class);
    private final GetScheduledTripsUseCase getScheduledTripsUseCase =
            mock(GetScheduledTripsUseCase.class);
    private final SearchScheduledTripsUseCase searchScheduledTripsUseCase =
            mock(SearchScheduledTripsUseCase.class);
    private final ScheduledTripController controller = new ScheduledTripController(
            getScheduledTripByIdUseCase, getScheduledTripsUseCase, searchScheduledTripsUseCase);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Nested
    @DisplayName("pen-test filter")
    class PenTestFilter {

        @Test
        @DisplayName("passes XSS payload through without sanitization")
        void filter_passesXssPayloadThroughWithoutSanitization() {
            String xssPayload = "<script>alert('xss')</script>";
            SearchScheduledTripsRequest request = new SearchScheduledTripsRequest(
                    null, null, null, null, null, null, null, null, null, xssPayload, 10);
            when(searchScheduledTripsUseCase.execute(request.toQuery()))
                    .thenReturn(
                            SliceResponse.of(List.of(searchResponse(xssPayload)), 10, false, null));

            JsonNode json = objectMapper.valueToTree(controller.filter(request).getBody());

            assertThat(json.get("data")
                            .get("content")
                            .get(0)
                            .get("train")
                            .get("name")
                            .asText())
                    .isEqualTo(xssPayload);
        }

        @Test
        @DisplayName("handles SQL injection payload in cursor safely")
        void filter_handlesSqlInjectionPayloadInCursorSafely() {
            SearchScheduledTripsRequest request = new SearchScheduledTripsRequest(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "'; DROP TABLE scheduled_trips; --",
                    10);
            when(searchScheduledTripsUseCase.execute(request.toQuery()))
                    .thenReturn(SliceResponse.empty(10));

            var result = controller.filter(request);

            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().status()).isEqualTo("success");
            assertThat(((io.github.phunguy65.ttbs.backend.shared.domain.SliceResponse<?>)
                                    result.getBody().data())
                            .content())
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("pen-test getById")
    class PenTestGetById {

        @Test
        @DisplayName("returns 404 for non existent uuid")
        void getById_returns404ForNonExistentUuid() {
            UUID scheduledTripId = UUID.fromString("22222222-2222-2222-2222-222222222222");
            when(getScheduledTripByIdUseCase.execute(new io.github.phunguy65.ttbs.backend.train
                            .application.query.GetScheduledTripByIdQuery(scheduledTripId)))
                    .thenReturn(Result.failure(new ScheduledTripError.ScheduledTripNotFound()));

            var result = controller.getById(scheduledTripId, new GetScheduledTripByIdRequest());

            assertThat(result.getStatusCode().value()).isEqualTo(404);
        }

        @Test
        @DisplayName("handles null id gracefully")
        void getById_handlesNullIdGracefully() {
            assertThatThrownBy(() -> controller.getById(null, new GetScheduledTripByIdRequest()))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("annotation check")
    class AnnotationCheck {

        @Test
        @DisplayName("scheduled trip controller is not annotated with PreAuthorize")
        void scheduledTripController_isNotAnnotatedWithPreAuthorize() {
            assertThat(ScheduledTripController.class.isAnnotationPresent(PreAuthorize.class))
                    .isFalse();
        }

        @Test
        @DisplayName("list does not require authentication")
        void list_doesNotRequireAuthentication() throws Exception {
            assertThat(listMethod().isAnnotationPresent(PreAuthorize.class)).isFalse();
        }

        @Test
        @DisplayName("filter does not require authentication")
        void filter_doesNotRequireAuthentication() throws Exception {
            assertThat(filterMethod().isAnnotationPresent(PreAuthorize.class)).isFalse();
        }

        @Test
        @DisplayName("getById does not require authentication")
        void getById_doesNotRequireAuthentication() throws Exception {
            assertThat(getByIdMethod().isAnnotationPresent(PreAuthorize.class)).isFalse();
        }

        @Test
        @DisplayName("list declares Valid on request")
        void list_declaresValidOnRequest() throws Exception {
            assertThat(parameterAnnotationNames(listMethod(), 0)).contains(Valid.class.getName());
        }

        @Test
        @DisplayName("filter declares Valid on request")
        void filter_declaresValidOnRequest() throws Exception {
            assertThat(parameterAnnotationNames(filterMethod(), 0)).contains(Valid.class.getName());
        }

        private Method listMethod() throws NoSuchMethodException {
            return ScheduledTripController.class.getDeclaredMethod(
                    "list", GetScheduledTripsRequest.class);
        }

        private Method filterMethod() throws NoSuchMethodException {
            return ScheduledTripController.class.getDeclaredMethod(
                    "filter", SearchScheduledTripsRequest.class);
        }

        private Method getByIdMethod() throws NoSuchMethodException {
            return ScheduledTripController.class.getDeclaredMethod(
                    "getById", UUID.class, GetScheduledTripByIdRequest.class);
        }

        private List<String> parameterAnnotationNames(Method method, int index) {
            return Arrays.stream(method.getParameterAnnotations()[index])
                    .map(annotation -> annotation.annotationType().getName())
                    .toList();
        }
    }

    private SearchScheduledTripsResponse searchResponse(String trainName) {
        return new SearchScheduledTripsResponse(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                Instant.parse("2026-05-16T08:00:00Z"),
                Instant.parse("2026-05-16T12:00:00Z"),
                ScheduledTripStatus.SCHEDULED,
                240,
                80,
                33,
                new SearchScheduledTripsResponse.Train(
                        UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
                        "TEST-SE1",
                        trainName,
                        120),
                new SearchScheduledTripsResponse.Route(
                        UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                        450000,
                        "VND",
                        new SearchScheduledTripsResponse.Station(
                                UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"),
                                "TESTHN",
                                "Ha Noi",
                                "Ha Noi"),
                        new SearchScheduledTripsResponse.Station(
                                UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"),
                                "TESTDN",
                                "Da Nang",
                                "Da Nang")));
    }
}

package io.github.phunguy65.ttbs.backend.train.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.query.GetAvailableSeatsQuery;
import io.github.phunguy65.ttbs.backend.train.application.query.GetCoachSeatMapQuery;
import io.github.phunguy65.ttbs.backend.train.application.response.CoachSeatMapResponse;
import io.github.phunguy65.ttbs.backend.train.application.response.SeatResponse;
import io.github.phunguy65.ttbs.backend.train.application.usecase.GetAvailableSeatsForScheduledTripUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.GetCoachSeatMapByScheduledTripUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.GetSeatsByTrainUseCase;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteSeatAvailabilityStatus;
import io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.GetAvailableSeatsRequest;
import io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.GetCoachSeatMapRequest;
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

@DisplayName("SeatController security")
class SeatControllerSecurityTest {

    private final GetSeatsByTrainUseCase getSeatsByTrainUseCase =
            mock(GetSeatsByTrainUseCase.class);
    private final GetAvailableSeatsForScheduledTripUseCase
            getAvailableSeatsForScheduledTripUseCase =
                    mock(GetAvailableSeatsForScheduledTripUseCase.class);
    private final GetCoachSeatMapByScheduledTripUseCase getCoachSeatMapByScheduledTripUseCase =
            mock(GetCoachSeatMapByScheduledTripUseCase.class);
    private final SeatController controller = new SeatController(
            getSeatsByTrainUseCase,
            getAvailableSeatsForScheduledTripUseCase,
            getCoachSeatMapByScheduledTripUseCase);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Nested
    @DisplayName("pen-test")
    class PenTest {

        @Test
        @DisplayName("malformed UUID payload is rejected by UUID parser")
        void pathVariable_malformedUuidPayloadIsRejectedByUuidParser() {
            assertThatThrownBy(() -> UUID.fromString("<script>alert('xss')</script>"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("passes XSS payload in response data through without sanitization")
        void getAvailableSeats_passesXssPayloadInResponseDataThroughWithoutSanitization() {
            UUID scheduledTripId = UUID.fromString("11111111-1111-1111-1111-111111111111");
            String xssPayload = "<script>alert('xss')</script>";
            when(getAvailableSeatsForScheduledTripUseCase.execute(
                            new GetAvailableSeatsQuery(0, 20, scheduledTripId)))
                    .thenReturn(PageResponse.of(
                            List.of(new SeatResponse(
                                    UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                                    UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
                                    xssPayload,
                                    Instant.parse("2026-05-16T08:00:00Z"))),
                            0,
                            20,
                            false,
                            1));

            JsonNode json = objectMapper.valueToTree(controller
                    .getAvailableSeats(scheduledTripId, new GetAvailableSeatsRequest())
                    .getBody());

            assertThat(json.get("data").get("content").get(0).get("seatNumber").asText())
                    .isEqualTo(xssPayload);
        }

        @Test
        @DisplayName("handles SQL injection payload in seat number safely")
        void getCoachSeatMap_handlesSqlInjectionPayloadInSeatNumberSafely() {
            UUID scheduledTripId = UUID.fromString("22222222-2222-2222-2222-222222222222");
            String sqlInjectionPayload = "'; DROP TABLE seats; --";
            when(getCoachSeatMapByScheduledTripUseCase.execute(
                            new GetCoachSeatMapQuery(0, 20, scheduledTripId)))
                    .thenReturn(Result.success(PageResponse.of(
                            List.of(new CoachSeatMapResponse(
                                    UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                                    1,
                                    64,
                                    List.of(new CoachSeatMapResponse.Seat(
                                            UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
                                            sqlInjectionPayload,
                                            RouteSeatAvailabilityStatus.AVAILABLE)))),
                            0,
                            20,
                            false,
                            1)));

            var result = controller.getCoachSeatMap(scheduledTripId, new GetCoachSeatMapRequest());

            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().status()).isEqualTo("success");
            PageResponse<?> page = (PageResponse<?>) result.getBody().data();
            assertThat(page.content()).hasSize(1);
        }

        @Test
        @DisplayName("handles null scheduled trip id gracefully")
        void getCoachSeatMap_handlesNullScheduledTripIdGracefully() {
            assertThatThrownBy(() -> controller.getCoachSeatMap(null, new GetCoachSeatMapRequest()))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("annotation check")
    class AnnotationCheck {

        @Test
        @DisplayName("seat controller is not annotated with PreAuthorize")
        void seatController_isNotAnnotatedWithPreAuthorize() {
            assertThat(SeatController.class.isAnnotationPresent(PreAuthorize.class))
                    .isFalse();
        }

        @Test
        @DisplayName("getAvailableSeats does not require authentication")
        void getAvailableSeats_doesNotRequireAuthentication() throws Exception {
            assertThat(getAvailableSeatsMethod().isAnnotationPresent(PreAuthorize.class))
                    .isFalse();
        }

        @Test
        @DisplayName("getCoachSeatMap does not require authentication")
        void getCoachSeatMap_doesNotRequireAuthentication() throws Exception {
            assertThat(getCoachSeatMapMethod().isAnnotationPresent(PreAuthorize.class))
                    .isFalse();
        }

        @Test
        @DisplayName("getAvailableSeats declares Valid on request")
        void getAvailableSeats_declaresValidOnRequest() throws Exception {
            assertThat(parameterAnnotationNames(getAvailableSeatsMethod(), 1))
                    .contains(Valid.class.getName());
        }

        @Test
        @DisplayName("getCoachSeatMap declares Valid on request")
        void getCoachSeatMap_declaresValidOnRequest() throws Exception {
            assertThat(parameterAnnotationNames(getCoachSeatMapMethod(), 1))
                    .contains(Valid.class.getName());
        }

        private Method getAvailableSeatsMethod() throws NoSuchMethodException {
            return SeatController.class.getDeclaredMethod(
                    "getAvailableSeats", UUID.class, GetAvailableSeatsRequest.class);
        }

        private Method getCoachSeatMapMethod() throws NoSuchMethodException {
            return SeatController.class.getDeclaredMethod(
                    "getCoachSeatMap", UUID.class, GetCoachSeatMapRequest.class);
        }

        private List<String> parameterAnnotationNames(Method method, int index) {
            return Arrays.stream(method.getParameterAnnotations()[index])
                    .map(annotation -> annotation.annotationType().getName())
                    .toList();
        }
    }
}

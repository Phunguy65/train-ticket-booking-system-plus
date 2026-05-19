package io.github.phunguy65.ttbs.backend.train.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.ErrorCode;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.FailData;
import io.github.phunguy65.ttbs.backend.train.application.query.GetScheduledTripByIdQuery;
import io.github.phunguy65.ttbs.backend.train.application.query.GetScheduledTripsQuery;
import io.github.phunguy65.ttbs.backend.train.application.response.ScheduledTripDetailResponse;
import io.github.phunguy65.ttbs.backend.train.application.response.ScheduledTripResponse;
import io.github.phunguy65.ttbs.backend.train.application.usecase.GetScheduledTripByIdUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.GetScheduledTripsUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.SearchScheduledTripsUseCase;
import io.github.phunguy65.ttbs.backend.train.domain.error.ScheduledTripError;
import io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripStatus;
import io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.GetScheduledTripByIdRequest;
import io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.GetScheduledTripsRequest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

@DisplayName("ScheduledTripController list and get by id")
class ScheduledTripControllerListAndGetByIdTest {

    private final GetScheduledTripByIdUseCase getScheduledTripByIdUseCase =
            mock(GetScheduledTripByIdUseCase.class);
    private final GetScheduledTripsUseCase getScheduledTripsUseCase =
            mock(GetScheduledTripsUseCase.class);
    private final SearchScheduledTripsUseCase searchScheduledTripsUseCase =
            mock(SearchScheduledTripsUseCase.class);
    private final ScheduledTripController controller = new ScheduledTripController(
            getScheduledTripByIdUseCase, getScheduledTripsUseCase, searchScheduledTripsUseCase);
    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    @Nested
    @DisplayName("list endpoint")
    class ListEndpoint {

        @Test
        @DisplayName("returns 200 with paged scheduled trips")
        void list_returns200WithPagedScheduledTrips() {
            PageResponse<ScheduledTripResponse> page =
                    PageResponse.of(List.of(scheduledTripResponse()), 0, 20, false, 1);
            when(getScheduledTripsUseCase.execute(new GetScheduledTripsQuery(0, 20)))
                    .thenReturn(page);

            var result = controller.list(new GetScheduledTripsRequest());

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().status()).isEqualTo("success");
            assertThat(result.getBody().data()).isEqualTo(page);
            assertThat(((PageResponse<?>) result.getBody().data()).content()).hasSize(1);
        }

        @Test
        @DisplayName("returns 200 with empty page when no scheduled trips")
        void list_returns200WithEmptyPageWhenNoScheduledTrips() {
            PageResponse<ScheduledTripResponse> page = PageResponse.empty(20);
            when(getScheduledTripsUseCase.execute(new GetScheduledTripsQuery(0, 20)))
                    .thenReturn(page);

            var result = controller.list(new GetScheduledTripsRequest());

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(((PageResponse<?>) result.getBody().data()).content()).isEmpty();
        }

        @Test
        @DisplayName("uses default pagination when no params provided")
        void list_usesDefaultPaginationWhenNoParamsProvided() {
            when(getScheduledTripsUseCase.execute(new GetScheduledTripsQuery(0, 20)))
                    .thenReturn(PageResponse.empty(20));

            controller.list(new GetScheduledTripsRequest());

            verify(getScheduledTripsUseCase).execute(new GetScheduledTripsQuery(0, 20));
        }
    }

    @Nested
    @DisplayName("get by id endpoint")
    class GetByIdEndpoint {

        @Test
        @DisplayName("returns 200 with scheduled trip detail response when found")
        void getById_returns200WithScheduledTripDetailResponseWhenFound() {
            UUID scheduledTripId = UUID.fromString("11111111-1111-1111-1111-111111111111");
            ScheduledTripDetailResponse response = scheduledTripDetailResponse(scheduledTripId);
            when(getScheduledTripByIdUseCase.execute(
                            new GetScheduledTripByIdQuery(scheduledTripId)))
                    .thenReturn(Result.success(response));

            var result = controller.getById(scheduledTripId, new GetScheduledTripByIdRequest());

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().status()).isEqualTo("success");
            assertThat(result.getBody().data()).isEqualTo(response);
        }

        @Test
        @DisplayName("returns 404 with scheduled trip not found error")
        void getById_returns404WithScheduledTripNotFoundError() {
            UUID scheduledTripId = UUID.fromString("22222222-2222-2222-2222-222222222222");
            when(getScheduledTripByIdUseCase.execute(
                            new GetScheduledTripByIdQuery(scheduledTripId)))
                    .thenReturn(Result.failure(new ScheduledTripError.ScheduledTripNotFound()));

            var result = controller.getById(scheduledTripId, new GetScheduledTripByIdRequest());

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().status()).isEqualTo("fail");
            FailData data = (FailData) result.getBody().data();
            assertThat(data.code()).isEqualTo(ErrorCode.SCHEDULED_TRIP_NOT_FOUND);
            assertThat(data.message()).isEqualTo("Scheduled trip not found");
        }
    }

    @Nested
    @DisplayName("response contract")
    class ResponseContract {

        @Test
        @DisplayName("returns JSend success wrapper with page data")
        void list_returnsJsendSuccessWrapperWithPageData() {
            when(getScheduledTripsUseCase.execute(new GetScheduledTripsQuery(0, 20)))
                    .thenReturn(PageResponse.of(List.of(scheduledTripResponse()), 0, 20, true, 30));

            JsonNode json = objectMapper.valueToTree(
                    controller.list(new GetScheduledTripsRequest()).getBody());

            assertThat(json.get("status").asText()).isEqualTo("success");
            assertThat(json.get("data").has("content")).isTrue();
            assertThat(json.get("data").get("page").asInt()).isZero();
            assertThat(json.get("data").get("size").asInt()).isEqualTo(20);
            assertThat(json.get("data").get("hasNext").asBoolean()).isTrue();
            assertThat(json.get("data").get("hasPrevious").asBoolean()).isFalse();
            assertThat(json.get("data").get("total").asLong()).isEqualTo(30);
        }

        @Test
        @DisplayName("returns JSend fail wrapper for errors")
        void getById_returnsJsendFailWrapperForErrors() {
            UUID scheduledTripId = UUID.fromString("33333333-3333-3333-3333-333333333333");
            when(getScheduledTripByIdUseCase.execute(
                            new GetScheduledTripByIdQuery(scheduledTripId)))
                    .thenReturn(Result.failure(new ScheduledTripError.ScheduledTripNotFound()));

            JsonNode json = objectMapper.valueToTree(controller
                    .getById(scheduledTripId, new GetScheduledTripByIdRequest())
                    .getBody());

            assertThat(json.get("status").asText()).isEqualTo("fail");
            assertThat(json.get("data").get("message").asText())
                    .isEqualTo("Scheduled trip not found");
            assertThat(json.get("data").get("code").asText()).isEqualTo("SCHEDULED_TRIP_NOT_FOUND");
            assertThat(json.get("data").get("errors")).isEmpty();
        }
    }

    private ScheduledTripResponse scheduledTripResponse() {
        return new ScheduledTripResponse(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
                Instant.parse("2026-05-16T08:00:00Z"),
                Instant.parse("2026-05-16T12:00:00Z"),
                ScheduledTripStatus.SCHEDULED,
                Instant.parse("2026-05-15T08:00:00Z"));
    }

    private ScheduledTripDetailResponse scheduledTripDetailResponse(UUID scheduledTripId) {
        return new ScheduledTripDetailResponse(
                scheduledTripId,
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
                Instant.parse("2026-05-16T08:00:00Z"),
                Instant.parse("2026-05-16T12:00:00Z"),
                ScheduledTripStatus.SCHEDULED,
                Instant.parse("2026-05-15T08:00:00Z"),
                new ScheduledTripDetailResponse.Train(
                        UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
                        "TEST-SE1",
                        "Test Express",
                        120),
                new ScheduledTripDetailResponse.Route(
                        UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                        450000,
                        "VND",
                        new ScheduledTripDetailResponse.Station(
                                UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"),
                                "TESTHN",
                                "Ha Noi",
                                "Ha Noi"),
                        new ScheduledTripDetailResponse.Station(
                                UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"),
                                "TESTDN",
                                "Da Nang",
                                "Da Nang")));
    }
}

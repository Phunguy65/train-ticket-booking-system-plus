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
import io.github.phunguy65.ttbs.backend.train.application.query.GetAvailableSeatsQuery;
import io.github.phunguy65.ttbs.backend.train.application.query.GetCoachSeatMapQuery;
import io.github.phunguy65.ttbs.backend.train.application.response.CoachSeatMapResponse;
import io.github.phunguy65.ttbs.backend.train.application.response.SeatResponse;
import io.github.phunguy65.ttbs.backend.train.application.usecase.GetAvailableSeatsForScheduledTripUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.GetCoachSeatMapByScheduledTripUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.GetSeatsByTrainUseCase;
import io.github.phunguy65.ttbs.backend.train.domain.error.ScheduledTripError;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteSeatAvailabilityStatus;
import io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.GetAvailableSeatsRequest;
import io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.GetCoachSeatMapRequest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

@DisplayName("SeatController available seats and coach seat map")
class SeatControllerAvailableAndCoachSeatMapTest {

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
    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    @Nested
    @DisplayName("available seats endpoint")
    class AvailableSeatsEndpoint {

        @Test
        @DisplayName("returns 200 with paged available seats")
        void getAvailableSeats_returns200WithPagedAvailableSeats() {
            UUID scheduledTripId = UUID.fromString("11111111-1111-1111-1111-111111111111");
            PageResponse<SeatResponse> page =
                    PageResponse.of(List.of(seatResponse()), 0, 20, false, 1);
            when(getAvailableSeatsForScheduledTripUseCase.execute(
                            new GetAvailableSeatsQuery(0, 20, scheduledTripId)))
                    .thenReturn(page);

            var result =
                    controller.getAvailableSeats(scheduledTripId, new GetAvailableSeatsRequest());

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().status()).isEqualTo("success");
            assertThat(result.getBody().data()).isEqualTo(page);
            assertThat(((PageResponse<?>) result.getBody().data()).content()).hasSize(1);
        }

        @Test
        @DisplayName("returns 200 with empty page")
        void getAvailableSeats_returns200WithEmptyPage() {
            UUID scheduledTripId = UUID.fromString("22222222-2222-2222-2222-222222222222");
            when(getAvailableSeatsForScheduledTripUseCase.execute(
                            new GetAvailableSeatsQuery(0, 20, scheduledTripId)))
                    .thenReturn(PageResponse.empty(20));

            var result =
                    controller.getAvailableSeats(scheduledTripId, new GetAvailableSeatsRequest());

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(((PageResponse<?>) result.getBody().data()).content()).isEmpty();
        }

        @Test
        @DisplayName("uses default pagination when no params provided")
        void getAvailableSeats_usesDefaultPaginationWhenNoParamsProvided() {
            UUID scheduledTripId = UUID.fromString("33333333-3333-3333-3333-333333333333");
            when(getAvailableSeatsForScheduledTripUseCase.execute(
                            new GetAvailableSeatsQuery(0, 20, scheduledTripId)))
                    .thenReturn(PageResponse.empty(20));

            controller.getAvailableSeats(scheduledTripId, new GetAvailableSeatsRequest());

            verify(getAvailableSeatsForScheduledTripUseCase)
                    .execute(new GetAvailableSeatsQuery(0, 20, scheduledTripId));
        }
    }

    @Nested
    @DisplayName("coach seat map endpoint")
    class CoachSeatMapEndpoint {

        @Test
        @DisplayName("returns 200 with paged coach seat map")
        void getCoachSeatMap_returns200WithPagedCoachSeatMap() {
            UUID scheduledTripId = UUID.fromString("44444444-4444-4444-4444-444444444444");
            PageResponse<CoachSeatMapResponse> page =
                    PageResponse.of(List.of(coachSeatMapResponse()), 0, 20, false, 1);
            when(getCoachSeatMapByScheduledTripUseCase.execute(
                            new GetCoachSeatMapQuery(0, 20, scheduledTripId)))
                    .thenReturn(Result.success(page));

            var result = controller.getCoachSeatMap(scheduledTripId, new GetCoachSeatMapRequest());

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().status()).isEqualTo("success");
            assertThat(result.getBody().data()).isEqualTo(page);
        }

        @Test
        @DisplayName("returns 404 with scheduled trip not found")
        void getCoachSeatMap_returns404WithScheduledTripNotFound() {
            UUID scheduledTripId = UUID.fromString("55555555-5555-5555-5555-555555555555");
            when(getCoachSeatMapByScheduledTripUseCase.execute(
                            new GetCoachSeatMapQuery(0, 20, scheduledTripId)))
                    .thenReturn(Result.failure(new ScheduledTripError.ScheduledTripNotFound()));

            var result = controller.getCoachSeatMap(scheduledTripId, new GetCoachSeatMapRequest());

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().status()).isEqualTo("fail");
            FailData data = (FailData) result.getBody().data();
            assertThat(data.code()).isEqualTo(ErrorCode.SCHEDULED_TRIP_NOT_FOUND);
            assertThat(data.message()).isEqualTo("Scheduled trip not found");
        }

        @Test
        @DisplayName("returns 200 with empty page when trip exists and has no coaches")
        void getCoachSeatMap_returns200WithEmptyPageWhenTripExistsAndHasNoCoaches() {
            UUID scheduledTripId = UUID.fromString("66666666-6666-6666-6666-666666666666");
            when(getCoachSeatMapByScheduledTripUseCase.execute(
                            new GetCoachSeatMapQuery(0, 20, scheduledTripId)))
                    .thenReturn(Result.success(PageResponse.empty(20)));

            var result = controller.getCoachSeatMap(scheduledTripId, new GetCoachSeatMapRequest());

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(((PageResponse<?>) result.getBody().data()).content()).isEmpty();
        }
    }

    @Nested
    @DisplayName("response contract")
    class ResponseContract {

        @Test
        @DisplayName("returns JSend success wrapper with page data")
        void getAvailableSeats_returnsJsendSuccessWrapperWithPageData() {
            UUID scheduledTripId = UUID.fromString("77777777-7777-7777-7777-777777777777");
            when(getAvailableSeatsForScheduledTripUseCase.execute(
                            new GetAvailableSeatsQuery(0, 20, scheduledTripId)))
                    .thenReturn(PageResponse.of(List.of(seatResponse()), 0, 20, true, 30));

            JsonNode json = objectMapper.valueToTree(controller
                    .getAvailableSeats(scheduledTripId, new GetAvailableSeatsRequest())
                    .getBody());

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
        void getCoachSeatMap_returnsJsendFailWrapperForErrors() {
            UUID scheduledTripId = UUID.fromString("88888888-8888-8888-8888-888888888888");
            when(getCoachSeatMapByScheduledTripUseCase.execute(
                            new GetCoachSeatMapQuery(0, 20, scheduledTripId)))
                    .thenReturn(Result.failure(new ScheduledTripError.ScheduledTripNotFound()));

            JsonNode json = objectMapper.valueToTree(controller
                    .getCoachSeatMap(scheduledTripId, new GetCoachSeatMapRequest())
                    .getBody());

            assertThat(json.get("status").asText()).isEqualTo("fail");
            assertThat(json.get("data").get("message").asText())
                    .isEqualTo("Scheduled trip not found");
            assertThat(json.get("data").get("code").asText()).isEqualTo("SCHEDULED_TRIP_NOT_FOUND");
            assertThat(json.get("data").get("errors")).isEmpty();
        }
    }

    private SeatResponse seatResponse() {
        return new SeatResponse(
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
                "1A",
                Instant.parse("2026-05-16T08:00:00Z"));
    }

    private CoachSeatMapResponse coachSeatMapResponse() {
        return new CoachSeatMapResponse(
                UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"),
                1,
                64,
                List.of(new CoachSeatMapResponse.Seat(
                        UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"),
                        "1A",
                        RouteSeatAvailabilityStatus.AVAILABLE)));
    }
}

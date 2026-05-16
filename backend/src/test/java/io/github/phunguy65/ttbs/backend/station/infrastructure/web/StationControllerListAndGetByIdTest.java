package io.github.phunguy65.ttbs.backend.station.infrastructure.web;

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
import io.github.phunguy65.ttbs.backend.station.application.query.GetStationsQuery;
import io.github.phunguy65.ttbs.backend.station.application.response.StationResponse;
import io.github.phunguy65.ttbs.backend.station.application.usecase.GetStationByIdUseCase;
import io.github.phunguy65.ttbs.backend.station.application.usecase.GetStationsUseCase;
import io.github.phunguy65.ttbs.backend.station.application.usecase.SearchStationsUseCase;
import io.github.phunguy65.ttbs.backend.station.domain.error.StationError;
import io.github.phunguy65.ttbs.backend.station.infrastructure.web.request.GetStationByIdRequest;
import io.github.phunguy65.ttbs.backend.station.infrastructure.web.request.GetStationsRequest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

@DisplayName("StationController list and get by id")
class StationControllerListAndGetByIdTest {

    private final GetStationByIdUseCase getStationByIdUseCase = mock(GetStationByIdUseCase.class);
    private final GetStationsUseCase getStationsUseCase = mock(GetStationsUseCase.class);
    private final SearchStationsUseCase searchStationsUseCase = mock(SearchStationsUseCase.class);
    private final StationController controller =
            new StationController(getStationByIdUseCase, getStationsUseCase, searchStationsUseCase);
    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    @Nested
    @DisplayName("list endpoint")
    class ListEndpoint {

        @Test
        @DisplayName("returns 200 with paged stations")
        void list_returns200WithPagedStations() {
            PageResponse<StationResponse> page =
                    PageResponse.of(List.of(stationResponse()), 0, 20, false, 1);
            when(getStationsUseCase.execute(new GetStationsQuery(0, 20))).thenReturn(page);

            var result = controller.list(new GetStationsRequest());

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().status()).isEqualTo("success");
            assertThat(result.getBody().data()).isEqualTo(page);
            assertThat(((PageResponse<?>) result.getBody().data()).content()).hasSize(1);
        }

        @Test
        @DisplayName("returns 200 with empty page when no stations")
        void list_returns200WithEmptyPageWhenNoStations() {
            PageResponse<StationResponse> page = PageResponse.empty(20);
            when(getStationsUseCase.execute(new GetStationsQuery(0, 20))).thenReturn(page);

            var result = controller.list(new GetStationsRequest());

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(((PageResponse<?>) result.getBody().data()).content()).isEmpty();
        }

        @Test
        @DisplayName("uses default pagination when no params provided")
        void list_usesDefaultPaginationWhenNoParamsProvided() {
            when(getStationsUseCase.execute(new GetStationsQuery(0, 20)))
                    .thenReturn(PageResponse.empty(20));

            controller.list(new GetStationsRequest());

            verify(getStationsUseCase).execute(new GetStationsQuery(0, 20));
        }
    }

    @Nested
    @DisplayName("get by id endpoint")
    class GetByIdEndpoint {

        @Test
        @DisplayName("returns 200 with station response when found")
        void getById_returns200WithStationResponseWhenFound() {
            UUID stationId = UUID.fromString("11111111-1111-1111-1111-111111111111");
            StationResponse response = stationResponse(stationId);
            when(getStationByIdUseCase.execute(
                            new io.github.phunguy65.ttbs.backend.station.application.query
                                    .GetStationByIdQuery(stationId)))
                    .thenReturn(Result.success(response));

            var result = controller.getById(stationId, new GetStationByIdRequest());

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().status()).isEqualTo("success");
            assertThat(result.getBody().data()).isEqualTo(response);
        }

        @Test
        @DisplayName("returns 404 with station not found error")
        void getById_returns404WithStationNotFoundError() {
            UUID stationId = UUID.fromString("22222222-2222-2222-2222-222222222222");
            when(getStationByIdUseCase.execute(
                            new io.github.phunguy65.ttbs.backend.station.application.query
                                    .GetStationByIdQuery(stationId)))
                    .thenReturn(Result.failure(new StationError.StationNotFound()));

            var result = controller.getById(stationId, new GetStationByIdRequest());

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().status()).isEqualTo("fail");
            FailData data = (FailData) result.getBody().data();
            assertThat(data.code()).isEqualTo(ErrorCode.STATION_NOT_FOUND);
            assertThat(data.message()).isEqualTo("Station not found");
        }
    }

    @Nested
    @DisplayName("response contract")
    class ResponseContract {

        @Test
        @DisplayName("returns JSend success wrapper with page data")
        void list_returnsJsendSuccessWrapperWithPageData() {
            when(getStationsUseCase.execute(new GetStationsQuery(0, 20)))
                    .thenReturn(PageResponse.of(List.of(stationResponse()), 0, 20, true, 30));

            JsonNode json = objectMapper.valueToTree(
                    controller.list(new GetStationsRequest()).getBody());

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
            UUID stationId = UUID.fromString("33333333-3333-3333-3333-333333333333");
            when(getStationByIdUseCase.execute(
                            new io.github.phunguy65.ttbs.backend.station.application.query
                                    .GetStationByIdQuery(stationId)))
                    .thenReturn(Result.failure(new StationError.StationNotFound()));

            JsonNode json = objectMapper.valueToTree(
                    controller.getById(stationId, new GetStationByIdRequest()).getBody());

            assertThat(json.get("status").asText()).isEqualTo("fail");
            assertThat(json.get("data").get("message").asText()).isEqualTo("Station not found");
            assertThat(json.get("data").get("code").asText()).isEqualTo("STATION_NOT_FOUND");
            assertThat(json.get("data").get("errors")).isEmpty();
        }
    }

    private StationResponse stationResponse() {
        return stationResponse(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    }

    private StationResponse stationResponse(UUID stationId) {
        return new StationResponse(
                stationId, "TESTHN", "Ha Noi", "Ha Noi", Instant.parse("2026-05-16T07:00:00Z"));
    }
}

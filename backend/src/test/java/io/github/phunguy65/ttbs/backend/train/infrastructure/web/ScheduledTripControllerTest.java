package io.github.phunguy65.ttbs.backend.train.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.phunguy65.ttbs.backend.shared.domain.SliceResponse;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.cursor.CursorEncoder;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.JsendResponse;
import io.github.phunguy65.ttbs.backend.train.application.query.SearchScheduledTripsCursor;
import io.github.phunguy65.ttbs.backend.train.application.response.SearchScheduledTripsResponse;
import io.github.phunguy65.ttbs.backend.train.application.usecase.GetScheduledTripByIdUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.GetScheduledTripsUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.SearchScheduledTripsUseCase;
import io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripStatus;
import io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.SearchScheduledTripsRequest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class ScheduledTripControllerTest {

    private final GetScheduledTripByIdUseCase getScheduledTripByIdUseCase =
            mock(GetScheduledTripByIdUseCase.class);
    private final GetScheduledTripsUseCase getScheduledTripsUseCase =
            mock(GetScheduledTripsUseCase.class);
    private final SearchScheduledTripsUseCase searchScheduledTripsUseCase =
            mock(SearchScheduledTripsUseCase.class);

    private final ScheduledTripController controller = new ScheduledTripController(
            getScheduledTripByIdUseCase, getScheduledTripsUseCase, searchScheduledTripsUseCase);

    @Test
    void filterReturnsEmptyMessageWhenNoTripsMatch() {
        SearchScheduledTripsRequest request = new SearchScheduledTripsRequest();
        when(searchScheduledTripsUseCase.execute(request.toQuery()))
                .thenReturn(SliceResponse.empty(20));

        @SuppressWarnings("unchecked")
        JsendResponse<SliceResponse<SearchScheduledTripsResponse>> response =
                (JsendResponse<SliceResponse<SearchScheduledTripsResponse>>)
                        controller.filter(request).getBody();

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message())
                .isEqualTo("No scheduled trips matched the selected filters.");
        assertThat(response.data().content()).isEmpty();
    }

    @Test
    void filterOmitsMessageWhenTripsExist() {
        SearchScheduledTripsRequest request = new SearchScheduledTripsRequest(
                null, null, null, null, null, null, null, null, null, "cursor-token", 20);
        SliceResponse<SearchScheduledTripsResponse> result = SliceResponse.of(
                List.of(new SearchScheduledTripsResponse(
                        UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        Instant.parse("2026-05-01T01:00:00Z"),
                        Instant.parse("2026-05-01T11:00:00Z"),
                        ScheduledTripStatus.SCHEDULED,
                        600,
                        8,
                        0,
                        null,
                        new SearchScheduledTripsResponse.Route(
                                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                                650000,
                                "VND",
                                new SearchScheduledTripsResponse.Station(
                                        UUID.fromString("33333333-3333-3333-3333-333333333333"),
                                        "HNO",
                                        "Ha Noi",
                                        "Ha Noi"),
                                new SearchScheduledTripsResponse.Station(
                                        UUID.fromString("44444444-4444-4444-4444-444444444444"),
                                        "DAD",
                                        "Da Nang",
                                        "Da Nang")))),
                20,
                false,
                null);
        when(searchScheduledTripsUseCase.execute(request.toQuery())).thenReturn(result);

        @SuppressWarnings("unchecked")
        JsendResponse<SliceResponse<SearchScheduledTripsResponse>> response =
                (JsendResponse<SliceResponse<SearchScheduledTripsResponse>>)
                        controller.filter(request).getBody();

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isNull();
        assertThat(response.data().content()).hasSize(1);
        verify(searchScheduledTripsUseCase).execute(eq(request.toQuery()));
    }

    @Test
    void requestToQueryPreservesOpaqueCursorCase() {
        String encodedCursor = new CursorEncoder(new ObjectMapper())
                .encode(new SearchScheduledTripsCursor(
                        "2026-05-01T01:00:00Z",
                        UUID.fromString("11111111-1111-1111-1111-111111111111")));
        SearchScheduledTripsRequest request = new SearchScheduledTripsRequest(
                null, null, null, null, null, null, null, null, null, encodedCursor, 20);

        assertThat(request.toQuery().cursor()).isEqualTo(encodedCursor);
    }
}

package io.github.phunguy65.ttbs.backend.station.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.JsendResponse;
import io.github.phunguy65.ttbs.backend.station.application.response.StationSearchResponse;
import io.github.phunguy65.ttbs.backend.station.application.usecase.GetStationByIdUseCase;
import io.github.phunguy65.ttbs.backend.station.application.usecase.GetStationsUseCase;
import io.github.phunguy65.ttbs.backend.station.application.usecase.SearchStationsUseCase;
import io.github.phunguy65.ttbs.backend.station.infrastructure.web.request.SearchStationsRequest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StationControllerTest {

    private final GetStationByIdUseCase getStationByIdUseCase = mock(GetStationByIdUseCase.class);
    private final GetStationsUseCase getStationsUseCase = mock(GetStationsUseCase.class);
    private final SearchStationsUseCase searchStationsUseCase = mock(SearchStationsUseCase.class);

    private final StationController controller =
            new StationController(getStationByIdUseCase, getStationsUseCase, searchStationsUseCase);

    @Test
    void searchReturnsEmptyMessageWhenNoStationMatches() {
        SearchStationsRequest request = new SearchStationsRequest();
        when(searchStationsUseCase.execute(request.toQuery())).thenReturn(List.of());

        @SuppressWarnings("unchecked")
        JsendResponse<List<StationSearchResponse>> response =
                (JsendResponse<List<StationSearchResponse>>)
                        controller.search(request).getBody();

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("No stations matched your search.");
        assertThat(response.data()).isEmpty();
    }

    @Test
    void searchReturnsDataWithoutMessageWhenMatchesExist() {
        SearchStationsRequest request = new SearchStationsRequest("ha", 10);
        when(searchStationsUseCase.execute(request.toQuery()))
                .thenReturn(List.of(new StationSearchResponse(
                        UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        "HNO",
                        "Ha Noi",
                        "Ha Noi")));

        @SuppressWarnings("unchecked")
        JsendResponse<List<StationSearchResponse>> response =
                (JsendResponse<List<StationSearchResponse>>)
                        controller.search(request).getBody();

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isNull();
        assertThat(response.data()).hasSize(1);
    }
}

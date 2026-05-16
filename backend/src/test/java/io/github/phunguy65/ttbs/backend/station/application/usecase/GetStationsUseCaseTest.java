package io.github.phunguy65.ttbs.backend.station.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.SortOrder;
import io.github.phunguy65.ttbs.backend.station.application.query.GetStationsQuery;
import io.github.phunguy65.ttbs.backend.station.application.response.StationResponse;
import io.github.phunguy65.ttbs.backend.station.domain.projection.StationSummary;
import io.github.phunguy65.ttbs.backend.station.domain.repository.StationRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetStationsUseCase")
class GetStationsUseCaseTest {

    @Mock
    private StationRepository stationRepository;

    @InjectMocks
    private GetStationsUseCase useCase;

    @Nested
    @DisplayName("happy path")
    class HappyPath {

        @Test
        @DisplayName("returns page response with mapped stations")
        void execute_returnsPageResponseWithMappedStations() {
            List<StationSummary> summaries = List.of(
                    stationSummary(
                            "11111111-1111-1111-1111-111111111111", "TESTHN", "Ha Noi", "Ha Noi"),
                    stationSummary(
                            "22222222-2222-2222-2222-222222222222",
                            "TESTSG",
                            "Sai Gon",
                            "Ho Chi Minh"));
            when(stationRepository.findAllSummaries(
                            0, 20, List.of(SortOrder.asc("code"), SortOrder.asc("id"))))
                    .thenReturn(PageResponse.of(summaries, 0, 20, false, 2));

            PageResponse<StationResponse> result = useCase.execute(new GetStationsQuery(0, 20));

            assertThat(result.content()).hasSize(2);
            assertThat(result.content().get(0))
                    .usingRecursiveComparison()
                    .isEqualTo(new StationResponse(
                            summaries.get(0).id(),
                            summaries.get(0).code(),
                            summaries.get(0).name(),
                            summaries.get(0).city(),
                            summaries.get(0).createdAt()));
            assertThat(result.content().get(1))
                    .usingRecursiveComparison()
                    .isEqualTo(new StationResponse(
                            summaries.get(1).id(),
                            summaries.get(1).code(),
                            summaries.get(1).name(),
                            summaries.get(1).city(),
                            summaries.get(1).createdAt()));
        }

        @Test
        @DisplayName("returns empty page when no stations exist")
        void execute_returnsEmptyPageWhenNoStationsExist() {
            when(stationRepository.findAllSummaries(
                            0, 20, List.of(SortOrder.asc("code"), SortOrder.asc("id"))))
                    .thenReturn(PageResponse.empty(20));

            PageResponse<StationResponse> result = useCase.execute(new GetStationsQuery(0, 20));

            assertThat(result.content()).isEmpty();
            assertThat(result.total()).isZero();
        }
    }

    @Nested
    @DisplayName("internal behavior")
    class InternalBehavior {

        @Test
        @DisplayName("delegates to repository with correct sort order")
        void execute_delegatesToRepositoryWithCorrectSortOrder() {
            when(stationRepository.findAllSummaries(
                            1, 10, List.of(SortOrder.asc("code"), SortOrder.asc("id"))))
                    .thenReturn(PageResponse.empty(10));

            useCase.execute(new GetStationsQuery(1, 10));

            ArgumentCaptor<List<SortOrder>> sortCaptor = ArgumentCaptor.captor();
            verify(stationRepository)
                    .findAllSummaries(
                            org.mockito.ArgumentMatchers.eq(1),
                            org.mockito.ArgumentMatchers.eq(10),
                            sortCaptor.capture());
            assertThat(sortCaptor.getValue())
                    .containsExactly(SortOrder.asc("code"), SortOrder.asc("id"));
        }

        @Test
        @DisplayName("passes page and size from query")
        void execute_passesPageAndSizeFromQuery() {
            when(stationRepository.findAllSummaries(
                            3, 50, List.of(SortOrder.asc("code"), SortOrder.asc("id"))))
                    .thenReturn(PageResponse.empty(50));

            useCase.execute(new GetStationsQuery(3, 50));

            verify(stationRepository)
                    .findAllSummaries(3, 50, List.of(SortOrder.asc("code"), SortOrder.asc("id")));
        }

        @Test
        @DisplayName("preserves pagination metadata")
        void execute_preservesPaginationMetadata() {
            when(stationRepository.findAllSummaries(
                            2, 20, List.of(SortOrder.asc("code"), SortOrder.asc("id"))))
                    .thenReturn(PageResponse.of(
                            List.of(stationSummary(
                                    "33333333-3333-3333-3333-333333333333",
                                    "TESTDN",
                                    "Da Nang",
                                    "Da Nang")),
                            2,
                            20,
                            true,
                            60));

            PageResponse<StationResponse> result = useCase.execute(new GetStationsQuery(2, 20));

            assertThat(result.page()).isEqualTo(2);
            assertThat(result.size()).isEqualTo(20);
            assertThat(result.hasNext()).isTrue();
            assertThat(result.hasPrevious()).isTrue();
            assertThat(result.total()).isEqualTo(60);
        }
    }

    @Nested
    @DisplayName("exception handling")
    class ExceptionHandling {

        @Test
        @DisplayName("propagates find all summaries failures")
        void execute_propagatesFindAllSummariesFailures() {
            RuntimeException failure = new RuntimeException("database unavailable");
            when(stationRepository.findAllSummaries(
                            org.mockito.ArgumentMatchers.eq(0),
                            org.mockito.ArgumentMatchers.eq(20),
                            anyList()))
                    .thenThrow(failure);

            assertThatThrownBy(() -> useCase.execute(new GetStationsQuery(0, 20)))
                    .isSameAs(failure);
        }
    }

    private StationSummary stationSummary(String id, String code, String name, String city) {
        return new StationSummary(
                UUID.fromString(id), code, name, city, Instant.parse("2026-05-16T07:00:00Z"));
    }
}

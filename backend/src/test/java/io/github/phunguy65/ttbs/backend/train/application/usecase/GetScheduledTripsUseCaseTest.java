package io.github.phunguy65.ttbs.backend.train.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.SortOrder;
import io.github.phunguy65.ttbs.backend.train.application.query.GetScheduledTripsQuery;
import io.github.phunguy65.ttbs.backend.train.application.response.ScheduledTripResponse;
import io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripStatus;
import io.github.phunguy65.ttbs.backend.train.domain.projection.ScheduledTripSummary;
import io.github.phunguy65.ttbs.backend.train.domain.repository.ScheduledTripRepository;
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
@DisplayName("GetScheduledTripsUseCase")
class GetScheduledTripsUseCaseTest {

    @Mock
    private ScheduledTripRepository scheduledTripRepository;

    @InjectMocks
    private GetScheduledTripsUseCase useCase;

    @Nested
    @DisplayName("happy path")
    class HappyPath {

        @Test
        @DisplayName("returns page response with mapped scheduled trips")
        void execute_returnsPageResponseWithMappedScheduledTrips() {
            List<ScheduledTripSummary> summaries = List.of(
                    scheduledTripSummary("11111111-1111-1111-1111-111111111111", "SCHEDULED"),
                    scheduledTripSummary("22222222-2222-2222-2222-222222222222", "BOARDING"));
            when(scheduledTripRepository.findAllSummaries(
                            0, 20, List.of(SortOrder.asc("departureTime"), SortOrder.asc("id"))))
                    .thenReturn(PageResponse.of(summaries, 0, 20, false, 2));

            PageResponse<ScheduledTripResponse> result =
                    useCase.execute(new GetScheduledTripsQuery(0, 20));

            assertThat(result.content()).hasSize(2);
            assertThat(result.content().get(0))
                    .usingRecursiveComparison()
                    .isEqualTo(responseFrom(summaries.get(0), ScheduledTripStatus.SCHEDULED));
            assertThat(result.content().get(1))
                    .usingRecursiveComparison()
                    .isEqualTo(responseFrom(summaries.get(1), ScheduledTripStatus.BOARDING));
        }

        @Test
        @DisplayName("returns empty page when no scheduled trips exist")
        void execute_returnsEmptyPageWhenNoScheduledTripsExist() {
            when(scheduledTripRepository.findAllSummaries(
                            0, 20, List.of(SortOrder.asc("departureTime"), SortOrder.asc("id"))))
                    .thenReturn(PageResponse.empty(20));

            PageResponse<ScheduledTripResponse> result =
                    useCase.execute(new GetScheduledTripsQuery(0, 20));

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
            when(scheduledTripRepository.findAllSummaries(
                            1, 10, List.of(SortOrder.asc("departureTime"), SortOrder.asc("id"))))
                    .thenReturn(PageResponse.empty(10));

            useCase.execute(new GetScheduledTripsQuery(1, 10));

            ArgumentCaptor<List<SortOrder>> sortCaptor = ArgumentCaptor.captor();
            verify(scheduledTripRepository)
                    .findAllSummaries(
                            org.mockito.ArgumentMatchers.eq(1),
                            org.mockito.ArgumentMatchers.eq(10),
                            sortCaptor.capture());
            assertThat(sortCaptor.getValue())
                    .containsExactly(SortOrder.asc("departureTime"), SortOrder.asc("id"));
        }

        @Test
        @DisplayName("passes page and size from query")
        void execute_passesPageAndSizeFromQuery() {
            when(scheduledTripRepository.findAllSummaries(
                            3, 50, List.of(SortOrder.asc("departureTime"), SortOrder.asc("id"))))
                    .thenReturn(PageResponse.empty(50));

            useCase.execute(new GetScheduledTripsQuery(3, 50));

            verify(scheduledTripRepository)
                    .findAllSummaries(
                            3, 50, List.of(SortOrder.asc("departureTime"), SortOrder.asc("id")));
        }

        @Test
        @DisplayName("preserves pagination metadata")
        void execute_preservesPaginationMetadata() {
            when(scheduledTripRepository.findAllSummaries(
                            2, 20, List.of(SortOrder.asc("departureTime"), SortOrder.asc("id"))))
                    .thenReturn(PageResponse.of(
                            List.of(scheduledTripSummary(
                                    "33333333-3333-3333-3333-333333333333", "ARRIVED")),
                            2,
                            20,
                            true,
                            60));

            PageResponse<ScheduledTripResponse> result =
                    useCase.execute(new GetScheduledTripsQuery(2, 20));

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
            when(scheduledTripRepository.findAllSummaries(
                            org.mockito.ArgumentMatchers.eq(0),
                            org.mockito.ArgumentMatchers.eq(20),
                            anyList()))
                    .thenThrow(failure);

            assertThatThrownBy(() -> useCase.execute(new GetScheduledTripsQuery(0, 20)))
                    .isSameAs(failure);
        }
    }

    private ScheduledTripResponse responseFrom(
            ScheduledTripSummary summary, ScheduledTripStatus status) {
        return new ScheduledTripResponse(
                summary.id(),
                summary.routeTemplateId(),
                summary.trainId(),
                summary.departureTime(),
                summary.arrivalTime(),
                status,
                summary.createdAt());
    }

    private ScheduledTripSummary scheduledTripSummary(String id, String status) {
        return new ScheduledTripSummary(
                UUID.fromString(id),
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
                Instant.parse("2026-05-16T08:00:00Z"),
                Instant.parse("2026-05-16T12:00:00Z"),
                status,
                Instant.parse("2026-05-15T08:00:00Z"));
    }
}

package io.github.phunguy65.ttbs.backend.train.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.SortOrder;
import io.github.phunguy65.ttbs.backend.train.application.query.GetAvailableSeatsQuery;
import io.github.phunguy65.ttbs.backend.train.application.response.SeatResponse;
import io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripId;
import io.github.phunguy65.ttbs.backend.train.domain.projection.SeatSummary;
import io.github.phunguy65.ttbs.backend.train.domain.repository.SeatRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetAvailableSeatsForScheduledTripUseCase")
class GetAvailableSeatsForScheduledTripUseCaseTest {

    @Mock
    private SeatRepository seatRepository;

    @InjectMocks
    private GetAvailableSeatsForScheduledTripUseCase useCase;

    @Nested
    @DisplayName("happy path")
    class HappyPath {

        @Test
        @DisplayName("maps seat summary fields to response")
        void execute_mapsSeatSummaryFieldsToResponse() {
            UUID scheduledTripId = UUID.fromString("11111111-1111-1111-1111-111111111111");
            SeatSummary summary = seatSummary("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", "1A");
            when(seatRepository.findAllAvailableSummaries(
                            0, 20, expectedSort(), ScheduledTripId.of(scheduledTripId)))
                    .thenReturn(PageResponse.of(List.of(summary), 0, 20, false, 1));

            PageResponse<SeatResponse> response =
                    useCase.execute(new GetAvailableSeatsQuery(0, 20, scheduledTripId));

            assertThat(response.content())
                    .containsExactly(new SeatResponse(
                            summary.id(),
                            summary.coachId(),
                            summary.seatNumber(),
                            summary.createdAt()));
        }

        @Test
        @DisplayName("maps multiple seats correctly")
        void execute_mapsMultipleSeatsCorrectly() {
            UUID scheduledTripId = UUID.fromString("22222222-2222-2222-2222-222222222222");
            SeatSummary first = seatSummary("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", "1A");
            SeatSummary second = seatSummary("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb", "1B");
            when(seatRepository.findAllAvailableSummaries(
                            0, 20, expectedSort(), ScheduledTripId.of(scheduledTripId)))
                    .thenReturn(PageResponse.of(List.of(first, second), 0, 20, false, 2));

            PageResponse<SeatResponse> response =
                    useCase.execute(new GetAvailableSeatsQuery(0, 20, scheduledTripId));

            assertThat(response.content())
                    .containsExactly(
                            new SeatResponse(
                                    first.id(),
                                    first.coachId(),
                                    first.seatNumber(),
                                    first.createdAt()),
                            new SeatResponse(
                                    second.id(),
                                    second.coachId(),
                                    second.seatNumber(),
                                    second.createdAt()));
        }
    }

    @Nested
    @DisplayName("pagination")
    class Pagination {

        @Test
        @DisplayName("preserves pagination metadata")
        void execute_preservesPaginationMetadata() {
            UUID scheduledTripId = UUID.fromString("33333333-3333-3333-3333-333333333333");
            when(seatRepository.findAllAvailableSummaries(
                            2, 10, expectedSort(), ScheduledTripId.of(scheduledTripId)))
                    .thenReturn(PageResponse.of(
                            List.of(seatSummary("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", "2A")),
                            2,
                            10,
                            true,
                            31));

            PageResponse<SeatResponse> response =
                    useCase.execute(new GetAvailableSeatsQuery(2, 10, scheduledTripId));

            assertThat(response.page()).isEqualTo(2);
            assertThat(response.size()).isEqualTo(10);
            assertThat(response.hasNext()).isTrue();
            assertThat(response.hasPrevious()).isTrue();
            assertThat(response.total()).isEqualTo(31);
        }

        @Test
        @DisplayName("returns empty content with correct metadata")
        void execute_returnsEmptyContentWithCorrectMetadata() {
            UUID scheduledTripId = UUID.fromString("44444444-4444-4444-4444-444444444444");
            when(seatRepository.findAllAvailableSummaries(
                            0, 20, expectedSort(), ScheduledTripId.of(scheduledTripId)))
                    .thenReturn(PageResponse.empty(20));

            PageResponse<SeatResponse> response =
                    useCase.execute(new GetAvailableSeatsQuery(0, 20, scheduledTripId));

            assertThat(response.content()).isEmpty();
            assertThat(response.page()).isZero();
            assertThat(response.size()).isEqualTo(20);
            assertThat(response.hasNext()).isFalse();
            assertThat(response.total()).isZero();
        }
    }

    @Nested
    @DisplayName("internal behavior")
    class InternalBehavior {

        @Test
        @DisplayName("delegates with seat number and id ascending sort")
        void execute_delegatesWithSeatNumberAndIdAscendingSort() {
            UUID scheduledTripId = UUID.fromString("55555555-5555-5555-5555-555555555555");
            when(seatRepository.findAllAvailableSummaries(
                            1, 25, expectedSort(), ScheduledTripId.of(scheduledTripId)))
                    .thenReturn(PageResponse.empty(25));

            useCase.execute(new GetAvailableSeatsQuery(1, 25, scheduledTripId));

            verify(seatRepository)
                    .findAllAvailableSummaries(
                            1, 25, expectedSort(), ScheduledTripId.of(scheduledTripId));
        }
    }

    @Nested
    @DisplayName("exception handling")
    class ExceptionHandling {

        @Test
        @DisplayName("propagates repository failures")
        void execute_propagatesRepositoryFailures() {
            UUID scheduledTripId = UUID.fromString("66666666-6666-6666-6666-666666666666");
            RuntimeException failure = new RuntimeException("database unavailable");
            when(seatRepository.findAllAvailableSummaries(
                            0, 20, expectedSort(), ScheduledTripId.of(scheduledTripId)))
                    .thenThrow(failure);

            assertThatThrownBy(() ->
                            useCase.execute(new GetAvailableSeatsQuery(0, 20, scheduledTripId)))
                    .isSameAs(failure);
        }
    }

    private List<SortOrder> expectedSort() {
        return List.of(SortOrder.asc("seatNumber"), SortOrder.asc("id"));
    }

    private SeatSummary seatSummary(String id, String seatNumber) {
        return new SeatSummary(
                UUID.fromString(id),
                UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"),
                seatNumber,
                Instant.parse("2026-05-16T08:00:00Z"));
    }
}

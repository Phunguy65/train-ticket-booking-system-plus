package io.github.phunguy65.ttbs.backend.train.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.query.GetCoachSeatMapQuery;
import io.github.phunguy65.ttbs.backend.train.application.response.CoachSeatMapResponse;
import io.github.phunguy65.ttbs.backend.train.domain.error.ScheduledTripError;
import io.github.phunguy65.ttbs.backend.train.domain.model.CoachId;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteSeatAvailabilityStatus;
import io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripId;
import io.github.phunguy65.ttbs.backend.train.domain.projection.CoachSeatMapCoachSummary;
import io.github.phunguy65.ttbs.backend.train.domain.projection.CoachSeatMapSeatSummary;
import io.github.phunguy65.ttbs.backend.train.domain.repository.ScheduledTripRepository;
import io.github.phunguy65.ttbs.backend.train.domain.repository.ScheduledTripSeatMapRepository;
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
@DisplayName("GetCoachSeatMapByScheduledTripUseCase")
class GetCoachSeatMapByScheduledTripUseCaseTest {

    @Mock
    private ScheduledTripRepository scheduledTripRepository;

    @Mock
    private ScheduledTripSeatMapRepository scheduledTripSeatMapRepository;

    @InjectMocks
    private GetCoachSeatMapByScheduledTripUseCase useCase;

    @Nested
    @DisplayName("happy path")
    class HappyPath {

        @Test
        @DisplayName("returns coach seat map when coaches are found")
        void execute_returnsCoachSeatMapWhenCoachesAreFound() {
            UUID scheduledTripId = UUID.fromString("11111111-1111-1111-1111-111111111111");
            UUID coachId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
            CoachSeatMapCoachSummary coach = new CoachSeatMapCoachSummary(coachId, 1, 2);
            CoachSeatMapSeatSummary seat = seatSummary(
                    "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                    coachId,
                    "1A",
                    RouteSeatAvailabilityStatus.AVAILABLE);
            when(scheduledTripSeatMapRepository.findCoachSummariesByScheduledTripId(
                            0, 20, ScheduledTripId.of(scheduledTripId)))
                    .thenReturn(PageResponse.of(List.of(coach), 0, 20, false, 1));
            when(scheduledTripSeatMapRepository.findSeatSummariesByScheduledTripIdAndCoachIds(
                            ScheduledTripId.of(scheduledTripId), List.of(CoachId.of(coachId))))
                    .thenReturn(List.of(seat));

            Result<PageResponse<CoachSeatMapResponse>, ScheduledTripError> result =
                    useCase.execute(new GetCoachSeatMapQuery(0, 20, scheduledTripId));

            assertThat(result.isSuccess()).isTrue();
            PageResponse<CoachSeatMapResponse> response = successValue(result);
            assertThat(response.content()).hasSize(1);
            assertThat(response.content().get(0).id()).isEqualTo(coachId);
            assertThat(response.content().get(0).seats())
                    .containsExactly(new CoachSeatMapResponse.Seat(
                            seat.id(), seat.seatNumber(), seat.status()));
        }

        @Test
        @DisplayName("groups seats into respective coaches")
        void execute_groupsSeatsIntoRespectiveCoaches() {
            UUID scheduledTripId = UUID.fromString("22222222-2222-2222-2222-222222222222");
            UUID firstCoachId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
            UUID secondCoachId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
            when(scheduledTripSeatMapRepository.findCoachSummariesByScheduledTripId(
                            0, 20, ScheduledTripId.of(scheduledTripId)))
                    .thenReturn(PageResponse.of(
                            List.of(
                                    new CoachSeatMapCoachSummary(firstCoachId, 1, 2),
                                    new CoachSeatMapCoachSummary(secondCoachId, 2, 2)),
                            0,
                            20,
                            false,
                            2));
            when(scheduledTripSeatMapRepository.findSeatSummariesByScheduledTripIdAndCoachIds(
                            ScheduledTripId.of(scheduledTripId),
                            List.of(CoachId.of(firstCoachId), CoachId.of(secondCoachId))))
                    .thenReturn(List.of(
                            seatSummary(
                                    "cccccccc-cccc-cccc-cccc-cccccccccccc",
                                    firstCoachId,
                                    "1A",
                                    RouteSeatAvailabilityStatus.AVAILABLE),
                            seatSummary(
                                    "dddddddd-dddd-dddd-dddd-dddddddddddd",
                                    secondCoachId,
                                    "2A",
                                    RouteSeatAvailabilityStatus.BOOKED)));

            PageResponse<CoachSeatMapResponse> response =
                    successValue(useCase.execute(new GetCoachSeatMapQuery(0, 20, scheduledTripId)));

            assertThat(response.content().get(0).seats())
                    .extracting(CoachSeatMapResponse.Seat::seatNumber)
                    .containsExactly("1A");
            assertThat(response.content().get(1).seats())
                    .extracting(CoachSeatMapResponse.Seat::seatNumber)
                    .containsExactly("2A");
        }

        @Test
        @DisplayName("uses empty seat list for coach with no seats")
        void execute_usesEmptySeatListForCoachWithNoSeats() {
            UUID scheduledTripId = UUID.fromString("33333333-3333-3333-3333-333333333333");
            UUID coachId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
            when(scheduledTripSeatMapRepository.findCoachSummariesByScheduledTripId(
                            0, 20, ScheduledTripId.of(scheduledTripId)))
                    .thenReturn(PageResponse.of(
                            List.of(new CoachSeatMapCoachSummary(coachId, 1, 64)),
                            0,
                            20,
                            false,
                            1));
            when(scheduledTripSeatMapRepository.findSeatSummariesByScheduledTripIdAndCoachIds(
                            ScheduledTripId.of(scheduledTripId), List.of(CoachId.of(coachId))))
                    .thenReturn(List.of());

            PageResponse<CoachSeatMapResponse> response =
                    successValue(useCase.execute(new GetCoachSeatMapQuery(0, 20, scheduledTripId)));

            assertThat(response.content().get(0).seats()).isEmpty();
        }
    }

    @Nested
    @DisplayName("empty and not found")
    class EmptyAndNotFound {

        @Test
        @DisplayName("returns scheduled trip not found when empty coaches and trip does not exist")
        void execute_returnsScheduledTripNotFoundWhenEmptyCoachesAndTripDoesNotExist() {
            UUID scheduledTripId = UUID.fromString("44444444-4444-4444-4444-444444444444");
            when(scheduledTripSeatMapRepository.findCoachSummariesByScheduledTripId(
                            0, 20, ScheduledTripId.of(scheduledTripId)))
                    .thenReturn(PageResponse.empty(20));
            when(scheduledTripRepository.existsById(ScheduledTripId.of(scheduledTripId)))
                    .thenReturn(false);

            Result<PageResponse<CoachSeatMapResponse>, ScheduledTripError> result =
                    useCase.execute(new GetCoachSeatMapQuery(0, 20, scheduledTripId));

            assertThat(result.isFailure()).isTrue();
            assertThat(((Result.Failure<PageResponse<CoachSeatMapResponse>, ScheduledTripError>)
                                    result)
                            .error())
                    .isInstanceOf(ScheduledTripError.ScheduledTripNotFound.class);
            verify(scheduledTripSeatMapRepository, never())
                    .findSeatSummariesByScheduledTripIdAndCoachIds(
                            ScheduledTripId.of(scheduledTripId), List.of());
        }

        @Test
        @DisplayName("returns empty page when empty coaches and trip exists")
        void execute_returnsEmptyPageWhenEmptyCoachesAndTripExists() {
            UUID scheduledTripId = UUID.fromString("55555555-5555-5555-5555-555555555555");
            when(scheduledTripSeatMapRepository.findCoachSummariesByScheduledTripId(
                            1, 10, ScheduledTripId.of(scheduledTripId)))
                    .thenReturn(PageResponse.of(List.of(), 1, 10, false, 0));
            when(scheduledTripRepository.existsById(ScheduledTripId.of(scheduledTripId)))
                    .thenReturn(true);

            PageResponse<CoachSeatMapResponse> response =
                    successValue(useCase.execute(new GetCoachSeatMapQuery(1, 10, scheduledTripId)));

            assertThat(response.content()).isEmpty();
            assertThat(response.page()).isEqualTo(1);
            assertThat(response.size()).isEqualTo(10);
            assertThat(response.total()).isZero();
        }
    }

    @Nested
    @DisplayName("pagination")
    class Pagination {

        @Test
        @DisplayName("preserves pagination metadata")
        void execute_preservesPaginationMetadata() {
            UUID scheduledTripId = UUID.fromString("66666666-6666-6666-6666-666666666666");
            UUID coachId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
            when(scheduledTripSeatMapRepository.findCoachSummariesByScheduledTripId(
                            2, 5, ScheduledTripId.of(scheduledTripId)))
                    .thenReturn(PageResponse.of(
                            List.of(new CoachSeatMapCoachSummary(coachId, 3, 44)), 2, 5, true, 16));
            when(scheduledTripSeatMapRepository.findSeatSummariesByScheduledTripIdAndCoachIds(
                            ScheduledTripId.of(scheduledTripId), List.of(CoachId.of(coachId))))
                    .thenReturn(List.of());

            PageResponse<CoachSeatMapResponse> response =
                    successValue(useCase.execute(new GetCoachSeatMapQuery(2, 5, scheduledTripId)));

            assertThat(response.page()).isEqualTo(2);
            assertThat(response.size()).isEqualTo(5);
            assertThat(response.hasNext()).isTrue();
            assertThat(response.hasPrevious()).isTrue();
            assertThat(response.total()).isEqualTo(16);
        }
    }

    @Nested
    @DisplayName("exception handling")
    class ExceptionHandling {

        @Test
        @DisplayName("propagates repository failures")
        void execute_propagatesRepositoryFailures() {
            UUID scheduledTripId = UUID.fromString("77777777-7777-7777-7777-777777777777");
            RuntimeException failure = new RuntimeException("database unavailable");
            when(scheduledTripSeatMapRepository.findCoachSummariesByScheduledTripId(
                            0, 20, ScheduledTripId.of(scheduledTripId)))
                    .thenThrow(failure);

            assertThatThrownBy(
                            () -> useCase.execute(new GetCoachSeatMapQuery(0, 20, scheduledTripId)))
                    .isSameAs(failure);
        }
    }

    private PageResponse<CoachSeatMapResponse> successValue(
            Result<PageResponse<CoachSeatMapResponse>, ScheduledTripError> result) {
        return ((Result.Success<PageResponse<CoachSeatMapResponse>, ScheduledTripError>) result)
                .value();
    }

    private CoachSeatMapSeatSummary seatSummary(
            String id, UUID coachId, String seatNumber, RouteSeatAvailabilityStatus status) {
        return new CoachSeatMapSeatSummary(UUID.fromString(id), coachId, seatNumber, status);
    }
}

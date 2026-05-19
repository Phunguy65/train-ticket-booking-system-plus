package io.github.phunguy65.ttbs.backend.train.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.query.GetScheduledTripByIdQuery;
import io.github.phunguy65.ttbs.backend.train.application.response.ScheduledTripDetailResponse;
import io.github.phunguy65.ttbs.backend.train.domain.error.ScheduledTripError;
import io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripId;
import io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripStatus;
import io.github.phunguy65.ttbs.backend.train.domain.projection.ScheduledTripEnrichedSummary;
import io.github.phunguy65.ttbs.backend.train.domain.repository.ScheduledTripRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetScheduledTripByIdUseCase")
class GetScheduledTripByIdUseCaseTest {

    @Mock
    private ScheduledTripRepository scheduledTripRepository;

    @InjectMocks
    private GetScheduledTripByIdUseCase useCase;

    @Nested
    @DisplayName("happy path")
    class HappyPath {

        @Test
        @DisplayName("returns scheduled trip detail response when found")
        void execute_returnsScheduledTripDetailResponseWhenFound() {
            UUID scheduledTripId = UUID.fromString("11111111-1111-1111-1111-111111111111");
            ScheduledTripEnrichedSummary summary = enrichedSummary(scheduledTripId);
            when(scheduledTripRepository.findEnrichedById(ScheduledTripId.of(scheduledTripId)))
                    .thenReturn(Optional.of(summary));

            Result<ScheduledTripDetailResponse, ScheduledTripError> result =
                    useCase.execute(new GetScheduledTripByIdQuery(scheduledTripId));

            assertThat(result.isSuccess()).isTrue();
            ScheduledTripDetailResponse response = ((Result.Success<
                                    ScheduledTripDetailResponse, ScheduledTripError>)
                            result)
                    .value();
            assertThat(response.id()).isEqualTo(summary.id());
            assertThat(response.departureTime()).isEqualTo(summary.departureTime());
            assertThat(response.arrivalTime()).isEqualTo(summary.arrivalTime());
            assertThat(response.status()).isEqualTo(ScheduledTripStatus.SCHEDULED);
            assertThat(response.createdAt()).isEqualTo(summary.createdAt());
            assertThat(response.train().id()).isEqualTo(summary.trainId());
            assertThat(response.train().trainNumber()).isEqualTo(summary.trainNumber());
            assertThat(response.train().name()).isEqualTo(summary.trainName());
            assertThat(response.train().totalSeats()).isEqualTo(summary.trainTotalSeats());
            assertThat(response.route().id()).isEqualTo(summary.routeTemplateId());
            assertThat(response.route().basePrice()).isEqualTo(summary.routeBasePrice());
            assertThat(response.route().currency()).isEqualTo(summary.routeCurrency());
            assertThat(response.route().origin().id()).isEqualTo(summary.originStationId());
            assertThat(response.route().destination().id())
                    .isEqualTo(summary.destinationStationId());
        }
    }

    @Nested
    @DisplayName("failure path")
    class FailurePath {

        @Test
        @DisplayName("returns scheduled trip not found when repository returns empty")
        void execute_returnsScheduledTripNotFoundWhenRepositoryReturnsEmpty() {
            UUID scheduledTripId = UUID.fromString("22222222-2222-2222-2222-222222222222");
            when(scheduledTripRepository.findEnrichedById(ScheduledTripId.of(scheduledTripId)))
                    .thenReturn(Optional.empty());

            Result<ScheduledTripDetailResponse, ScheduledTripError> result =
                    useCase.execute(new GetScheduledTripByIdQuery(scheduledTripId));

            assertThat(result.isFailure()).isTrue();
            ScheduledTripError error = ((Result.Failure<
                                    ScheduledTripDetailResponse, ScheduledTripError>)
                            result)
                    .error();
            assertThat(error).isInstanceOf(ScheduledTripError.ScheduledTripNotFound.class);
        }
    }

    @Nested
    @DisplayName("internal behavior")
    class InternalBehavior {

        @Test
        @DisplayName("calls find enriched by id with exact scheduled trip id")
        void execute_callsFindEnrichedByIdWithExactScheduledTripId() {
            UUID scheduledTripId = UUID.fromString("33333333-3333-3333-3333-333333333333");
            when(scheduledTripRepository.findEnrichedById(ScheduledTripId.of(scheduledTripId)))
                    .thenReturn(Optional.of(enrichedSummary(scheduledTripId)));

            useCase.execute(new GetScheduledTripByIdQuery(scheduledTripId));

            verify(scheduledTripRepository).findEnrichedById(ScheduledTripId.of(scheduledTripId));
        }

        @Test
        @DisplayName("maps null train fields to null train in response")
        void execute_mapsNullTrainFieldsToNullTrainInResponse() {
            UUID scheduledTripId = UUID.fromString("44444444-4444-4444-4444-444444444444");
            when(scheduledTripRepository.findEnrichedById(ScheduledTripId.of(scheduledTripId)))
                    .thenReturn(Optional.of(enrichedSummary(scheduledTripId, null, null, null)));

            ScheduledTripDetailResponse response = successValue(scheduledTripId);

            assertThat(response.train()).isNull();
        }

        @Test
        @DisplayName("maps null train number only to null train in response")
        void execute_mapsNullTrainNumberOnlyToNullTrainInResponse() {
            UUID scheduledTripId = UUID.fromString("55555555-5555-5555-5555-555555555555");
            when(scheduledTripRepository.findEnrichedById(ScheduledTripId.of(scheduledTripId)))
                    .thenReturn(
                            Optional.of(enrichedSummary(scheduledTripId, null, "Express", 120)));

            ScheduledTripDetailResponse response = successValue(scheduledTripId);

            assertThat(response.train()).isNull();
        }

        @Test
        @DisplayName("maps all fields from enriched summary to response")
        void execute_mapsAllFieldsFromEnrichedSummaryToResponse() {
            UUID scheduledTripId = UUID.fromString("66666666-6666-6666-6666-666666666666");
            ScheduledTripEnrichedSummary summary = enrichedSummary(scheduledTripId);
            when(scheduledTripRepository.findEnrichedById(ScheduledTripId.of(scheduledTripId)))
                    .thenReturn(Optional.of(summary));

            ScheduledTripDetailResponse response = successValue(scheduledTripId);

            assertThat(response)
                    .usingRecursiveComparison()
                    .isEqualTo(new ScheduledTripDetailResponse(
                            summary.id(),
                            summary.routeTemplateId(),
                            summary.trainId(),
                            summary.departureTime(),
                            summary.arrivalTime(),
                            ScheduledTripStatus.SCHEDULED,
                            summary.createdAt(),
                            new ScheduledTripDetailResponse.Train(
                                    summary.trainId(),
                                    summary.trainNumber(),
                                    summary.trainName(),
                                    summary.trainTotalSeats()),
                            new ScheduledTripDetailResponse.Route(
                                    summary.routeTemplateId(),
                                    summary.routeBasePrice(),
                                    summary.routeCurrency(),
                                    new ScheduledTripDetailResponse.Station(
                                            summary.originStationId(),
                                            summary.originStationCode(),
                                            summary.originStationName(),
                                            summary.originStationCity()),
                                    new ScheduledTripDetailResponse.Station(
                                            summary.destinationStationId(),
                                            summary.destinationStationCode(),
                                            summary.destinationStationName(),
                                            summary.destinationStationCity()))));
        }
    }

    @Nested
    @DisplayName("exception handling")
    class ExceptionHandling {

        @Test
        @DisplayName("propagates find enriched by id failures")
        void execute_propagatesFindEnrichedByIdFailures() {
            UUID scheduledTripId = UUID.fromString("77777777-7777-7777-7777-777777777777");
            RuntimeException failure = new RuntimeException("database unavailable");
            when(scheduledTripRepository.findEnrichedById(ScheduledTripId.of(scheduledTripId)))
                    .thenThrow(failure);

            assertThatThrownBy(
                            () -> useCase.execute(new GetScheduledTripByIdQuery(scheduledTripId)))
                    .isSameAs(failure);
        }
    }

    private ScheduledTripDetailResponse successValue(UUID scheduledTripId) {
        Result<ScheduledTripDetailResponse, ScheduledTripError> result =
                useCase.execute(new GetScheduledTripByIdQuery(scheduledTripId));
        return ((Result.Success<ScheduledTripDetailResponse, ScheduledTripError>) result).value();
    }

    private ScheduledTripEnrichedSummary enrichedSummary(UUID scheduledTripId) {
        return enrichedSummary(scheduledTripId, "TEST-SE1", "Test Express", 120);
    }

    private ScheduledTripEnrichedSummary enrichedSummary(
            UUID scheduledTripId, String trainNumber, String trainName, Integer trainTotalSeats) {
        return new ScheduledTripEnrichedSummary(
                scheduledTripId,
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
                Instant.parse("2026-05-16T08:00:00Z"),
                Instant.parse("2026-05-16T12:00:00Z"),
                "SCHEDULED",
                Instant.parse("2026-05-15T08:00:00Z"),
                240,
                80,
                trainNumber,
                trainName,
                trainTotalSeats,
                UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"),
                "TESTHN",
                "Ha Noi",
                "Ha Noi",
                UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"),
                "TESTDN",
                "Da Nang",
                "Da Nang",
                450000,
                "VND");
    }
}

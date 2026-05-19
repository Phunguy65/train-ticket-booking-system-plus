package io.github.phunguy65.ttbs.backend.station.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.station.application.query.GetStationByIdQuery;
import io.github.phunguy65.ttbs.backend.station.application.response.StationResponse;
import io.github.phunguy65.ttbs.backend.station.domain.error.StationError;
import io.github.phunguy65.ttbs.backend.station.domain.model.StationId;
import io.github.phunguy65.ttbs.backend.station.domain.projection.StationSummary;
import io.github.phunguy65.ttbs.backend.station.domain.repository.StationRepository;
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
@DisplayName("GetStationByIdUseCase")
class GetStationByIdUseCaseTest {

    @Mock
    private StationRepository stationRepository;

    @InjectMocks
    private GetStationByIdUseCase useCase;

    @Nested
    @DisplayName("happy path")
    class HappyPath {

        @Test
        @DisplayName("returns station response when found")
        void execute_returnsStationResponseWhenFound() {
            UUID stationId = UUID.fromString("11111111-1111-1111-1111-111111111111");
            StationSummary summary = stationSummary(stationId);
            when(stationRepository.findSummaryById(StationId.of(stationId)))
                    .thenReturn(Optional.of(summary));

            Result<StationResponse, StationError> result =
                    useCase.execute(new GetStationByIdQuery(stationId));

            assertThat(result.isSuccess()).isTrue();
            StationResponse response =
                    ((Result.Success<StationResponse, StationError>) result).value();
            assertThat(response.id()).isEqualTo(summary.id());
            assertThat(response.code()).isEqualTo(summary.code());
            assertThat(response.name()).isEqualTo(summary.name());
            assertThat(response.city()).isEqualTo(summary.city());
            assertThat(response.createdAt()).isEqualTo(summary.createdAt());
        }
    }

    @Nested
    @DisplayName("failure path")
    class FailurePath {

        @Test
        @DisplayName("returns station not found when repository returns empty")
        void execute_returnsStationNotFoundWhenRepositoryReturnsEmpty() {
            UUID stationId = UUID.fromString("22222222-2222-2222-2222-222222222222");
            when(stationRepository.findSummaryById(StationId.of(stationId)))
                    .thenReturn(Optional.empty());

            Result<StationResponse, StationError> result =
                    useCase.execute(new GetStationByIdQuery(stationId));

            assertThat(result.isFailure()).isTrue();
            StationError error = ((Result.Failure<StationResponse, StationError>) result).error();
            assertThat(error).isInstanceOf(StationError.StationNotFound.class);
        }
    }

    @Nested
    @DisplayName("internal behavior")
    class InternalBehavior {

        @Test
        @DisplayName("calls find summary by id with exact station id")
        void execute_callsFindSummaryByIdWithExactStationId() {
            UUID stationId = UUID.fromString("33333333-3333-3333-3333-333333333333");
            when(stationRepository.findSummaryById(StationId.of(stationId)))
                    .thenReturn(Optional.of(stationSummary(stationId)));

            useCase.execute(new GetStationByIdQuery(stationId));

            verify(stationRepository).findSummaryById(StationId.of(stationId));
        }

        @Test
        @DisplayName("maps all fields from summary to response")
        void execute_mapsAllFieldsFromSummaryToResponse() {
            UUID stationId = UUID.fromString("44444444-4444-4444-4444-444444444444");
            StationSummary summary = new StationSummary(
                    stationId,
                    "TESTDN",
                    "Da Nang",
                    "Da Nang",
                    Instant.parse("2026-05-16T08:00:00Z"));
            when(stationRepository.findSummaryById(StationId.of(stationId)))
                    .thenReturn(Optional.of(summary));

            Result<StationResponse, StationError> result =
                    useCase.execute(new GetStationByIdQuery(stationId));

            StationResponse response =
                    ((Result.Success<StationResponse, StationError>) result).value();
            assertThat(response)
                    .usingRecursiveComparison()
                    .isEqualTo(new StationResponse(
                            summary.id(),
                            summary.code(),
                            summary.name(),
                            summary.city(),
                            summary.createdAt()));
        }
    }

    @Nested
    @DisplayName("exception handling")
    class ExceptionHandling {

        @Test
        @DisplayName("propagates find summary by id failures")
        void execute_propagatesFindSummaryByIdFailures() {
            UUID stationId = UUID.fromString("55555555-5555-5555-5555-555555555555");
            RuntimeException failure = new RuntimeException("database unavailable");
            when(stationRepository.findSummaryById(StationId.of(stationId))).thenThrow(failure);

            assertThatThrownBy(() -> useCase.execute(new GetStationByIdQuery(stationId)))
                    .isSameAs(failure);
        }
    }

    private StationSummary stationSummary(UUID stationId) {
        return new StationSummary(
                stationId, "TESTHN", "Ha Noi", "Ha Noi", Instant.parse("2026-05-16T07:00:00Z"));
    }
}

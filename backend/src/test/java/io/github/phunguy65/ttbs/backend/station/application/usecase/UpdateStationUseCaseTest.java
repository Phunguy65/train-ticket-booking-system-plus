package io.github.phunguy65.ttbs.backend.station.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.station.application.command.UpdateStationCommand;
import io.github.phunguy65.ttbs.backend.station.application.response.StationResponse;
import io.github.phunguy65.ttbs.backend.station.domain.error.StationError;
import io.github.phunguy65.ttbs.backend.station.domain.model.Station;
import io.github.phunguy65.ttbs.backend.station.domain.model.StationId;
import io.github.phunguy65.ttbs.backend.station.domain.repository.StationRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.jackson.nullable.JsonNullable;

@ExtendWith(MockitoExtension.class)
class UpdateStationUseCaseTest {

    @Mock
    private StationRepository stationRepository;

    private UpdateStationUseCase useCase;

    private static final StationId STATION_ID = StationId.of(UUID.randomUUID());

    @BeforeEach
    void setUp() {
        useCase = new UpdateStationUseCase(stationRepository);
    }

    private Station makeStation() {
        return Station.reconstitute(
                STATION_ID, "HN", "Hanoi Station", "Hanoi", Instant.now(), null);
    }

    @Test
    void execute_updateName_shouldUpdateOnlyName() {
        Station existing = makeStation();
        when(stationRepository.findById(STATION_ID)).thenReturn(Optional.of(existing));
        when(stationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateStationCommand command = new UpdateStationCommand(
                STATION_ID,
                JsonNullable.undefined(),
                JsonNullable.of("Hanoi Central"),
                JsonNullable.undefined());

        Result<StationResponse, StationError> result = useCase.execute(command);

        assertThat(result.isSuccess()).isTrue();
        StationResponse dto = ((Result.Success<StationResponse, StationError>) result).value();
        assertThat(dto.name()).isEqualTo("Hanoi Central");
        assertThat(dto.code()).isEqualTo("HN");
        assertThat(dto.city()).isEqualTo("Hanoi");
    }

    @Test
    void execute_stationNotFound_shouldReturnStationNotFound() {
        when(stationRepository.findById(STATION_ID)).thenReturn(Optional.empty());

        UpdateStationCommand command = new UpdateStationCommand(
                STATION_ID,
                JsonNullable.of("SGN"),
                JsonNullable.undefined(),
                JsonNullable.undefined());

        Result<StationResponse, StationError> result = useCase.execute(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(((Result.Failure<StationResponse, StationError>) result).error())
                .isInstanceOf(StationError.StationNotFound.class);
        verify(stationRepository, never()).save(any());
    }

    @Test
    void execute_codeConflict_shouldReturnStationCodeAlreadyExists() {
        Station existing = makeStation();
        when(stationRepository.findById(STATION_ID)).thenReturn(Optional.of(existing));
        when(stationRepository.existsByCode("SGN")).thenReturn(true);

        UpdateStationCommand command = new UpdateStationCommand(
                STATION_ID,
                JsonNullable.of("SGN"),
                JsonNullable.undefined(),
                JsonNullable.undefined());

        Result<StationResponse, StationError> result = useCase.execute(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(((Result.Failure<StationResponse, StationError>) result).error())
                .isInstanceOf(StationError.StationCodeAlreadyExists.class);
        verify(stationRepository, never()).save(any());
    }

    @Test
    void execute_sameCode_shouldNotCheckConflict() {
        Station existing = makeStation();
        when(stationRepository.findById(STATION_ID)).thenReturn(Optional.of(existing));
        when(stationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateStationCommand command = new UpdateStationCommand(
                STATION_ID,
                JsonNullable.of("HN"),
                JsonNullable.undefined(),
                JsonNullable.undefined());

        Result<StationResponse, StationError> result = useCase.execute(command);

        assertThat(result.isSuccess()).isTrue();
        verify(stationRepository, never()).existsByCode(anyString());
    }
}

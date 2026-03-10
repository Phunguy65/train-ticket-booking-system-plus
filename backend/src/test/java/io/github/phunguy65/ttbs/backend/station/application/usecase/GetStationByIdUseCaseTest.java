package io.github.phunguy65.ttbs.backend.station.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.station.application.dto.StationDto;
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

@ExtendWith(MockitoExtension.class)
class GetStationByIdUseCaseTest {

    @Mock
    private StationRepository stationRepository;

    private GetStationByIdUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetStationByIdUseCase(stationRepository);
    }

    @Test
    void execute_stationFound_shouldReturnDto() {
        StationId id = StationId.of(UUID.randomUUID());
        Station station =
                Station.reconstitute(id, "HN", "Hanoi Station", "Hanoi", Instant.now(), null);
        when(stationRepository.findById(id)).thenReturn(Optional.of(station));

        Result<StationDto, StationError> result = useCase.execute(id);

        assertThat(result.isSuccess()).isTrue();
        StationDto dto = ((Result.Success<StationDto, StationError>) result).value();
        assertThat(dto.code()).isEqualTo("HN");
        assertThat(dto.name()).isEqualTo("Hanoi Station");
        assertThat(dto.city()).isEqualTo("Hanoi");
    }

    @Test
    void execute_stationNotFound_shouldReturnStationNotFoundError() {
        StationId id = StationId.of(UUID.randomUUID());
        when(stationRepository.findById(id)).thenReturn(Optional.empty());

        Result<StationDto, StationError> result = useCase.execute(id);

        assertThat(result.isFailure()).isTrue();
        StationError error = ((Result.Failure<StationDto, StationError>) result).error();
        assertThat(error).isInstanceOf(StationError.StationNotFound.class);
    }
}

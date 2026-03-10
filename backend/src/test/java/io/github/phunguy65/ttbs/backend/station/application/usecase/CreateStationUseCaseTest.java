package io.github.phunguy65.ttbs.backend.station.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.station.application.command.CreateStationCommand;
import io.github.phunguy65.ttbs.backend.station.application.dto.StationDto;
import io.github.phunguy65.ttbs.backend.station.domain.error.StationError;
import io.github.phunguy65.ttbs.backend.station.domain.event.StationCreated;
import io.github.phunguy65.ttbs.backend.station.domain.model.Station;
import io.github.phunguy65.ttbs.backend.station.domain.repository.StationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class CreateStationUseCaseTest {

    @Mock
    private StationRepository stationRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private CreateStationUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateStationUseCase(stationRepository, eventPublisher);
    }

    @Test
    void execute_success_shouldSaveStationAndReturnDto() {
        CreateStationCommand command = new CreateStationCommand("HN", "Hanoi Station", "Hanoi");
        when(stationRepository.existsByCode("HN")).thenReturn(false);
        when(stationRepository.save(any(Station.class))).thenAnswer(inv -> inv.getArgument(0));

        Result<StationDto, StationError> result = useCase.execute(command);

        assertThat(result.isSuccess()).isTrue();
        StationDto dto = ((Result.Success<StationDto, StationError>) result).value();
        assertThat(dto.code()).isEqualTo("HN");
        assertThat(dto.name()).isEqualTo("Hanoi Station");
        assertThat(dto.city()).isEqualTo("Hanoi");
        verify(stationRepository).save(any(Station.class));
    }

    @Test
    void execute_duplicateCode_shouldReturnStationCodeAlreadyExistsError() {
        CreateStationCommand command = new CreateStationCommand("HN", "Duplicate", "Hanoi");
        when(stationRepository.existsByCode("HN")).thenReturn(true);

        Result<StationDto, StationError> result = useCase.execute(command);

        assertThat(result.isFailure()).isTrue();
        StationError error = ((Result.Failure<StationDto, StationError>) result).error();
        assertThat(error).isInstanceOf(StationError.StationCodeAlreadyExists.class);
        assertThat(((StationError.StationCodeAlreadyExists) error).code()).isEqualTo("HN");
        verify(stationRepository, never()).save(any());
    }

    @Test
    void execute_success_shouldPublishStationCreatedEvent() {
        CreateStationCommand command =
                new CreateStationCommand("SGN", "Saigon Station", "Ho Chi Minh City");
        when(stationRepository.existsByCode("SGN")).thenReturn(false);
        when(stationRepository.save(any(Station.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(command);

        verify(eventPublisher, atLeastOnce()).publishEvent(any(StationCreated.class));
    }
}

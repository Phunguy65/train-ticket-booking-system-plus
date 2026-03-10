package io.github.phunguy65.ttbs.backend.train.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.command.CreateTrainCommand;
import io.github.phunguy65.ttbs.backend.train.application.dto.TrainDto;
import io.github.phunguy65.ttbs.backend.train.domain.error.TrainError;
import io.github.phunguy65.ttbs.backend.train.domain.event.TrainCreated;
import io.github.phunguy65.ttbs.backend.train.domain.model.Train;
import io.github.phunguy65.ttbs.backend.train.domain.repository.TrainRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class CreateTrainUseCaseTest {

    @Mock
    private TrainRepository trainRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private CreateTrainUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateTrainUseCase(trainRepository, eventPublisher);
    }

    @Test
    void execute_success_shouldSaveTrainAndReturnDto() {
        CreateTrainCommand command = new CreateTrainCommand("SE001", "Reunification Express", 250);
        when(trainRepository.existsByTrainNumber("SE001")).thenReturn(false);
        when(trainRepository.save(any(Train.class))).thenAnswer(inv -> inv.getArgument(0));

        Result<TrainDto, TrainError> result = useCase.execute(command);

        assertThat(result.isSuccess()).isTrue();
        TrainDto dto = ((Result.Success<TrainDto, TrainError>) result).value();
        assertThat(dto.trainNumber()).isEqualTo("SE001");
        assertThat(dto.name()).isEqualTo("Reunification Express");
        assertThat(dto.totalSeats()).isEqualTo(250);
        verify(trainRepository).save(any(Train.class));
    }

    @Test
    void execute_duplicateTrainNumber_shouldReturnTrainNumberAlreadyExistsError() {
        CreateTrainCommand command = new CreateTrainCommand("SE001", "Duplicate Train", 100);
        when(trainRepository.existsByTrainNumber("SE001")).thenReturn(true);

        Result<TrainDto, TrainError> result = useCase.execute(command);

        assertThat(result.isFailure()).isTrue();
        TrainError error = ((Result.Failure<TrainDto, TrainError>) result).error();
        assertThat(error).isInstanceOf(TrainError.TrainNumberAlreadyExists.class);
        assertThat(((TrainError.TrainNumberAlreadyExists) error).trainNumber()).isEqualTo("SE001");
        verify(trainRepository, never()).save(any());
    }

    @Test
    void execute_success_shouldPublishTrainCreatedEvent() {
        CreateTrainCommand command = new CreateTrainCommand("SE002", "Another Train", 150);
        when(trainRepository.existsByTrainNumber("SE002")).thenReturn(false);
        when(trainRepository.save(any(Train.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(command);

        verify(eventPublisher, atLeastOnce()).publishEvent(any(TrainCreated.class));
    }
}

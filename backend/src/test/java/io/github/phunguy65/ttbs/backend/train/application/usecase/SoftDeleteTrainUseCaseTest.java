package io.github.phunguy65.ttbs.backend.train.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.command.SoftDeleteTrainCommand;
import io.github.phunguy65.ttbs.backend.train.domain.error.TrainError;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.TrainRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SoftDeleteTrainUseCaseTest {

    @Mock
    private TrainRepository trainRepository;

    @Mock
    private TrainCascadeSoftDeleteService trainCascadeSoftDeleteService;

    @InjectMocks
    private SoftDeleteTrainUseCase useCase;

    @Test
    void executeReturnsNotFoundWhenTrainDoesNotExist() {
        TrainId trainId = TrainId.of(UUID.randomUUID());
        when(trainRepository.existsById(trainId)).thenReturn(false);

        Result<Void, TrainError> result = useCase.execute(new SoftDeleteTrainCommand(trainId));

        assertThat(result).isInstanceOf(Result.Failure.class);
        assertThat(((Result.Failure<Void, TrainError>) result).error())
                .isInstanceOf(TrainError.TrainNotFound.class);
        verify(trainCascadeSoftDeleteService, never()).execute(eq(List.of(trainId)), any());
    }

    @Test
    void executeRunsCascadeDeleteWhenTrainExists() {
        TrainId trainId = TrainId.of(UUID.randomUUID());
        when(trainRepository.existsById(trainId)).thenReturn(true);

        Result<Void, TrainError> result = useCase.execute(new SoftDeleteTrainCommand(trainId));

        assertThat(result).isInstanceOf(Result.Success.class);
        verify(trainCascadeSoftDeleteService).execute(eq(List.of(trainId)), any());
    }

    @Test
    void executePropagatesExceptionFromCascadeService() {
        TrainId trainId = TrainId.of(UUID.randomUUID());
        when(trainRepository.existsById(trainId)).thenReturn(true);
        doThrow(new RuntimeException("DB error"))
                .when(trainCascadeSoftDeleteService)
                .execute(eq(List.of(trainId)), any());

        assertThatThrownBy(() -> useCase.execute(new SoftDeleteTrainCommand(trainId)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB error");
    }
}

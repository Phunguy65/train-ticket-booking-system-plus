package io.github.phunguy65.ttbs.backend.train.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.command.BulkSoftDeleteTrainsCommand;
import io.github.phunguy65.ttbs.backend.train.domain.error.TrainError;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BulkSoftDeleteTrainsUseCaseTest {

    @Mock
    private TrainCascadeSoftDeleteService trainCascadeSoftDeleteService;

    @InjectMocks
    private BulkSoftDeleteTrainsUseCase useCase;

    @Test
    void executeDelegatesEmptyTrainIdsAndReturnsZero() {
        BulkSoftDeleteTrainsCommand command = new BulkSoftDeleteTrainsCommand(List.of());
        when(trainCascadeSoftDeleteService.execute(eq(List.of()), any())).thenReturn(0);

        Result<Integer, TrainError> result = useCase.execute(command);

        assertThat(result).isInstanceOf(Result.Success.class);
        assertThat(((Result.Success<Integer, TrainError>) result).value()).isZero();
        verify(trainCascadeSoftDeleteService).execute(eq(List.of()), any());
    }

    @Test
    void executeDelegatesToCascadeServiceAndReturnsAffectedCount() {
        TrainId trainId = TrainId.of(UUID.randomUUID());
        BulkSoftDeleteTrainsCommand command = new BulkSoftDeleteTrainsCommand(List.of(trainId));
        when(trainCascadeSoftDeleteService.execute(eq(List.of(trainId)), any())).thenReturn(1);

        Result<Integer, TrainError> result = useCase.execute(command);

        assertThat(result).isInstanceOf(Result.Success.class);
        assertThat(((Result.Success<Integer, TrainError>) result).value()).isEqualTo(1);
        verify(trainCascadeSoftDeleteService).execute(eq(List.of(trainId)), any());
    }

    @Test
    void executeReturnsAffectedCountForMultipleTrains() {
        List<TrainId> trainIds =
                List.of(TrainId.of(UUID.randomUUID()), TrainId.of(UUID.randomUUID()));
        BulkSoftDeleteTrainsCommand command = new BulkSoftDeleteTrainsCommand(trainIds);
        when(trainCascadeSoftDeleteService.execute(eq(trainIds), any())).thenReturn(2);

        Result<Integer, TrainError> result = useCase.execute(command);

        assertThat(result).isInstanceOf(Result.Success.class);
        assertThat(((Result.Success<Integer, TrainError>) result).value()).isEqualTo(2);
        verify(trainCascadeSoftDeleteService).execute(eq(trainIds), any());
    }
}

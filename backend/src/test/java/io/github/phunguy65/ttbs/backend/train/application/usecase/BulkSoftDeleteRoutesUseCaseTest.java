package io.github.phunguy65.ttbs.backend.train.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.command.BulkSoftDeleteRoutesCommand;
import io.github.phunguy65.ttbs.backend.train.domain.errors.RouteError;
import io.github.phunguy65.ttbs.backend.train.domain.event.RouteDeleted;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class BulkSoftDeleteRoutesUseCaseTest {

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private BulkSoftDeleteRoutesUseCase useCase;

    private static final RouteId ROUTE_ID_1 = RouteId.of(UUID.randomUUID());
    private static final RouteId ROUTE_ID_2 = RouteId.of(UUID.randomUUID());
    private static final RouteId ROUTE_ID_3 = RouteId.of(UUID.randomUUID());

    @BeforeEach
    void setUp() {
        useCase = new BulkSoftDeleteRoutesUseCase(routeRepository, eventPublisher);
    }

    @Test
    void execute_allValidIds_shouldSoftDeleteAndPublishEvents() {
        List<RouteId> ids = List.of(ROUTE_ID_1, ROUTE_ID_2);
        when(routeRepository.existsById(ROUTE_ID_1)).thenReturn(true);
        when(routeRepository.existsById(ROUTE_ID_2)).thenReturn(true);
        when(routeRepository.softDeleteByIds(any(), any())).thenReturn(2);

        Result<Integer, RouteError> result = useCase.execute(new BulkSoftDeleteRoutesCommand(ids));

        assertThat(result.isSuccess()).isTrue();
        assertThat(((Result.Success<Integer, RouteError>) result).value()).isEqualTo(2);

        // verify 2 RouteDeleted events were published
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getAllValues()).allMatch(e -> e instanceof RouteDeleted);
    }

    @Test
    void execute_oneInvalidId_shouldFailAllAndPublishNoEvents() {
        List<RouteId> ids = List.of(ROUTE_ID_1, ROUTE_ID_2, ROUTE_ID_3);
        when(routeRepository.existsById(ROUTE_ID_1)).thenReturn(true);
        when(routeRepository.existsById(ROUTE_ID_2)).thenReturn(false); // missing
        when(routeRepository.existsById(ROUTE_ID_3)).thenReturn(false); // missing

        Result<Integer, RouteError> result = useCase.execute(new BulkSoftDeleteRoutesCommand(ids));

        assertThat(result.isSuccess()).isFalse();
        RouteError error = ((Result.Failure<Integer, RouteError>) result).error();
        assertThat(error).isInstanceOf(RouteError.RoutesNotFound.class);
        RouteError.RoutesNotFound notFound = (RouteError.RoutesNotFound) error;
        assertThat(notFound.invalidIds())
                .containsExactlyInAnyOrder(ROUTE_ID_2.value(), ROUTE_ID_3.value());

        verify(routeRepository, never()).softDeleteByIds(any(), any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void execute_publishesCorrectNumberOfEvents() {
        List<RouteId> ids = List.of(ROUTE_ID_1, ROUTE_ID_2, ROUTE_ID_3);
        when(routeRepository.existsById(any())).thenReturn(true);
        when(routeRepository.softDeleteByIds(any(), any())).thenReturn(3);

        useCase.execute(new BulkSoftDeleteRoutesCommand(ids));

        verify(eventPublisher, times(3)).publishEvent(any(RouteDeleted.class));
    }
}

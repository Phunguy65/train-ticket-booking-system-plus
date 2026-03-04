package io.github.phunguy65.ttbs.backend.train.application.listener;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.train.domain.event.CoachesDeleted;
import io.github.phunguy65.ttbs.backend.train.domain.event.TrainsDeleted;
import io.github.phunguy65.ttbs.backend.train.domain.model.CoachId;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.CoachRepository;
import java.time.Instant;
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
class CascadeOnTrainsDeletedListenerTest {

    @Mock
    private CoachRepository coachRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private CascadeOnTrainsDeletedListener listener;

    @BeforeEach
    void setUp() {
        listener = new CascadeOnTrainsDeletedListener(coachRepository, eventPublisher);
    }

    @Test
    void onTrainsDeleted_withActiveCoaches_shouldSoftDeleteAndPublishCoachesDeleted() {
        TrainId trainId = TrainId.of(UUID.randomUUID());
        CoachId coachId1 = CoachId.of(UUID.randomUUID());
        CoachId coachId2 = CoachId.of(UUID.randomUUID());
        List<CoachId> coachIds = List.of(coachId1, coachId2);

        TrainsDeleted event = TrainsDeleted.of(List.of(trainId), Instant.now());
        when(coachRepository.findActiveIdsByTrainIds(event.trainIds())).thenReturn(coachIds);

        listener.onTrainsDeleted(event);

        verify(coachRepository).softDeleteByIds(eq(coachIds), any(Instant.class));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(CoachesDeleted.class);
        CoachesDeleted published = (CoachesDeleted) captor.getValue();
        assertThat(published.coachIds()).containsExactlyInAnyOrderElementsOf(coachIds);
    }

    @Test
    void onTrainsDeleted_withNoActiveCoaches_shouldNotDeleteOrPublish() {
        TrainId trainId = TrainId.of(UUID.randomUUID());
        TrainsDeleted event = TrainsDeleted.of(List.of(trainId), Instant.now());
        when(coachRepository.findActiveIdsByTrainIds(any())).thenReturn(List.of());

        listener.onTrainsDeleted(event);

        verify(coachRepository, never()).softDeleteByIds(any(), any());
        verifyNoInteractions(eventPublisher);
    }
}

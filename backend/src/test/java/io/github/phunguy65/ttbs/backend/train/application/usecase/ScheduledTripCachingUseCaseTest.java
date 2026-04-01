package io.github.phunguy65.ttbs.backend.train.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.cache.ValkeyCacheConfig;
import io.github.phunguy65.ttbs.backend.train.application.query.GetCoachSeatMapQuery;
import io.github.phunguy65.ttbs.backend.train.application.query.GetScheduledTripByIdQuery;
import io.github.phunguy65.ttbs.backend.train.application.query.GetScheduledTripsQuery;
import io.github.phunguy65.ttbs.backend.train.domain.model.CoachId;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteSeatAvailabilityStatus;
import io.github.phunguy65.ttbs.backend.train.domain.projection.CoachSeatMapCoachSummary;
import io.github.phunguy65.ttbs.backend.train.domain.projection.CoachSeatMapSeatSummary;
import io.github.phunguy65.ttbs.backend.train.domain.projection.ScheduledTripEnrichedSummary;
import io.github.phunguy65.ttbs.backend.train.domain.projection.ScheduledTripSummary;
import io.github.phunguy65.ttbs.backend.train.domain.repository.ScheduledTripRepository;
import io.github.phunguy65.ttbs.backend.train.domain.repository.ScheduledTripSeatMapRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(ScheduledTripCachingUseCaseTest.TestConfig.class)
class ScheduledTripCachingUseCaseTest {

    private static final UUID SCHEDULED_TRIP_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ROUTE_TEMPLATE_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID TRAIN_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID COACH_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID SEAT_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    @Configuration
    @EnableCaching
    static class TestConfig {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(
                    ValkeyCacheConfig.SCHEDULED_TRIP_LIST_CACHE,
                    ValkeyCacheConfig.SCHEDULED_TRIP_BY_ID_CACHE,
                    ValkeyCacheConfig.COACH_SEAT_MAP_CACHE);
        }

        @Bean
        ScheduledTripRepository scheduledTripRepository() {
            return mock(ScheduledTripRepository.class);
        }

        @Bean
        ScheduledTripSeatMapRepository scheduledTripSeatMapRepository() {
            return mock(ScheduledTripSeatMapRepository.class);
        }

        @Bean
        GetScheduledTripsUseCase getScheduledTripsUseCase(
                ScheduledTripRepository scheduledTripRepository) {
            return new GetScheduledTripsUseCase(scheduledTripRepository);
        }

        @Bean
        GetScheduledTripByIdUseCase getScheduledTripByIdUseCase(
                ScheduledTripRepository scheduledTripRepository) {
            return new GetScheduledTripByIdUseCase(scheduledTripRepository);
        }

        @Bean
        GetCoachSeatMapByScheduledTripUseCase getCoachSeatMapByScheduledTripUseCase(
                ScheduledTripRepository scheduledTripRepository,
                ScheduledTripSeatMapRepository scheduledTripSeatMapRepository) {
            return new GetCoachSeatMapByScheduledTripUseCase(
                    scheduledTripRepository, scheduledTripSeatMapRepository);
        }
    }

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private ScheduledTripRepository scheduledTripRepository;

    @Autowired
    private ScheduledTripSeatMapRepository scheduledTripSeatMapRepository;

    @Autowired
    private GetScheduledTripsUseCase getScheduledTripsUseCase;

    @Autowired
    private GetScheduledTripByIdUseCase getScheduledTripByIdUseCase;

    @Autowired
    private GetCoachSeatMapByScheduledTripUseCase getCoachSeatMapByScheduledTripUseCase;

    @AfterEach
    void tearDown() {
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
        reset(scheduledTripRepository, scheduledTripSeatMapRepository);
    }

    @Test
    void listUseCaseCachesByPageAndSize() {
        GetScheduledTripsQuery query = new GetScheduledTripsQuery(0, 20);
        PageResponse<ScheduledTripSummary> page = PageResponse.of(
                List.of(new ScheduledTripSummary(
                        SCHEDULED_TRIP_ID,
                        ROUTE_TEMPLATE_ID,
                        TRAIN_ID,
                        Instant.parse("2026-04-01T00:00:00Z"),
                        Instant.parse("2026-04-01T02:00:00Z"),
                        "SCHEDULED",
                        Instant.parse("2026-03-01T00:00:00Z"))),
                0,
                20,
                false,
                1);
        when(scheduledTripRepository.findAllSummaries(eq(0), eq(20), anyList())).thenReturn(page);

        var first = getScheduledTripsUseCase.execute(query);
        var second = getScheduledTripsUseCase.execute(query);

        assertThat(second).isEqualTo(first);
        verify(scheduledTripRepository, times(1)).findAllSummaries(eq(0), eq(20), anyList());
    }

    @Test
    void listUseCaseSeparatesCacheEntriesByPageAndSize() {
        GetScheduledTripsQuery firstQuery = new GetScheduledTripsQuery(0, 20);
        GetScheduledTripsQuery secondQuery = new GetScheduledTripsQuery(1, 20);
        PageResponse<ScheduledTripSummary> firstPage = PageResponse.of(
                List.of(new ScheduledTripSummary(
                        SCHEDULED_TRIP_ID,
                        ROUTE_TEMPLATE_ID,
                        TRAIN_ID,
                        Instant.parse("2026-04-01T00:00:00Z"),
                        Instant.parse("2026-04-01T02:00:00Z"),
                        "SCHEDULED",
                        Instant.parse("2026-03-01T00:00:00Z"))),
                0,
                20,
                true,
                2);
        PageResponse<ScheduledTripSummary> secondPage = PageResponse.of(
                List.of(new ScheduledTripSummary(
                        UUID.fromString("88888888-8888-8888-8888-888888888888"),
                        ROUTE_TEMPLATE_ID,
                        TRAIN_ID,
                        Instant.parse("2026-04-02T00:00:00Z"),
                        Instant.parse("2026-04-02T02:00:00Z"),
                        "SCHEDULED",
                        Instant.parse("2026-03-02T00:00:00Z"))),
                1,
                20,
                false,
                2);
        when(scheduledTripRepository.findAllSummaries(eq(0), eq(20), anyList()))
                .thenReturn(firstPage);
        when(scheduledTripRepository.findAllSummaries(eq(1), eq(20), anyList()))
                .thenReturn(secondPage);

        var first = getScheduledTripsUseCase.execute(firstQuery);
        var second = getScheduledTripsUseCase.execute(secondQuery);
        var firstAgain = getScheduledTripsUseCase.execute(firstQuery);
        var secondAgain = getScheduledTripsUseCase.execute(secondQuery);

        assertThat(firstAgain).isEqualTo(first);
        assertThat(secondAgain).isEqualTo(second);
        verify(scheduledTripRepository, times(1)).findAllSummaries(eq(0), eq(20), anyList());
        verify(scheduledTripRepository, times(1)).findAllSummaries(eq(1), eq(20), anyList());
    }

    @Test
    void byIdUseCaseCachesSuccessfulResultsOnly() {
        GetScheduledTripByIdQuery query = new GetScheduledTripByIdQuery(SCHEDULED_TRIP_ID);
        when(scheduledTripRepository.findEnrichedById(any()))
                .thenReturn(Optional.of(enrichedSummary()));

        var first = getScheduledTripByIdUseCase.execute(query);
        var second = getScheduledTripByIdUseCase.execute(query);

        assertThat(second).isEqualTo(first);
        verify(scheduledTripRepository, times(1)).findEnrichedById(any());
    }

    @Test
    void byIdUseCaseDoesNotCacheFailures() {
        GetScheduledTripByIdQuery query = new GetScheduledTripByIdQuery(SCHEDULED_TRIP_ID);
        when(scheduledTripRepository.findEnrichedById(any())).thenReturn(Optional.empty());

        var first = getScheduledTripByIdUseCase.execute(query);
        var second = getScheduledTripByIdUseCase.execute(query);

        assertThat(first).isEqualTo(second);
        assertThat(first.isFailure()).isTrue();
        verify(scheduledTripRepository, times(2)).findEnrichedById(any());
    }

    @Test
    void coachSeatMapCachesNonEmptySuccessfulResultsOnly() {
        GetCoachSeatMapQuery query = new GetCoachSeatMapQuery(0, 20, SCHEDULED_TRIP_ID);
        when(scheduledTripSeatMapRepository.findCoachSummariesByScheduledTripId(
                        anyInt(), anyInt(), any()))
                .thenReturn(PageResponse.of(
                        List.of(new CoachSeatMapCoachSummary(COACH_ID, 1, 40)), 0, 20, false, 1));
        when(scheduledTripSeatMapRepository.findSeatSummariesByScheduledTripIdAndCoachIds(
                        any(), anyList()))
                .thenReturn(List.of(new CoachSeatMapSeatSummary(
                        SEAT_ID, COACH_ID, "A1", RouteSeatAvailabilityStatus.AVAILABLE)));

        var first = getCoachSeatMapByScheduledTripUseCase.execute(query);
        var second = getCoachSeatMapByScheduledTripUseCase.execute(query);

        assertThat(second).isEqualTo(first);
        verify(scheduledTripSeatMapRepository, times(1))
                .findCoachSummariesByScheduledTripId(anyInt(), anyInt(), any());
        verify(scheduledTripSeatMapRepository, times(1))
                .findSeatSummariesByScheduledTripIdAndCoachIds(any(), anyList());
        verifyNoInteractions(scheduledTripRepository);
    }

    @Test
    void coachSeatMapSeparatesCacheEntriesByPageAndSize() {
        GetCoachSeatMapQuery firstQuery = new GetCoachSeatMapQuery(0, 20, SCHEDULED_TRIP_ID);
        GetCoachSeatMapQuery secondQuery = new GetCoachSeatMapQuery(1, 20, SCHEDULED_TRIP_ID);
        when(scheduledTripSeatMapRepository.findCoachSummariesByScheduledTripId(
                        eq(0), eq(20), any()))
                .thenReturn(PageResponse.of(
                        List.of(new CoachSeatMapCoachSummary(COACH_ID, 1, 40)), 0, 20, true, 2));
        when(scheduledTripSeatMapRepository.findCoachSummariesByScheduledTripId(
                        eq(1), eq(20), any()))
                .thenReturn(PageResponse.of(
                        List.of(new CoachSeatMapCoachSummary(
                                UUID.fromString("99999999-9999-9999-9999-999999999999"), 2, 40)),
                        1,
                        20,
                        false,
                        2));
        when(scheduledTripSeatMapRepository.findSeatSummariesByScheduledTripIdAndCoachIds(
                        any(), anyList()))
                .thenAnswer(invocation -> {
                    List<CoachId> coachIds = invocation.getArgument(1);
                    return coachIds.stream()
                            .map(coachId -> new CoachSeatMapSeatSummary(
                                    SEAT_ID,
                                    coachId.value(),
                                    "A1",
                                    RouteSeatAvailabilityStatus.AVAILABLE))
                            .toList();
                });

        var first = getCoachSeatMapByScheduledTripUseCase.execute(firstQuery);
        var second = getCoachSeatMapByScheduledTripUseCase.execute(secondQuery);
        var firstAgain = getCoachSeatMapByScheduledTripUseCase.execute(firstQuery);
        var secondAgain = getCoachSeatMapByScheduledTripUseCase.execute(secondQuery);

        assertThat(firstAgain).isEqualTo(first);
        assertThat(secondAgain).isEqualTo(second);
        verify(scheduledTripSeatMapRepository, times(1))
                .findCoachSummariesByScheduledTripId(eq(0), eq(20), any());
        verify(scheduledTripSeatMapRepository, times(1))
                .findCoachSummariesByScheduledTripId(eq(1), eq(20), any());
        verify(scheduledTripSeatMapRepository, times(2))
                .findSeatSummariesByScheduledTripIdAndCoachIds(any(), anyList());
    }

    @Test
    void coachSeatMapDoesNotCacheEmptySuccessfulResults() {
        GetCoachSeatMapQuery query = new GetCoachSeatMapQuery(0, 20, SCHEDULED_TRIP_ID);
        when(scheduledTripSeatMapRepository.findCoachSummariesByScheduledTripId(
                        anyInt(), anyInt(), any()))
                .thenReturn(PageResponse.of(List.of(), 0, 20, false, 0));
        when(scheduledTripRepository.existsById(any())).thenReturn(true);

        Result<?, ?> first = getCoachSeatMapByScheduledTripUseCase.execute(query);
        Result<?, ?> second = getCoachSeatMapByScheduledTripUseCase.execute(query);

        assertThat(first).isEqualTo(second);
        verify(scheduledTripSeatMapRepository, times(2))
                .findCoachSummariesByScheduledTripId(anyInt(), anyInt(), any());
        verify(scheduledTripRepository, times(2)).existsById(any());
    }

    private ScheduledTripEnrichedSummary enrichedSummary() {
        return new ScheduledTripEnrichedSummary(
                SCHEDULED_TRIP_ID,
                ROUTE_TEMPLATE_ID,
                TRAIN_ID,
                Instant.parse("2026-04-01T00:00:00Z"),
                Instant.parse("2026-04-01T02:00:00Z"),
                "SCHEDULED",
                Instant.parse("2026-03-01T00:00:00Z"),
                120,
                80,
                "SE1",
                "North Express",
                80,
                UUID.fromString("66666666-6666-6666-6666-666666666666"),
                "HNI",
                "Ha Noi",
                "Ha Noi",
                UUID.fromString("77777777-7777-7777-7777-777777777777"),
                "DAD",
                "Da Nang",
                "Da Nang",
                500000,
                "VND");
    }
}

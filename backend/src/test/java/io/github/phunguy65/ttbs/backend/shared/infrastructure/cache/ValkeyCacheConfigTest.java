package io.github.phunguy65.ttbs.backend.shared.infrastructure.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.response.CoachSeatMapResponse;
import io.github.phunguy65.ttbs.backend.train.domain.error.ScheduledTripError;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class ValkeyCacheConfigTest {

    private final ValkeyCacheConfig valkeyCacheConfig = new ValkeyCacheConfig();
    private final ObjectMapper objectMapper =
            JsonMapper.builder().findAndAddModules().build();

    @Test
    void createsCacheManagerWithCoachSeatMapOverride() {
        RedisCacheManager cacheManager = valkeyCacheConfig.redisCacheManager(
                mock(RedisConnectionFactory.class), objectMapper);

        assertThat(cacheManager.getCache(ValkeyCacheConfig.SCHEDULED_TRIP_LIST_CACHE))
                .isNotNull();
        assertThat(cacheManager.getCache(ValkeyCacheConfig.SCHEDULED_TRIP_BY_ID_CACHE))
                .isNotNull();
        assertThat(cacheManager.getCache(ValkeyCacheConfig.COACH_SEAT_MAP_CACHE))
                .isNotNull();
        RedisCacheConfiguration coachSeatMapConfiguration =
                cacheManager.getCacheConfigurations().get(ValkeyCacheConfig.COACH_SEAT_MAP_CACHE);
        assertThat(coachSeatMapConfiguration).isNotNull();
        assertThat(cacheManager.getCacheConfigurations())
                .containsKey(ValkeyCacheConfig.COACH_SEAT_MAP_CACHE);
    }

    @Test
    void cacheConfigurationsUseExpectedTtls() {
        RedisCacheConfiguration scheduledTripListConfiguration =
                valkeyCacheConfig.cacheConfiguration(
                        Duration.ofHours(12),
                        valkeyCacheConfig.scheduledTripListSerializer(objectMapper));
        RedisCacheConfiguration scheduledTripByIdConfiguration =
                valkeyCacheConfig.cacheConfiguration(
                        Duration.ofHours(12),
                        valkeyCacheConfig.scheduledTripByIdSerializer(objectMapper));
        RedisCacheConfiguration coachSeatMapConfiguration = valkeyCacheConfig.cacheConfiguration(
                Duration.ofSeconds(60), valkeyCacheConfig.coachSeatMapSerializer(objectMapper));

        assertThat(scheduledTripListConfiguration.getTtlFunction().getTimeToLive("k", "v"))
                .isEqualTo(Duration.ofHours(12));
        assertThat(scheduledTripByIdConfiguration.getTtlFunction().getTimeToLive("k", "v"))
                .isEqualTo(Duration.ofHours(12));
        assertThat(coachSeatMapConfiguration.getTtlFunction().getTimeToLive("k", "v"))
                .isEqualTo(Duration.ofSeconds(60));
    }

    @Test
    void serializerRoundTripsNestedCachePayloads() {
        var serializer = valkeyCacheConfig.coachSeatMapSerializer(objectMapper);
        Result.Success<PageResponse<CoachSeatMapResponse>, ScheduledTripError> payload =
                new Result.Success<>(PageResponse.of(
                        List.of(new CoachSeatMapResponse(
                                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                                1,
                                40,
                                List.of(new CoachSeatMapResponse.Seat(
                                        UUID.fromString("22222222-2222-2222-2222-222222222222"),
                                        "A1",
                                        io.github.phunguy65.ttbs.backend.train.domain.model
                                                .RouteSeatAvailabilityStatus.AVAILABLE)))),
                        0,
                        20,
                        false,
                        1));

        Object deserialized = serializer.deserialize(serializer.serialize(payload));

        assertThat(deserialized).isEqualTo(payload);
    }
}

package io.github.phunguy65.ttbs.backend.shared.infrastructure.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

class ValkeyCacheConfigTest {

    private final ValkeyCacheConfig valkeyCacheConfig = new ValkeyCacheConfig();
    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(ValkeyCacheConfig.class);

    @Test
    void createsCacheManagerWithCoachSeatMapOverride() {
        RedisCacheManager cacheManager =
                valkeyCacheConfig.redisCacheManager(mock(RedisConnectionFactory.class));

        assertThat(cacheManager.getCache(ValkeyCacheConfig.SCHEDULED_TRIP_LIST_CACHE))
                .isNotNull();
        assertThat(cacheManager.getCache(ValkeyCacheConfig.SCHEDULED_TRIP_BY_ID_CACHE))
                .isNotNull();
        assertThat(cacheManager.getCache(ValkeyCacheConfig.COACH_SEAT_MAP_CACHE))
                .isNotNull();
        assertThat(cacheManager.getCache(ValkeyCacheConfig.SCHEDULED_TRIP_FILTER_CACHE))
                .isNotNull();
        assertThat(cacheManager.getCache(ValkeyCacheConfig.STATION_SEARCH_CACHE))
                .isNotNull();
        RedisCacheConfiguration coachSeatMapConfiguration =
                cacheManager.getCacheConfigurations().get(ValkeyCacheConfig.COACH_SEAT_MAP_CACHE);
        assertThat(coachSeatMapConfiguration).isNotNull();
        assertThat(cacheManager.getCacheConfigurations())
                .containsKeys(
                        ValkeyCacheConfig.COACH_SEAT_MAP_CACHE,
                        ValkeyCacheConfig.SCHEDULED_TRIP_FILTER_CACHE,
                        ValkeyCacheConfig.STATION_SEARCH_CACHE,
                        ValkeyCacheConfig.SCHEDULED_TRIP_LIST_CACHE,
                        ValkeyCacheConfig.SCHEDULED_TRIP_BY_ID_CACHE);
    }

    @Test
    void cacheConfigurationsUseExpectedTtls() {
        var serializer = valkeyCacheConfig.redisValueSerializer();
        RedisCacheConfiguration scheduledTripListConfiguration =
                valkeyCacheConfig.cacheConfiguration(Duration.ofHours(12), serializer);
        RedisCacheConfiguration scheduledTripByIdConfiguration =
                valkeyCacheConfig.cacheConfiguration(Duration.ofHours(12), serializer);
        RedisCacheConfiguration coachSeatMapConfiguration =
                valkeyCacheConfig.cacheConfiguration(Duration.ofSeconds(60), serializer);
        RedisCacheConfiguration scheduledTripFilterConfiguration =
                valkeyCacheConfig.cacheConfiguration(Duration.ofMinutes(5), serializer);
        RedisCacheConfiguration stationSearchConfiguration =
                valkeyCacheConfig.cacheConfiguration(Duration.ofMinutes(5), serializer);

        assertThat(scheduledTripListConfiguration.getTtlFunction().getTimeToLive("k", "v"))
                .isEqualTo(Duration.ofHours(12));
        assertThat(scheduledTripByIdConfiguration.getTtlFunction().getTimeToLive("k", "v"))
                .isEqualTo(Duration.ofHours(12));
        assertThat(coachSeatMapConfiguration.getTtlFunction().getTimeToLive("k", "v"))
                .isEqualTo(Duration.ofSeconds(60));
        assertThat(scheduledTripFilterConfiguration.getTtlFunction().getTimeToLive("k", "v"))
                .isEqualTo(Duration.ofMinutes(5));
        assertThat(stationSearchConfiguration.getTtlFunction().getTimeToLive("k", "v"))
                .isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    void cacheConfigurationUsesStringKeysJsonValuesAndDisablesNullCaching() {
        RedisCacheConfiguration configuration = valkeyCacheConfig.cacheConfiguration(
                Duration.ofMinutes(5), valkeyCacheConfig.redisValueSerializer());

        assertThat(configuration.getKeySerializationPair()).isNotNull();
        assertThat(configuration.getValueSerializationPair()).isNotNull();
        assertThat(configuration.getAllowCacheNullValues()).isFalse();
    }

    @Test
    void serializerRoundTripsNestedCachePayloads() {
        var serializer = valkeyCacheConfig.redisValueSerializer();
        Result<PageResponse<String>, String> payload =
                Result.success(PageResponse.of(List.of("SE1", "SE2"), 0, 20, true, 40));

        Object restored = serializer.deserialize(serializer.serialize(payload));

        assertThat(restored).isEqualTo(payload);
    }

    @Test
    void registersCacheManagerOnlyWhenRedisConnectionFactoryExists() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(RedisCacheManager.class));

        contextRunner
                .withBean(RedisConnectionFactory.class, () -> mock(RedisConnectionFactory.class))
                .run(context -> assertThat(context).hasSingleBean(RedisCacheManager.class));
    }
}

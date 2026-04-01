package io.github.phunguy65.ttbs.backend.shared.infrastructure.cache;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.response.CoachSeatMapResponse;
import io.github.phunguy65.ttbs.backend.train.application.response.ScheduledTripDetailResponse;
import io.github.phunguy65.ttbs.backend.train.application.response.ScheduledTripResponse;
import io.github.phunguy65.ttbs.backend.train.domain.error.ScheduledTripError;
import java.time.Duration;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

@Configuration
@ConditionalOnBean(RedisConnectionFactory.class)
public class ValkeyCacheConfig {

    public static final String SCHEDULED_TRIP_LIST_CACHE = "scheduledTripList";
    public static final String SCHEDULED_TRIP_BY_ID_CACHE = "scheduledTripById";
    public static final String COACH_SEAT_MAP_CACHE = "coachSeatMap";

    @Bean
    RedisCacheManager redisCacheManager(
            RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        RedisCacheConfiguration defaultCacheConfiguration =
                cacheConfiguration(Duration.ofHours(12), scheduledTripListSerializer(objectMapper));

        Map<String, RedisCacheConfiguration> cacheConfigurations = Map.of(
                SCHEDULED_TRIP_BY_ID_CACHE,
                cacheConfiguration(Duration.ofHours(12), scheduledTripByIdSerializer(objectMapper)),
                COACH_SEAT_MAP_CACHE,
                cacheConfiguration(Duration.ofSeconds(60), coachSeatMapSerializer(objectMapper)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultCacheConfiguration)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    RedisCacheConfiguration cacheConfiguration(
            Duration ttl, JacksonJsonRedisSerializer<?> valueSerializer) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer))
                .entryTtl(ttl);
    }

    JacksonJsonRedisSerializer<PageResponse<ScheduledTripResponse>> scheduledTripListSerializer(
            ObjectMapper objectMapper) {
        JavaType pageType = objectMapper
                .getTypeFactory()
                .constructParametricType(PageResponse.class, ScheduledTripResponse.class);
        return new JacksonJsonRedisSerializer<>(objectMapper, pageType);
    }

    JacksonJsonRedisSerializer<Result.Success<ScheduledTripDetailResponse, ScheduledTripError>>
            scheduledTripByIdSerializer(ObjectMapper objectMapper) {
        JavaType successType = objectMapper
                .getTypeFactory()
                .constructParametricType(
                        Result.Success.class,
                        ScheduledTripDetailResponse.class,
                        ScheduledTripError.class);
        return new JacksonJsonRedisSerializer<>(objectMapper, successType);
    }

    JacksonJsonRedisSerializer<
                    Result.Success<PageResponse<CoachSeatMapResponse>, ScheduledTripError>>
            coachSeatMapSerializer(ObjectMapper objectMapper) {
        JavaType pageType = objectMapper
                .getTypeFactory()
                .constructParametricType(PageResponse.class, CoachSeatMapResponse.class);
        JavaType errorType = objectMapper.getTypeFactory().constructType(ScheduledTripError.class);
        JavaType successType = objectMapper
                .getTypeFactory()
                .constructParametricType(Result.Success.class, pageType, errorType);
        return new JacksonJsonRedisSerializer<>(objectMapper, successType);
    }
}

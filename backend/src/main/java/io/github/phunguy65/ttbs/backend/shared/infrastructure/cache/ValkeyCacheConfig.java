package io.github.phunguy65.ttbs.backend.shared.infrastructure.cache;

import java.time.Duration;
import java.util.Map;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

@Configuration
public class ValkeyCacheConfig {

    public static final String SCHEDULED_TRIP_LIST_CACHE = "scheduledTripList";
    public static final String SCHEDULED_TRIP_BY_ID_CACHE = "scheduledTripById";
    public static final String COACH_SEAT_MAP_CACHE = "coachSeatMap";
    public static final String SCHEDULED_TRIP_FILTER_CACHE = "scheduledTripFilter";
    public static final String STATION_SEARCH_CACHE = "stationSearch";

    @Bean
    RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        return builder -> {
            RedisSerializer<Object> valueSerializer = redisValueSerializer();
            RedisCacheConfiguration defaultConfig =
                    cacheConfiguration(Duration.ofMinutes(5), valueSerializer);

            Map<String, RedisCacheConfiguration> cacheConfigurations = Map.of(
                    SCHEDULED_TRIP_LIST_CACHE,
                    cacheConfiguration(Duration.ofHours(12), valueSerializer),
                    SCHEDULED_TRIP_BY_ID_CACHE,
                    cacheConfiguration(Duration.ofHours(12), valueSerializer),
                    COACH_SEAT_MAP_CACHE,
                    cacheConfiguration(Duration.ofSeconds(60), valueSerializer),
                    SCHEDULED_TRIP_FILTER_CACHE,
                    cacheConfiguration(Duration.ofMinutes(5), valueSerializer),
                    STATION_SEARCH_CACHE,
                    cacheConfiguration(Duration.ofMinutes(5), valueSerializer));

            builder.cacheDefaults(defaultConfig)
                    .withInitialCacheConfigurations(cacheConfigurations);
        };
    }

    RedisSerializer<Object> redisValueSerializer() {
        return GenericJacksonJsonRedisSerializer.builder()
                .enableDefaultTyping(BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType("io.github.phunguy65.ttbs.backend")
                        .allowIfSubType("java.lang")
                        .allowIfSubType("java.math")
                        .allowIfSubType("java.time")
                        .allowIfSubType("java.util")
                        .build())
                .build();
    }

    RedisCacheConfiguration cacheConfiguration(Duration ttl, RedisSerializer<?> valueSerializer) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer))
                .entryTtl(ttl);
    }
}

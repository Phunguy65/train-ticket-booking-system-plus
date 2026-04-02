package io.github.phunguy65.ttbs.backend.shared.infrastructure.cache;

import java.time.Duration;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

@Configuration
@ConditionalOnBean(RedisConnectionFactory.class)
public class ValkeyCacheConfig {

    public static final String SCHEDULED_TRIP_LIST_CACHE = "scheduledTripList";
    public static final String SCHEDULED_TRIP_BY_ID_CACHE = "scheduledTripById";
    public static final String COACH_SEAT_MAP_CACHE = "coachSeatMap";
    public static final String SCHEDULED_TRIP_FILTER_CACHE = "scheduledTripFilter";
    public static final String STATION_SEARCH_CACHE = "stationSearch";

    @Bean
    RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        RedisSerializer<Object> valueSerializer = redisValueSerializer();
        RedisCacheConfiguration defaultCacheConfiguration =
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

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultCacheConfiguration)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
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

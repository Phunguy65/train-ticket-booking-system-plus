package io.github.phunguy65.ttbs.backend.shared.infrastructure.cache;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.time.Duration;
import java.util.Map;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.KotlinDetector;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.ClassUtils;
import tools.jackson.core.TreeNode;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;
import tools.jackson.databind.jsontype.impl.DefaultTypeResolverBuilder;

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
        PolymorphicTypeValidator polymorphicTypeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("io.github.phunguy65.ttbs.backend")
                .allowIfSubType("java.lang")
                .allowIfSubType("java.math")
                .allowIfSubType("java.time")
                .allowIfSubType("java.util")
                .build();
        ObjectMapper objectMapper = JsonMapper.builder()
                .setDefaultTyping(new CollectionAwareTypeResolverBuilder(polymorphicTypeValidator))
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY, false)
                .build();

        return new GenericJacksonJsonRedisSerializer(objectMapper);
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

    private static final class CollectionAwareTypeResolverBuilder
            extends DefaultTypeResolverBuilder {

        private CollectionAwareTypeResolverBuilder(
                PolymorphicTypeValidator polymorphicTypeValidator) {
            super(
                    polymorphicTypeValidator,
                    DefaultTyping.NON_FINAL,
                    JsonTypeInfo.As.PROPERTY,
                    JsonTypeInfo.Id.CLASS,
                    "@class");
        }

        @Override
        public boolean useForType(JavaType javaType) {
            if (javaType.isJavaLangObject()) {
                return true;
            }

            JavaType type = javaType;
            while (type.isArrayType()) {
                type = type.getContentType();
            }

            if (type.isReferenceType()) {
                type = type.getReferencedType();
            }

            if (type.isCollectionLikeType() || type.isMapLikeType()) {
                return true;
            }

            Class<?> rawClass = type.getRawClass();
            if (type.isEnumType() || ClassUtils.isPrimitiveOrWrapper(rawClass)) {
                return false;
            }

            if (type.isFinal()
                    && !KotlinDetector.isKotlinType(rawClass)
                    && rawClass.getPackageName().startsWith("java")) {
                return false;
            }

            if (TreeNode.class.isAssignableFrom(rawClass)) {
                return false;
            }

            return true;
        }

        @Override
        public DefaultTypeResolverBuilder withDefaultImpl(Class<?> defaultImpl) {
            return this;
        }
    }
}

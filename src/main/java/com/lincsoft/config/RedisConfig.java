package com.lincsoft.config;

import static com.lincsoft.constant.CommonConstants.REDIS_USER_DETAILS_PREFIX;

import java.time.Duration;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

/**
 * Redis and Cache Configuration Class.
 *
 * <p>Provides Redis-based cache configuration with Spring Cache abstraction support. Since Spring
 * Data Redis 4.0, {@code GenericJackson2JsonRedisSerializer} (Jackson 2 based) is deprecated, so
 * this class uses the Jackson 3 based {@link GenericJacksonJsonRedisSerializer}.
 *
 * <p>Configures:
 *
 * <ul>
 *   <li>{@link CacheManager}: Redis-backed cache manager for {@code @Cacheable} support, with
 *       per-cache TTL configuration
 *   <li>{@link RedisTemplate}: For programmatic Redis operations (token blacklist, etc.)
 *   <li>{@link CacheErrorHandler}: Graceful degradation on Redis failures, ensuring cache errors do
 *       not block business logic
 * </ul>
 *
 * @author 林创科技
 * @since 2026-04-09
 */
@Configuration
@EnableCaching
@Slf4j
@RequiredArgsConstructor
public class RedisConfig implements CachingConfigurer {

  private final AppProperties appProperties;

  /**
   * Creates a Jackson 3 based Redis JSON serializer with polymorphic type validation.
   *
   * <p>Uses {@link BasicPolymorphicTypeValidator} to restrict deserializable types to the project
   * package and Java standard library, preventing arbitrary class instantiation attacks. Default
   * typing is enabled to embed {@code @class} type information in JSON.
   *
   * @return a configured {@link GenericJacksonJsonRedisSerializer}
   */
  private GenericJacksonJsonRedisSerializer jsonRedisSerializer() {
    // Polymorphic type validator:
    // Restricts deserializable types to project package and standard library
    // to prevent arbitrary class instantiation (deserialization attack prevention)
    BasicPolymorphicTypeValidator typeValidator =
        BasicPolymorphicTypeValidator.builder()
            .allowIfBaseType(Object.class)
            .allowIfSubTypeIsArray()
            .allowIfSubType("com.lincsoft")
            .build();

    return GenericJacksonJsonRedisSerializer.builder()
        // Enable default typing (embeds @class type info in JSON)
        .enableDefaultTyping(typeValidator)
        .build();
  }

  /**
   * Configures a Redis-backed CacheManager for Spring Cache abstraction.
   *
   * <p>Sets cache entry TTL, key/value serialization, and prefix strategy. Supports
   * {@code @Cacheable}, {@code @CacheEvict}, and {@code @CachePut} annotations.
   *
   * @param connectionFactory the Redis connection factory
   * @return the configured CacheManager bean
   */
  @Bean
  public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
    GenericJacksonJsonRedisSerializer serializer = jsonRedisSerializer();
    StringRedisSerializer stringSerializer = new StringRedisSerializer();

    // Default cache configuration (applies to all caches without a specific override)
    RedisCacheConfiguration defaultConfig =
        RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(appProperties.getCache().getDefaultTtlHours()))
            .computePrefixWith(cacheName -> cacheName + ":")
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(serializer))
            .disableCachingNullValues();

    // Per-cache TTL overrides
    RedisCacheConfiguration userDetailsConfig =
        defaultConfig.entryTtl(
            Duration.ofMinutes(appProperties.getCache().getUserDetailsTtlMinutes()));

    return RedisCacheManager.builder(connectionFactory)
        .cacheDefaults(defaultConfig)
        .withInitialCacheConfigurations(Map.of(REDIS_USER_DETAILS_PREFIX, userDetailsConfig))
        .build();
  }

  /**
   * Configures a RedisTemplate with String keys and JSON-serialized values.
   *
   * <p>Uses {@link StringRedisSerializer} for keys and hash keys to ensure human-readable Redis
   * keys. Uses {@link GenericJacksonJsonRedisSerializer} with type validation for values and hash
   * values to support secure complex object serialization.
   *
   * @param connectionFactory the Redis connection factory
   * @return the configured RedisTemplate bean
   */
  @Bean
  public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
    GenericJacksonJsonRedisSerializer serializer = jsonRedisSerializer();

    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);

    // Key serializer: String
    StringRedisSerializer stringSerializer = new StringRedisSerializer();
    template.setKeySerializer(stringSerializer);
    template.setHashKeySerializer(stringSerializer);

    // Value serializer: Jackson 3 JSON (with type validation)
    template.setValueSerializer(serializer);
    template.setHashValueSerializer(serializer);

    template.afterPropertiesSet();
    return template;
  }

  /**
   * Provides a graceful-degradation cache error handler.
   *
   * <p>When Redis is unavailable or encounters errors during cache operations, this handler logs
   * the failure and allows the application to continue normally. Cache GET errors result in a cache
   * miss (method body executes), and cache PUT/EVICT errors are silently logged without affecting
   * the return value.
   *
   * @return a {@link CacheErrorHandler} that logs errors without propagating them
   */
  @Override
  public CacheErrorHandler errorHandler() {
    return new SimpleCacheErrorHandler() {
      @Override
      public void handleCacheGetError(
          @NonNull RuntimeException exception, @NonNull Cache cache, @NonNull Object key) {
        log.warn(
            "Cache GET failed [cache={}, key={}]: {}",
            cache.getName(),
            key,
            exception.getMessage());
      }

      @Override
      public void handleCachePutError(
          @NonNull RuntimeException exception,
          @NonNull Cache cache,
          @NonNull Object key,
          Object value) {
        log.warn(
            "Cache PUT failed [cache={}, key={}]: {}",
            cache.getName(),
            key,
            exception.getMessage());
      }

      @Override
      public void handleCacheEvictError(
          @NonNull RuntimeException exception, @NonNull Cache cache, @NonNull Object key) {
        log.warn(
            "Cache EVICT failed [cache={}, key={}]: {}",
            cache.getName(),
            key,
            exception.getMessage());
      }

      @Override
      public void handleCacheClearError(@NonNull RuntimeException exception, @NonNull Cache cache) {
        log.warn("Cache CLEAR failed [cache={}]: {}", cache.getName(), exception.getMessage());
      }
    };
  }
}

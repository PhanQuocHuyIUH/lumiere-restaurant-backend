package iuh.fit.se.shared.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Minimal Redis configuration for caching and distributed operations.
 * Supports:
 * - Payment/Refund idempotency (24h): idem:payment:*
 * - QR Session caching: qr:session:*           TTL = dynamic (session.expiresAt)
 * - Menu caching (30m): menu:*                 TTL = MENU_CACHE_TTL
 * - Dashboard counters: dashboard:*            TTL = end of day
 */
@Configuration
@EnableCaching
public class RedisConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisConfig.class);
    private static final Duration MENU_CACHE_TTL     = Duration.ofMinutes(30);
    private static final Duration TRENDING_CACHE_TTL = Duration.ofMinutes(5);
    private static final Duration TABLE_CACHE_TTL    = Duration.ofSeconds(30);

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        GenericJackson2JsonRedisSerializer valueSerializer = buildCacheSerializer();
        RedisSerializationContext.SerializationPair<String> keyPair =
                RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer());
        RedisSerializationContext.SerializationPair<Object> valuePair =
                RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(MENU_CACHE_TTL)
                .serializeKeysWith(keyPair)
                .serializeValuesWith(valuePair);

        Map<String, RedisCacheConfiguration> perCacheConfigs = Map.of(
                "trending", defaultConfig.entryTtl(TRENDING_CACHE_TTL),
                "tables",   defaultConfig.entryTtl(TABLE_CACHE_TTL)
        );
        RedisCacheManager redisCacheManager = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(perCacheConfigs)
                .build();
        return new SafeCacheManager(redisCacheManager);
    }

    private static GenericJackson2JsonRedisSerializer buildCacheSerializer() {
        ObjectMapper om = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        om.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        return new GenericJackson2JsonRedisSerializer(om);
    }

    @Bean
    public CacheErrorHandler cacheErrorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                LOGGER.debug("Ignoring cache get failure for {}:{}", cache.getName(), key, exception);
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                LOGGER.debug("Ignoring cache put failure for {}:{}", cache.getName(), key, exception);
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                LOGGER.debug("Ignoring cache evict failure for {}:{}", cache.getName(), key, exception);
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                LOGGER.debug("Ignoring cache clear failure for {}", cache.getName(), exception);
            }
        };
    }

    private static final class SafeCacheManager implements CacheManager {

        private final RedisCacheManager delegate;
        private final ConcurrentMap<String, Cache> cacheByName = new ConcurrentHashMap<>();

        private SafeCacheManager(RedisCacheManager delegate) {
            this.delegate = delegate;
        }

        @Override
        public Cache getCache(String name) {
            return cacheByName.computeIfAbsent(name, cacheName -> {
                Cache cache = delegate.getCache(cacheName);
                return cache == null ? null : new SafeCache(cache);
            });
        }

        @Override
        public Collection<String> getCacheNames() {
            return delegate.getCacheNames();
        }
    }

    private static final class SafeCache implements Cache {

        private final Cache delegate;

        private SafeCache(Cache delegate) {
            this.delegate = delegate;
        }

        @Override
        public String getName() {
            return delegate.getName();
        }

        @Override
        public Object getNativeCache() {
            return delegate.getNativeCache();
        }

        @Override
        public ValueWrapper get(Object key) {
            try {
                return delegate.get(key);
            } catch (RuntimeException ex) {
                return null;
            }
        }

        @Override
        public <T> T get(Object key, Class<T> type) {
            try {
                return delegate.get(key, type);
            } catch (RuntimeException ex) {
                return null;
            }
        }

        @Override
        public <T> T get(Object key, Callable<T> valueLoader) {
            try {
                return delegate.get(key, valueLoader);
            } catch (RuntimeException ex) {
                try {
                    return valueLoader.call();
                } catch (Exception loaderEx) {
                    throw new IllegalStateException(loaderEx);
                }
            }
        }

        @Override
        public void put(Object key, Object value) {
            try {
                delegate.put(key, value);
            } catch (RuntimeException ex) {
                // no-op: allow business flow to continue when Redis is unavailable
            }
        }

        @Override
        public ValueWrapper putIfAbsent(Object key, Object value) {
            try {
                ValueWrapper existing = delegate.putIfAbsent(key, value);
                return existing == null ? null : new SimpleValueWrapper(existing.get());
            } catch (RuntimeException ex) {
                return null;
            }
        }

        @Override
        public void evict(Object key) {
            try {
                delegate.evict(key);
            } catch (RuntimeException ex) {
                // no-op: allow business flow to continue when Redis is unavailable
            }
        }

        @Override
        public void clear() {
            try {
                delegate.clear();
            } catch (RuntimeException ex) {
                // no-op: allow business flow to continue when Redis is unavailable
            }
        }
    }
}

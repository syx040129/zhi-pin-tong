package com.example.recruitment.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 缓存服务：优先使用 Redis，Redis 不可用时自动降级为本地内存缓存，
 * 保证系统在缺少 Redis 的环境下依然可以完整运行。
 */
@Slf4j
@Component
public class AppCacheService {

    private final StringRedisTemplate redisTemplate;
    private final Map<String, CacheEntry> localCache = new ConcurrentHashMap<>();
    private volatile Boolean redisUsable;

    public AppCacheService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private boolean redisOk() {
        if (redisUsable != null) {
            return redisUsable;
        }
        synchronized (this) {
            if (redisUsable == null) {
                try {
                    redisTemplate.opsForValue().set("__probe__", "1", Duration.ofSeconds(5));
                    redisUsable = Boolean.TRUE;
                    log.info("Redis 连接成功，缓存使用 Redis");
                } catch (Exception e) {
                    redisUsable = Boolean.FALSE;
                    log.warn("Redis 连接失败（{}），缓存自动降级为本地内存", e.getMessage());
                }
            }
        }
        return redisUsable;
    }

    public void set(String key, String value, long seconds) {
        if (redisOk()) {
            try {
                redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(seconds));
                return;
            } catch (RedisConnectionFailureException ignored) {
            } catch (Exception e) {
                log.warn("Redis 写入失败: {}", e.getMessage());
            }
        }
        localCache.put(key, new CacheEntry(value, System.currentTimeMillis() + seconds * 1000L));
    }

    public String get(String key) {
        if (redisOk()) {
            try {
                String value = redisTemplate.opsForValue().get(key);
                if (value != null) {
                    return value;
                }
            } catch (Exception e) {
                log.warn("Redis 读取失败: {}", e.getMessage());
            }
        }
        CacheEntry entry = localCache.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.expireAt < System.currentTimeMillis()) {
            localCache.remove(key);
            return null;
        }
        return entry.value;
    }

    public void delete(String key) {
        try {
            if (redisOk()) {
                redisTemplate.delete(key);
            }
        } catch (Exception ignored) {
        }
        localCache.remove(key);
    }

    public boolean redisAvailable() {
        return redisOk();
    }

    private record CacheEntry(String value, long expireAt) {
    }
}

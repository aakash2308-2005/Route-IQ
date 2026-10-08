package com.smartcommute.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Fixed-window rate limiter backed by Redis counters (docs/04-lld.md section 8; NFR-13).
 *
 * <p>Uses {@code INCR} on a key that expires at the end of the window. The first increment sets the
 * TTL. Because the counter lives in Redis it works across multiple backend instances.</p>
 *
 * <p>Fail-open: if Redis is unavailable, requests are allowed rather than blocked, so a cache outage
 * does not take the API down (NFR-7).</p>
 */
@Component
public class RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RateLimiter.class);

    private final StringRedisTemplate redis;

    public RateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * @return true if the request is allowed, false if the caller has exceeded {@code limit} in the
     * current window.
     */
    public boolean allow(String key, int limit, Duration window) {
        String redisKey = "ratelimit:" + key;
        try {
            Long count = redis.opsForValue().increment(redisKey);
            if (count != null && count == 1L) {
                redis.expire(redisKey, window);
            }
            return count == null || count <= limit;
        } catch (Exception e) {
            log.warn("rate limiter unavailable (fail-open): {}", e.getMessage());
            return true;
        }
    }
}

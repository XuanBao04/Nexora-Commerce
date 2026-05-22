package com.nexoracommerce.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis-backed rate limiter using atomic INCR + EXPIRE (fixed-window algorithm).
 * <p>
 * Key format: {@code rate_limit:<endpoint>:<clientIp>}
 * <p>
 * Fail-open: if Redis is unavailable, requests are allowed through.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private static final String KEY_PREFIX = "rate_limit:";

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Check whether the given key has exceeded the rate limit.
     *
     * @param endpoint      the request path (e.g., /api/auth/login)
     * @param clientIp      the client's IP address
     * @param maxRequests   maximum allowed requests in the window
     * @param windowSeconds duration of the window in seconds
     * @return {@code true} if the request should be blocked (rate limited),
     *         {@code false} if the request is allowed
     */
    public boolean isRateLimited(String endpoint, String clientIp, int maxRequests, int windowSeconds) {
        String key = KEY_PREFIX + endpoint + ":" + clientIp;

        try {
            Long currentCount = redisTemplate.opsForValue().increment(key);

            if (currentCount == null) {
                // Should not happen with INCR, but defensive check
                return false;
            }

            // First request in this window → set the TTL
            if (currentCount == 1) {
                redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
            }

            if (currentCount > maxRequests) {
                log.warn("[RateLimit] Blocked request: endpoint={}, ip={}, count={}/{}, window={}s",
                        endpoint, clientIp, currentCount, maxRequests, windowSeconds);
                return true;
            }

            return false;

        } catch (Exception e) {
            // Fail-open: if Redis is down, allow the request
            log.error("[RateLimit] Redis error — failing open for endpoint={}, ip={}: {}",
                    endpoint, clientIp, e.getMessage());
            return false;
        }
    }
}

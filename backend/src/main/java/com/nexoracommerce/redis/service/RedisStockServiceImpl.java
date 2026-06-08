package com.nexoracommerce.redis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisStockServiceImpl implements RedisStockService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String STOCK_KEY_PREFIX = "stock:";
    private static final String LOCK_KEY_PREFIX = "stock:lock:";
    private static final long LOCK_TIMEOUT_SECONDS = 10L;

    public long getStock(String productId) {
        Object value = redisTemplate.opsForValue().get(getStockKey(productId));
        return value == null ? 0L : Long.parseLong(value.toString());
    }

    public void setStock(String productId, long quantity) {
        redisTemplate.opsForValue().set(getStockKey(productId), quantity);
        log.info("Redis stock set: productId={}, quantity={}", productId, quantity);
    }

    private static final String DECREMENT_STOCK_LUA =
            "local key = KEYS[1]\n" +
            "local quantity = tonumber(ARGV[1])\n" +
            "local current = redis.call('get', key)\n" +
            "if not current then\n" +
            "    return -1\n" +
            "end\n" +
            "local current_num = tonumber(current)\n" +
            "if current_num < quantity then\n" +
            "    return -2\n" +
            "end\n" +
            "redis.call('decrby', key, quantity)\n" +
            "return current_num - quantity";

    private final DefaultRedisScript<Long> decrementStockScript = new DefaultRedisScript<>(DECREMENT_STOCK_LUA, Long.class);

    // Trừ tồn kho atomic bằng Lua script để chống race condition
    public boolean decrementStock(String productId, long quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        String key = getStockKey(productId);
        Long result = redisTemplate.execute(
                decrementStockScript,
                Collections.singletonList(key),
                quantity
        );

        if (result == null || result < 0) {
            log.warn("Decrement stock failed for productId={}, quantity={}. Code: {}", 
                    productId, quantity, result);
            return false;
        }

        return true;
    }

    // Hoàn trả tồn kho (dùng khi rollback)
    public void incrementStock(String productId, long quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        redisTemplate.opsForValue().increment(getStockKey(productId), quantity);
    }

    public boolean decrementIfAvailable(String productId, long quantity) {
        return decrementStock(productId, quantity);
    }

    // Distributed lock cho sản phẩm
    public String acquireLock(String productId) {
        String lockKey = getLockKey(productId);
        String lockToken = System.nanoTime() + "";

        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockToken, Duration.ofSeconds(LOCK_TIMEOUT_SECONDS));

        if (acquired != null && acquired) {
            return lockToken;
        }
        return null;
    }

    public void releaseLock(String productId, String lockToken) {
        String lockKey = getLockKey(productId);
        Object currentToken = redisTemplate.opsForValue().get(lockKey);

        if (currentToken != null && currentToken.toString().equals(lockToken)) {
            redisTemplate.delete(lockKey);
        }
    }

    public boolean isLocked(String productId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(getLockKey(productId)));
    }

    public void deleteStock(String productId) {
        redisTemplate.delete(getStockKey(productId));
    }

    public void clearAllStock() {
        redisTemplate.delete(redisTemplate.keys(STOCK_KEY_PREFIX + "*"));
    }

    private String getStockKey(String productId) {
        return STOCK_KEY_PREFIX + productId;
    }

    private String getLockKey(String productId) {
        return LOCK_KEY_PREFIX + productId;
    }
}

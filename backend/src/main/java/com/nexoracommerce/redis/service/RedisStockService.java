package com.nexoracommerce.redis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Redis-based stock counter service for atomic stock operations
 * Prevents overselling through atomic DECR operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisStockService implements IRedisStockService {

    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String STOCK_KEY_PREFIX = "stock:";
    private static final String LOCK_KEY_PREFIX = "stock:lock:";
    private static final long LOCK_TIMEOUT_SECONDS = 10L;
    
    /**
     * Gets the current stock for a product from Redis
     */
    public long getStock(String productId) {
        Object value = redisTemplate.opsForValue().get(getStockKey(productId));
        if (value == null) {
            return 0L;
        }
        return Long.parseLong(value.toString());
    }
    
    /**
     * Sets the stock for a product (used during sync from DB)
     */
    public void setStock(String productId, long quantity) {
        String key = getStockKey(productId);
        redisTemplate.opsForValue().set(key, quantity);
        log.info("Redis stock set: productId={}, quantity={}", productId, quantity);
    }
    
    /**
     * Atomically decrements stock for checkout
     * Returns true if successful (stock available)
     * Returns false if stock insufficient (prevents overselling)
     */
    public boolean decrementStock(String productId, long quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        
        String key = getStockKey(productId);
        long currentStock = getStock(productId);
        
        if (currentStock < quantity) {
            log.warn("Insufficient stock in Redis: productId={}, required={}, available={}", 
                    productId, quantity, currentStock);
            return false;
        }
        
        // Atomic DECR operation
        Long newStock = redisTemplate.opsForValue().decrement(key, quantity);
        log.info("Stock decremented: productId={}, quantity={}, newStock={}", 
                productId, quantity, newStock);
        
        return true;
    }
    
    /**
     * Atomically increments stock (for rollback on payment failure)
     */
    public void incrementStock(String productId, long quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        
        String key = getStockKey(productId);
        Long newStock = redisTemplate.opsForValue().increment(key, quantity);
        log.info("Stock incremented (rollback): productId={}, quantity={}, newStock={}", 
                productId, quantity, newStock);
    }
    
    /**
     * Atomically decrements stock if available (check-and-decrement in single operation)
     * More reliable than separate check and decrement
     */
    public boolean decrementIfAvailable(String productId, long quantity) {
        return decrementStock(productId, quantity);
    }
    
    /**
     * Acquires a distributed lock for a product
     * Returns lock token if successful, null otherwise
     */
    public String acquireLock(String productId) {
        String lockKey = getLockKey(productId);
        String lockToken = System.nanoTime() + "";
        
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockToken, 
                        java.time.Duration.ofSeconds(LOCK_TIMEOUT_SECONDS));
        
        if (acquired != null && acquired) {
            log.debug("Lock acquired: productId={}, token={}", productId, lockToken);
            return lockToken;
        }
        
        return null;
    }
    
    /**
     * Releases a distributed lock for a product
     */
    public void releaseLock(String productId, String lockToken) {
        String lockKey = getLockKey(productId);
        Object currentToken = redisTemplate.opsForValue().get(lockKey);
        
        if (currentToken != null && currentToken.toString().equals(lockToken)) {
            redisTemplate.delete(lockKey);
            log.debug("Lock released: productId={}", productId);
        }
    }
    
    /**
     * Checks if lock is held (for debugging)
     */
    public boolean isLocked(String productId) {
        String lockKey = getLockKey(productId);
        return redisTemplate.hasKey(lockKey);
    }
    
    /**
     * Deletes stock key (use with caution, typically in tests)
     */
    public void deleteStock(String productId) {
        redisTemplate.delete(getStockKey(productId));
    }
    
    /**
     * Clears all stock data (use with caution, typically in tests or resets)
     */
    public void clearAllStock() {
        redisTemplate.delete(
                redisTemplate.keys(STOCK_KEY_PREFIX + "*")
        );
    }
    
    private String getStockKey(String productId) {
        return STOCK_KEY_PREFIX + productId;
    }
    
    private String getLockKey(String productId) {
        return LOCK_KEY_PREFIX + productId;
    }
}

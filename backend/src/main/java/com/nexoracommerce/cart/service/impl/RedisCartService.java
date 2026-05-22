package com.nexoracommerce.cart.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Encapsulates all Redis Hash operations for the shopping cart.
 * <p>
 * Redis key format: {@code cart:user:{userId}}
 * Hash fields: productId → quantity (stored as Integer)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCartService {

    private static final String KEY_PREFIX = "cart:user:";

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${cart.redis.ttl-days:7}")
    private long ttlDays;

    /**
     * Add quantity to an item in the cart (increments if already exists).
     */
    public void addItem(String userId, String productId, int quantity) {
        String key = buildKey(userId);
        redisTemplate.opsForHash().increment(key, productId, quantity);
        refreshTtl(userId);
        log.debug("[Cart] Added item: userId={}, productId={}, qty={}", userId, productId, quantity);
    }

    /**
     * Remove an item entirely from the cart.
     */
    public void removeItem(String userId, String productId) {
        String key = buildKey(userId);
        redisTemplate.opsForHash().delete(key, productId);
        refreshTtl(userId);
        log.debug("[Cart] Removed item: userId={}, productId={}", userId, productId);
    }

    /**
     * Set the exact quantity for an item (overwrites previous value).
     */
    public void setItemQuantity(String userId, String productId, int quantity) {
        String key = buildKey(userId);
        redisTemplate.opsForHash().put(key, productId, quantity);
        refreshTtl(userId);
        log.debug("[Cart] Set quantity: userId={}, productId={}, qty={}", userId, productId, quantity);
    }

    /**
     * Get all items in the cart as a Map of productId → quantity.
     */
    public Map<String, Integer> getCart(String userId) {
        String key = buildKey(userId);
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);

        Map<String, Integer> cart = new HashMap<>();
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            String productId = entry.getKey().toString();
            int quantity = ((Number) entry.getValue()).intValue();
            cart.put(productId, quantity);
        }
        return cart;
    }

    /**
     * Get the quantity of a specific item in the cart.
     * Returns null if the item does not exist.
     */
    public Integer getItemQuantity(String userId, String productId) {
        String key = buildKey(userId);
        Object value = redisTemplate.opsForHash().get(key, productId);
        if (value == null) {
            return null;
        }
        return ((Number) value).intValue();
    }

    /**
     * Clear the entire cart for a user.
     */
    public void clearCart(String userId) {
        String key = buildKey(userId);
        redisTemplate.delete(key);
        log.debug("[Cart] Cleared cart: userId={}", userId);
    }

    /**
     * Refresh the TTL on the cart key (called on every write operation).
     */
    public void refreshTtl(String userId) {
        String key = buildKey(userId);
        redisTemplate.expire(key, ttlDays, TimeUnit.DAYS);
    }

    private String buildKey(String userId) {
        return KEY_PREFIX + userId;
    }
}

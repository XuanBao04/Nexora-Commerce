package com.nexoracommerce.cart.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Encapsulates all Redis Hash operations for the shopping cart.
 * <p>
 * Redis key format: {@code cart:user:{userId}}
 * Hash fields: variantSku → quantity (stored as Integer)
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
        final String key = Objects.requireNonNull(buildKey(userId));
        final String field = Objects.requireNonNull(productId);
        redisTemplate.opsForHash().increment(key, field, quantity);
        refreshTtl(userId);
        log.debug("[Cart] Added item: userId={}, sku={}, qty={}", userId, productId, quantity);
    }

    /**
     * Remove an item entirely from the cart.
     */
    public void removeItem(String userId, String productId) {
        final String key = Objects.requireNonNull(buildKey(userId));
        final String field = Objects.requireNonNull(productId);
        redisTemplate.opsForHash().delete(key, field);
        refreshTtl(userId);
        log.debug("[Cart] Removed item: userId={}, sku={}", userId, productId);
    }

    /**
     * Set the exact quantity for an item (overwrites previous value).
     */
    public void setItemQuantity(String userId, String productId, int quantity) {
        final String key = Objects.requireNonNull(buildKey(userId));
        final String field = Objects.requireNonNull(productId);
        redisTemplate.opsForHash().put(key, field, quantity);
        refreshTtl(userId);
        log.debug("[Cart] Set quantity: userId={}, sku={}, qty={}", userId, productId, quantity);
    }

    /**
    * Get all items in the cart as a Map of variantSku → quantity.
     */
    public Map<String, Integer> getCart(String userId) {
        final String key = Objects.requireNonNull(buildKey(userId));
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
        final String key = Objects.requireNonNull(buildKey(userId));
        final String field = Objects.requireNonNull(productId);
        Object value = redisTemplate.opsForHash().get(key, field);
        if (value == null) {
            return null;
        }
        return ((Number) value).intValue();
    }

    /**
     * Clear the entire cart for a user.
     */
    public void clearCart(String userId) {
        final String key = Objects.requireNonNull(buildKey(userId));
        redisTemplate.delete(key);
        log.debug("[Cart] Cleared cart: userId={}", userId);
    }

    /**
     * Refresh the TTL on the cart key (called on every write operation).
     */
    public void refreshTtl(String userId) {
        final String key = Objects.requireNonNull(buildKey(userId));
        redisTemplate.expire(key, ttlDays, TimeUnit.DAYS);
    }

    private String buildKey(String userId) {
        return KEY_PREFIX + userId;
    }
}

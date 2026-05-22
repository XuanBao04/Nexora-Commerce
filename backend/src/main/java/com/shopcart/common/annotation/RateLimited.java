package com.shopcart.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to apply Redis-backed rate limiting on controller methods.
 * <p>
 * Usage: {@code @RateLimited(maxRequests = 5, windowSeconds = 60)}
 * <p>
 * Rate limiting is enforced per client IP using a fixed-window counter stored in Redis.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimited {

    /**
     * Maximum number of requests allowed within the time window.
     */
    int maxRequests() default 5;

    /**
     * Duration of the rate-limit window in seconds.
     */
    int windowSeconds() default 60;
}

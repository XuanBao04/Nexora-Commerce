package com.shopcart.config.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopcart.common.annotation.RateLimited;
import com.shopcart.common.response.ErrorResponse;
import com.shopcart.common.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;

/**
 * Spring MVC interceptor that enforces rate limiting on controller methods
 * annotated with {@link RateLimited}.
 * <p>
 * Runs after Spring Security filters, so it does not interfere with
 * authentication/authorization or CORS processing.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiterService rateLimiterService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        // Only intercept actual controller methods (not static resources, etc.)
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // Check for @RateLimited annotation on the method
        RateLimited rateLimited = handlerMethod.getMethodAnnotation(RateLimited.class);
        if (rateLimited == null) {
            return true; // No rate limiting configured → allow
        }

        String clientIp = extractClientIp(request);
        String endpoint = request.getRequestURI();
        int maxRequests = rateLimited.maxRequests();
        int windowSeconds = rateLimited.windowSeconds();

        if (rateLimiterService.isRateLimited(endpoint, clientIp, maxRequests, windowSeconds)) {
            // Build 429 response using the project's standard ErrorResponse format
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(windowSeconds));

            ErrorResponse errorResponse = new ErrorResponse(
                    LocalDateTime.now(),
                    HttpStatus.TOO_MANY_REQUESTS.value(),
                    "Too Many Requests",
                    "You have exceeded the rate limit. Please try again after " + windowSeconds + " seconds.",
                    endpoint
            );

            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            return false; // Abort the request
        }

        return true; // Allow the request
    }

    /**
     * Extract the real client IP, supporting reverse proxy headers.
     * Priority: X-Forwarded-For → X-Real-IP → request.getRemoteAddr()
     */
    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // X-Forwarded-For may contain multiple IPs: "client, proxy1, proxy2"
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }

        return request.getRemoteAddr();
    }
}

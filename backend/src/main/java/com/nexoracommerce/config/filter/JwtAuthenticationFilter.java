package com.nexoracommerce.config.filter;

import com.nexoracommerce.auth.service.impl.CustomUserDetailsService;
import com.nexoracommerce.config.security.JwtService;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

/**
 * JWT Authentication Filter — executed exactly once per HTTP request.
 * Delegates exceptions to GlobalExceptionHandler using HandlerExceptionResolver.
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final HandlerExceptionResolver resolver;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            CustomUserDetailsService userDetailsService,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver
    ) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.resolver = resolver;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String token = extractTokenFromRequest(request);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String username = jwtService.extractUsername(token);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtService.isTokenValid(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    log.debug("[JWT] Authenticated user '{}' for {} {}",
                            username, request.getMethod(), request.getRequestURI());
                } else {
                    throw new io.jsonwebtoken.security.SignatureException("JWT token is invalid");
                }
            }
            filterChain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("[JWT] Authentication failed: {}", e.getMessage());
            resolver.resolveException(request, response, null, e);
        } catch (Exception e) {
            log.error("[JWT] Unexpected authentication filter error", e);
            resolver.resolveException(request, response, null, e);
        }
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (StringUtils.hasText(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }

        return null;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/v1/authentications/sessions")
                || path.startsWith("/api/v1/authentications/registrations")
                || path.startsWith("/actuator/health");
    }
}

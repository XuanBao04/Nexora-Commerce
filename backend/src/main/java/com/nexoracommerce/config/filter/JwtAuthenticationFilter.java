package com.nexoracommerce.config.filter;

import com.nexoracommerce.auth.service.impl.CustomUserDetailsService;
import com.nexoracommerce.config.security.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter — executed exactly once per HTTP request.
 *
 * <p>Execution flow:
 * <ol>
 *   <li>Read the {@code Authorization} header.</li>
 *   <li>If the value starts with {@code "Bearer "}, extract the raw token.</li>
 *   <li>Parse the username from the token via {@link JwtService}.</li>
 *   <li>If username is present AND no authentication exists yet in the context:</li>
 *       <ul>
 *         <li>Load {@link UserDetails} from {@link CustomUserDetailsService}.</li>
 *         <li>Validate the token (signature, expiry, subject match).</li>
 *         <li>Create a {@link UsernamePasswordAuthenticationToken} and push it
 *             into {@link SecurityContextHolder}.</li>
 *       </ul>
 *   <li>Continue the filter chain regardless of outcome.</li>
 * </ol>
 *
 * <p>Failures are handled gracefully: invalid/expired tokens are logged and
 * the request continues unauthenticated — Spring Security's authorization rules
 * will then reject it with 401 if the endpoint requires authentication.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Extract token from Authorization header
        final String token = extractTokenFromRequest(request);

        // 2. If no token present → skip to next filter (endpoint may be public)
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Parse username from token
        final String username;
        try {
            username = jwtService.extractUsername(token);
        } catch (ExpiredJwtException e) {
            log.warn("[JWT] Token expired for request: {} {}", request.getMethod(), request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("[JWT] Invalid token structure: {}", e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        // 4. Only proceed if username is present AND SecurityContext is empty
        //    (avoid overwriting an already-authenticated context)
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // 5. Validate token against the loaded UserDetails
            if (jwtService.isTokenValid(token, userDetails)) {

                // 6. Build an authenticated token and register it in the SecurityContext
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,                         // credentials not needed post-auth
                                userDetails.getAuthorities()
                        );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.debug("[JWT] Authenticated user '{}' for {} {}",
                        username, request.getMethod(), request.getRequestURI());
            } else {
                log.warn("[JWT] Token invalid for user '{}' on {} {}",
                        username, request.getMethod(), request.getRequestURI());
            }
        }

        // 7. Always continue the filter chain
        filterChain.doFilter(request, response);
    }

    /**
     * Extract the raw JWT string from the {@code Authorization: Bearer <token>} header.
     *
     * @param request the incoming HTTP request
     * @return the raw token string, or {@code null} if the header is absent / malformed
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (StringUtils.hasText(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }

        return null;
    }

    /**
     * Skip JWT processing entirely for public endpoints that never carry a token.
     * This is a performance optimization — the filter chain still works correctly
     * even without this override, but skipping saves unnecessary header parsing.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/v1/authentications/sessions")
                || path.startsWith("/api/v1/authentications/registrations")
                || path.startsWith("/actuator/health");
    }
}

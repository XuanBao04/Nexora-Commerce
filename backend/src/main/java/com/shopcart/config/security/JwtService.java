package com.shopcart.config.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT Service - responsible for creating, validating, and parsing JWT tokens.
 *
 * <p>Token structure:
 * <ul>
 *   <li>Header: algorithm = HS256</li>
 *   <li>Payload: sub (username), role, iat (issued at), exp (expiration)</li>
 *   <li>Signature: HMAC-SHA256 with secret key from application config</li>
 * </ul>
 *
 * <p>Two token types:
 * <ul>
 *   <li>Access Token  — short-lived (default 24h), used on every API call</li>
 *   <li>Refresh Token — long-lived  (default 7d), used only to obtain new access token</li>
 * </ul>
 */
@Slf4j
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    // ======================== Token Generation ========================

    /**
     * Generate an access token for an authenticated user.
     * Embeds the user's role as an extra claim for authorization checks.
     *
     * @param userDetails the authenticated user details
     * @return signed JWT access token string
     */
    public String generateAccessToken(UserDetails userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();
        // Store the first authority (e.g. ROLE_CUSTOMER) as a plain claim for easy extraction
        if (!userDetails.getAuthorities().isEmpty()) {
            extraClaims.put("role", userDetails.getAuthorities().iterator().next().getAuthority());
        }
        return buildToken(extraClaims, userDetails.getUsername(), accessTokenExpiration);
    }

    /**
     * Generate a refresh token for an authenticated user.
     * Contains no extra claims — only the subject (username) and expiry.
     *
     * @param userDetails the authenticated user details
     * @return signed JWT refresh token string
     */
    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(new HashMap<>(), userDetails.getUsername(), refreshTokenExpiration);
    }

    // ======================== Token Parsing ========================

    /**
     * Extract the username (subject) from a token.
     *
     * @param token the JWT string
     * @return username stored in the token's subject claim
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract the role claim from a token.
     *
     * @param token the JWT string
     * @return role string (e.g. "ROLE_CUSTOMER"), or null if not present
     */
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    /**
     * Extract the expiration date from a token.
     *
     * @param token the JWT string
     * @return expiration date
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Generic claim extractor using a resolver function.
     *
     * @param token          the JWT string
     * @param claimsResolver a function to pull a specific field from Claims
     * @param <T>            the return type of the claim
     * @return the extracted claim value
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // ======================== Token Validation ========================

    /**
     * Validate that a token belongs to the given user AND has not expired.
     *
     * @param token       the JWT string
     * @param userDetails the user to validate against
     * @return true if valid, false otherwise
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check whether the token structure is valid (parseable and properly signed),
     * regardless of expiry. Useful for logging/debugging purposes.
     *
     * @param token the JWT string
     * @return true if the token can be parsed and the signature is valid
     */
    public boolean isTokenStructureValid(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            // Expired but structurally valid — still consider the structure OK
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT structure invalid: {}", e.getMessage());
            return false;
        }
    }

    // ======================== Private Helpers ========================

    /**
     * Build and sign a JWT token with provided claims, subject, and expiration.
     */
    private String buildToken(
            Map<String, Object> extraClaims,
            String subject,
            long expiration
    ) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Parse and return all claims from the token.
     * Throws JwtException subtypes on invalid/expired tokens.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Derive the HMAC-SHA256 signing key from the hex-encoded secret.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Check if the token's expiration timestamp is before the current time.
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
}

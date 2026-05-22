---
name: spring-security-jwt-skill
description: Enforce stateless JWT authentication, token lifecycle management, role-based access control, and Spring Security 6.x best practices for e-commerce APIs.
---

## Scope & Activation Rules

Activate when:
- Implementing authentication/authorization endpoints (login, logout, refresh)
- Creating security filters, JWT providers, or claim extractors
- Configuring Spring Security (SecurityFilterChain, SecurityConfig)
- Protecting REST endpoints with role-based access (@PreAuthorize, @Secured)
- Handling CORS, CSRF, or token expiration scenarios
- Managing user credentials and password encoding

## System Directives

### DO
- **Use Stateless JWT Architecture**: Never store session state on server. Validate tokens on every request via filter.
- **Implement Short-Lived Access Tokens**: 15-30 minutes max. Use refresh tokens (7-30 days) for token renewal.
- **Extract User ID from JWT Claim**: Use custom claim (e.g., `userId`, `sub`) during authentication context setup.
- **Hash Passwords with BCrypt**: Always use `BCryptPasswordEncoder`. Never store plaintext or unsalted passwords.
- **Validate Token Signature & Expiry**: Reject any token with invalid signature or expired claim.
- **Use SecurityContext for Principal Extraction**: Leverage `SecurityContextHolder.getContext().getAuthentication()` to get current user.
- **Implement CORS Explicitly**: Define allowed origins, methods, and credentials via `CorsConfigurationSource`.
- **Protect Sensitive Endpoints with Roles**: Use `@PreAuthorize("hasRole('USER')")` or `@Secured("ROLE_USER")`.
- **Clear SecurityContext on Logout**: Manually clear context to prevent token reuse.
- **Log Authentication Events**: Entry/exit of login, logout, token refresh; failed authentication attempts.
- **Use @JsonIgnore for Sensitive Fields**: Never expose passwords or sensitive data in JSON responses.

### DO NOT
- Store passwords in plaintext or with weak encoding (MD5, SHA1 without salt).
- Use Spring's default in-memory session management. Always use stateless JWT.
- Expose JWT secret in code or version control. Use environment variables or secure vaults.
- Set JWT expiry to more than 30 days for access tokens. Implement refresh token rotation instead.
- Skip token validation on any endpoint. Validate signature, expiry, and claims universally.
- Use session-based authentication alongside JWT. Choose one approach consistently.
- Allow CORS from `*` in production. Specify exact origins.
- Decode JWT without verifying signature. Always validate cryptographically.
- Store unencrypted sensitive data (PII, payment info) in JWT claims.
- Log JWT tokens in full. Only log token ID or username, never the full token.
- Implement custom encryption for JWT. Use industry-standard algorithms (HS256, RS256).
- Skip HTTPS in production. All authentication flows require TLS.

## Production Reference Implementation

### JWT Token Provider (Token Lifecycle)

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider {
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${jwt.access-token-expiry:900}") // 15 minutes
    private long accessTokenExpiry;
    
    @Value("${jwt.refresh-token-expiry:604800}") // 7 days
    private long refreshTokenExpiry;
    
    private final PasswordEncoder passwordEncoder;
    
    public AuthTokenResponse generateTokens(String userId, String email, List<String> roles) {
        log.info("Generating tokens for user: {}", userId);
        
        String accessToken = createToken(userId, email, roles, accessTokenExpiry, "access");
        String refreshToken = createToken(userId, email, roles, refreshTokenExpiry, "refresh");
        
        return new AuthTokenResponse(
            accessToken,
            refreshToken,
            "Bearer",
            accessTokenExpiry,
            email
        );
    }
    
    private String createToken(String userId, String email, List<String> roles, 
                               long expiryInSeconds, String type) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("email", email);
        claims.put("roles", roles);
        claims.put("type", type);
        
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(userId)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expiryInSeconds * 1000))
            .signWith(SignatureAlgorithm.HS256, jwtSecret)
            .compact();
    }
    
    public Claims extractClaims(String token) {
        try {
            return Jwts.parser()
                .setSigningKey(jwtSecret)
                .parseClaimsJws(token)
                .getBody();
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token", e);
            throw new JwtAuthenticationException("Invalid or expired JWT token");
        }
    }
    
    public String extractUserId(String token) {
        return extractClaims(token).get("userId", String.class);
    }
    
    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }
    
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        return extractClaims(token).get("roles", List.class);
    }
    
    public boolean isTokenValid(String token) {
        try {
            Jwts.parser()
                .setSigningKey(jwtSecret)
                .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.info("JWT token expired");
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token");
            return false;
        }
    }
}
```

### JWT Authentication Filter

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtTokenProvider jwtTokenProvider;
    
    private static final List<String> EXCLUDED_PATHS = List.of(
        "/api/auth/login",
        "/api/auth/register",
        "/api/auth/refresh",
        "/api/health",
        "/swagger-ui",
        "/v3/api-docs"
    );
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        try {
            String path = request.getRequestURI();
            if (EXCLUDED_PATHS.stream().anyMatch(path::startsWith)) {
                filterChain.doFilter(request, response);
                return;
            }
            
            String token = extractBearerToken(request);
            if (token != null && jwtTokenProvider.isTokenValid(token)) {
                String userId = jwtTokenProvider.extractUserId(token);
                List<String> roles = jwtTokenProvider.extractRoles(token);
                
                List<GrantedAuthority> authorities = roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                    .collect(Collectors.toList());
                
                UsernamePasswordAuthenticationToken auth = 
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug("JWT token validated for user: {}", userId);
            }
            
            filterChain.doFilter(request, response);
        } catch (JwtAuthenticationException e) {
            log.warn("JWT authentication failed: {}", e.getMessage());
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "JWT_INVALID");
        } catch (Exception e) {
            log.error("Authentication filter error", e);
            sendErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, "AUTH_ERROR");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
    
    private String extractBearerToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
    
    private void sendErrorResponse(HttpServletResponse response, 
                                   HttpStatus status, String errorCode) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.getWriter().write(
            "{\"error\":\"" + errorCode + "\",\"message\":\"Authentication failed\"}"
        );
    }
}
```

### Security Configuration

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/api/health", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/orders/**", "/api/cart/**").hasRole("USER")
                .anyRequest().authenticated()
            )
            .cors().configurationSource(corsConfigurationSource())
            .and()
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling()
            .authenticationEntryPoint((request, response, authException) -> {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"UNAUTHORIZED\",\"message\":\"" + 
                    authException.getMessage() + "\"}");
            })
            .accessDeniedHandler((request, response, accessDeniedException) -> {
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"FORBIDDEN\",\"message\":\"Access denied\"}");
            });
        
        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
            "https://nexora.com",
            "https://app.nexora.com"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```

### Authentication Controller

```java
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    
    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    
    @PostMapping("/login")
    public ResponseEntity<AuthTokenResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for user: {}", request.email());
        AuthTokenResponse response = authService.login(request.email(), request.password());
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration attempt for email: {}", request.email());
        UserResponse response = authService.register(request);
        return ResponseEntity
            .created(URI.create("/api/users/" + response.id()))
            .body(response);
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<AuthTokenResponse> refreshToken(
        @RequestHeader("Authorization") String bearerToken) {
        
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            throw new JwtAuthenticationException("Invalid refresh token format");
        }
        
        String refreshToken = bearerToken.substring(7);
        AuthTokenResponse response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        log.info("Logout for user: {}", SecurityContextHolder.getContext().getAuthentication().getName());
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }
}
```

### AuthService (Service Layer)

```java
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {
    
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    
    public AuthTokenResponse login(String email, String password) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UserNotFoundException("User not found: " + email));
        
        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.warn("Failed login attempt for user: {}", email);
            throw new AuthenticationException("Invalid email or password");
        }
        
        log.info("User logged in successfully: {}", email);
        return jwtTokenProvider.generateTokens(
            user.getId(),
            user.getEmail(),
            user.getRoles().stream().map(Role::getName).collect(Collectors.toList())
        );
    }
    
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already registered");
        }
        
        User user = User.builder()
            .email(request.email())
            .firstName(request.firstName())
            .lastName(request.lastName())
            .password(passwordEncoder.encode(request.password()))
            .roles(List.of(new Role("USER")))
            .createdAt(LocalDateTime.now())
            .build();
        
        User savedUser = userRepository.save(user);
        log.info("User registered: {}", request.email());
        return userMapper.toResponse(savedUser);
    }
    
    public AuthTokenResponse refreshToken(String refreshToken) {
        if (!jwtTokenProvider.isTokenValid(refreshToken)) {
            throw new JwtAuthenticationException("Refresh token expired or invalid");
        }
        
        String userId = jwtTokenProvider.extractUserId(refreshToken);
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
        
        log.info("Token refreshed for user: {}", userId);
        return jwtTokenProvider.generateTokens(
            user.getId(),
            user.getEmail(),
            user.getRoles().stream().map(Role::getName).collect(Collectors.toList())
        );
    }
}
```

### Records for Auth Requests/Responses

```java
public record LoginRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Email format is invalid")
    String email,
    
    @NotBlank(message = "Password is required")
    String password
) {}

public record RegisterRequest(
    @NotBlank String email,
    @NotBlank String password,
    @NotBlank String firstName,
    @NotBlank String lastName
) {}

public record AuthTokenResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn,
    String email
) {}

public record UserResponse(
    String id,
    String email,
    String firstName,
    String lastName,
    LocalDateTime createdAt,
    List<String> roles
) {}
```

## Anti-Patterns & Automated Fixes

### Anti-Pattern 1: Storing JWT Secret in Code
**Problem**: Exposing JWT secret in source control compromises token security.
```yaml
# ❌ WRONG - application.yml
jwt:
  secret: "my-super-secret-key-12345"
```
**Fix**: Use environment variables and `.gitignore`.
```yaml
# ✅ CORRECT - application.yml
jwt:
  secret: ${JWT_SECRET}
  access-token-expiry: 900
  refresh-token-expiry: 604800
```

### Anti-Pattern 2: Missing Expiry Validation
**Problem**: Accepting expired tokens bypasses security.
```java
// ❌ WRONG
public boolean isTokenValid(String token) {
    try {
        Jwts.parser().setSigningKey(secret).parseClaimsJws(token);
        return true;
    } catch (Exception e) {
        return false;
    }
}
```
**Fix**: Explicitly check expiry and signature.
```java
// ✅ CORRECT
public boolean isTokenValid(String token) {
    try {
        Claims claims = Jwts.parser()
            .setSigningKey(jwtSecret)
            .parseClaimsJws(token)
            .getBody();
        return !isTokenExpired(claims);
    } catch (ExpiredJwtException | JwtException e) {
        return false;
    }
}

private boolean isTokenExpired(Claims claims) {
    return claims.getExpiration().before(new Date());
}
```

### Anti-Pattern 3: CORS Allows Any Origin
**Problem**: `allowedOrigins: "*"` exposes API to CSRF attacks.
```java
// ❌ WRONG
config.setAllowedOrigins(List.of("*"));
config.setAllowCredentials(true);
```
**Fix**: Specify exact origins.
```java
// ✅ CORRECT
config.setAllowedOrigins(List.of(
    "https://nexora.com",
    "https://app.nexora.com"
));
config.setAllowCredentials(true);
```

### Anti-Pattern 4: Logging Full JWT Tokens
**Problem**: Tokens in logs can be intercepted; full token exposure is a security risk.
```java
// ❌ WRONG
log.info("Generated token: {}", token);
```
**Fix**: Log only metadata, never the full token.
```java
// ✅ CORRECT
log.info("Generated token for user: {}, expires in: {} seconds", userId, expirySeconds);
```

## Verification Commands

### Verify JWT Configuration
```bash
# Check JWT secret is not in code
grep -r "jwt.*secret.*=" backend/src/main/resources --include="*.yml" | grep -v "\${" || echo "✅ JWT secret not hardcoded"

# Verify CORS configuration is restrictive
grep -A 5 "setAllowedOrigins" backend/src/main/java --include="*.java" | grep -v '"*"' && echo "✅ CORS origins are restricted"
```

### Test Token Generation & Validation
```bash
# Build and start application
cd backend && mvn clean package -DskipTests
java -jar target/nexora-commerce-backend-1.0.0.jar &

# Test login endpoint
JWT_TOKEN=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123"}' \
  | jq -r '.accessToken')

# Verify token structure (should have 3 parts separated by dots)
echo $JWT_TOKEN | grep -oP '^\w+\.\w+\.\w+$' && echo "✅ Valid JWT format"

# Test token refresh
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Authorization: Bearer $JWT_TOKEN"
```

### Verify Password Encoding
```bash
# Check BCrypt is used (not MD5 or SHA1)
grep -r "BCryptPasswordEncoder\|PasswordEncoder" backend/src/main/java --include="*.java" | grep -v "//.*" && echo "✅ Using BCrypt"

# Verify no plaintext passwords in code
grep -r "password.*=.*\"" backend/src/main/java --include="*.java" | grep -v "encode\|hash" || echo "✅ No hardcoded plaintext passwords"
```

### Verify Stateless Configuration
```bash
# Check SessionCreationPolicy is STATELESS
grep -r "STATELESS\|stateless" backend/src/main/java --include="*.java" && echo "✅ Stateless session configuration"

# Verify JwtAuthenticationFilter is registered before UsernamePasswordAuthenticationFilter
grep -A 2 "addFilterBefore.*JwtAuthenticationFilter" backend/src/main/java && echo "✅ JWT filter registered"
```

### Security Audit
```bash
# Run OWASP dependency check
mvn org.owasp:dependency-check-maven:check

# Test token expiry (manually)
curl -X GET http://localhost:8080/api/orders \
  -H "Authorization: Bearer $EXPIRED_TOKEN" \
  # Should return 401 Unauthorized
```

---
name: spring-exception-handling-skill
description: Strict guidelines and architecture standards for designing a centralized, secure, and unified REST Global Exception Handling mechanism in Spring Boot 3.x.
---

## Scope & Activation Rules
This skill is activated automatically when creating, refactoring, or reviewing any **Global Exception Handler** (annotated with `@RestControllerAdvice`), custom exceptions, validation constraints, or API response error wrappers in a Spring Boot application. All exception handlers must strictly adhere to these rules to maintain a secure, robust, and industry-standard exception handling layer.

---

## System Directives

### DO
1. **Centralize Error Boundary:** Rely entirely on a single `@RestControllerAdvice` to catch and format errors. Keep controllers completely thin and free of `try-catch` blocks. Allow exceptions to propagate upward naturally.
2. **Standardize JSON Error Structure:** Always return errors in a unified generic structure, wrapping them in an `ApiResponse<T>` envelope where `success` is `false`, `data` is `null`, and both HTTP `status` and `message` are clearly populated.
3. **Handle Validation Errors Gracefully:** Intercept `MethodArgumentNotValidException` (thrown by `@Valid` on request payloads) and `ConstraintViolationException`. Extract all field-specific validation errors (field name and violation message) into a clean key-value map and return it in the `data` field with a `400 Bad Request` status.
4. **Enforce Production Data Masking:** 
   - For all unhandled general exceptions (`Exception.class` / `RuntimeException.class`), log the full stack trace on the backend for developers.
   - Return a sanitized, secure error message to the client (e.g., `"An unexpected error occurred"`) to prevent exposing database schemas, SQL queries, or class hierarchies.
   - Generate and include a unique `Error ID` (Correlation ID / UUID) in both the log statement and the client response so the error can be traced without leaking stack traces.
5. **Use Precise HTTP Status Mappings:** Map specific exception classes to their logical HTTP status codes:
   - `ResourceNotFoundException` / `EntityNotFoundException` $\rightarrow$ `404 Not Found`
   - `BusinessException` / Validation Errors $\rightarrow$ `400 Bad Request`
   - `AccessDeniedException` $\rightarrow$ `403 Forbidden`
   - `BadCredentialsException` / `AuthenticationException` $\rightarrow$ `401 Unauthorized`
   - `HttpRequestMethodNotSupportedException` $\rightarrow$ `405 Method Not Allowed`
6. **Log with Proper Severity Levels:** Use `warn` level logging for client-caused errors (validation failures, 404s, business rules violations) to avoid spamming alerts. Use `error` level logging exclusively for server-side unhandled errors (500s).

### DO NOT
1. **Never Return Raw Stack Traces:** Never allow Java stack traces, database driver messages, or package names to escape to the HTTP response body. Doing so presents a high-risk security vulnerability.
2. **Never Return HTML Error Pages:** Ensure the handler always returns a JSON content-type (`application/json`), preventing Spring Boot's default BasicErrorController from rendering the White Label HTML error page.
3. **Do Not Catch Exceptions to Only Log and Rethrow:** Avoid adding repetitive `try-catch` blocks in Service or Controller implementations that merely log the exception and rethrow it. Let them bubble up naturally to the handler.
4. **Never Leak Sensitive Auth Details:** Avoid returning highly specific auth errors that help attackers enumerate usernames (e.g., do not return `"Username not found"`, instead return a generic `"Invalid username or password"`).

---

## Standard Reference Pattern

Below is the standard clean architecture blueprint illustrating a custom business exception heirarchy alongside a perfectly unified, secure `@RestControllerAdvice`.

### 1. Base Business Exceptions (`BusinessException.java` & `ResourceNotFoundException.java`)

```java
package com.nexoracommerce.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base custom exception for all business and domain rule violations.
 */
@Getter
public class BusinessException extends RuntimeException {
    private final HttpStatus status;

    public BusinessException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
    }

    public BusinessException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}
```

```java
package com.nexoracommerce.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a requested database resource or entity is not found.
 */
public class ResourceNotFoundException extends BusinessException {
    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
```

### 2. Centralized REST Exception Handler (`GlobalExceptionHandler.java`)

```java
package com.nexoracommerce.common.exception;

import com.nexoracommerce.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Centralized exception handling boundary for the REST Web Layer.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles custom business domain exceptions.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        log.warn("Business rule violation: {}", ex.getMessage());
        
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .timestamp(LocalDateTime.now())
                .status(ex.getStatus().value())
                .message(ex.getMessage())
                .build();
                
        return ResponseEntity.status(ex.getStatus()).body(response);
    }

    /**
     * Handles DTO input validation errors and maps them to field-level details.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(MethodArgumentNotValidException ex) {
        log.warn("Input validation failed for DTO: {}", ex.getBindingResult().getObjectName());
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ApiResponse<Map<String, String>> response = ApiResponse.<Map<String, String>>builder()
                .success(false)
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .message("Validation failed")
                .data(errors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles Spring Security access control violations (403).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Unauthorized resource access attempt: {}", ex.getMessage());
        
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .message("Access denied. You do not have permission to perform this action.")
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * Catch-all fallback handler for system and server errors. Masks internal details
     * and generates a unique trace ID.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception ex) {
        String errorId = UUID.randomUUID().toString();
        log.error("Unhandled internal server exception [Error ID: {}]", errorId, ex);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message("An unexpected error occurred. Please contact system support and quote Error ID: " + errorId)
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
```

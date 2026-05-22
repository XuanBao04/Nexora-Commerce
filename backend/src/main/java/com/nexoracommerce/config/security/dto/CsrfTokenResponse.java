package com.nexoracommerce.config.security.dto;

public record CsrfTokenResponse(
    String headerName,
    String parameterName,
    String token
) {}

package com.nexoracommerce.config.security;

import com.nexoracommerce.common.response.ApiResponse;
import com.nexoracommerce.config.security.dto.CsrfTokenResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/csrf-tokens")
public class CsrfController {

    @GetMapping
    public ResponseEntity<ApiResponse<CsrfTokenResponse>> csrfToken(CsrfToken csrfToken) {
        CsrfTokenResponse response = new CsrfTokenResponse(
                csrfToken.getHeaderName(),
                csrfToken.getParameterName(),
                csrfToken.getToken()
        );
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}

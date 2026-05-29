package com.nexoracommerce.user.dto.response;

import lombok.Builder;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Builder
public record UserProfileResponse(
    UUID id,
    String username,
    String fullName,
    String email,
    LocalDateTime createdAt,
    Set<String> roles,
    boolean active,
    String role
) {}

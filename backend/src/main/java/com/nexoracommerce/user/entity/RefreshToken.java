package com.nexoracommerce.user.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = "user")
@Entity
@Table(name = "refresh_tokens", indexes = {
    @Index(name = "idx_refresh_tokens_user", columnList = "user_id")
})
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "refresh_tokens_id_seq")
    @SequenceGenerator(name = "refresh_tokens_id_seq", sequenceName = "refresh_tokens_id_seq", allocationSize = 1)
    private Long id;

    @NotNull(message = "User is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotBlank(message = "Token is required")
    @Size(max = 255, message = "Token must not exceed 255 characters")
    @Column(nullable = false, unique = true, length = 255)
    private String token;

    @NotNull(message = "Expiry date is required")
    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;
}

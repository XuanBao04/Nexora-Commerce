package com.nexoracommerce.ai.entity;

import com.nexoracommerce.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = "user")
@Entity
@Table(name = "ai_chat_messages", indexes = {
    @Index(name = "idx_ai_chat_session", columnList = "session_id"),
    @Index(name = "idx_ai_messages_user", columnList = "user_id")
})
public class AiChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // Nullable for guest chat sessions

    @NotBlank(message = "Session ID is required")
    @Size(max = 255, message = "Session ID must not exceed 255 characters")
    @Column(name = "session_id", nullable = false, length = 255)
    private String sessionId;

    @NotBlank(message = "Role is required")
    @Size(max = 50, message = "Role must not exceed 50 characters")
    @Column(nullable = false, length = 50)
    private String role; // 'USER' or 'AI'

    @NotBlank(message = "Content is required")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
}


package com.shopcart.ai.entity;

import com.shopcart.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "ai_chat_messages")
public class AiChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ai_chat_messages_id_seq")
    @SequenceGenerator(name = "ai_chat_messages_id_seq", sequenceName = "ai_chat_messages_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // Nullable for guest chat sessions

    @Column(name = "session_id", nullable = false, length = 255)
    private String sessionId;

    @Column(nullable = false, length = 50)
    private String role; // 'USER' or 'AI'

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
}

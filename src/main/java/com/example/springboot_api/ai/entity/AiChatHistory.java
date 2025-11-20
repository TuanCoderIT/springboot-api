package com.example.springboot_api.ai.entity;

import com.example.springboot_api.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "ai_chat_history",
        indexes = {
                @Index(name = "idx_chat_history_user_id", columnList = "user_id"),
                @Index(name = "idx_chat_history_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_chat_history_user"))
    private User user;

    @Column(nullable = false, columnDefinition = "text")
    private String question;

    @Column(nullable = false, columnDefinition = "text")
    private String answer;

    // Lưu JSON nguồn trích dẫn (list chunk id, file id, title, v.v.)
    @Column(name = "source_chunks", columnDefinition = "jsonb")
    private String sourceChunks;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}


package com.example.springboot_api.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity(name = MessageReaction.ENTITY_NAME)
@Table(name = MessageReaction.TABLE_NAME, schema = "public", indexes = {
        @Index(name = "uq_message_reactions", columnList = "message_id, user_id, emoji", unique = true),
        @Index(name = "idx_message_reactions_message", columnList = "message_id")
})
public class MessageReaction implements Serializable {
    public static final String ENTITY_NAME = "Message_Reaction";
    public static final String TABLE_NAME = "message_reactions";
    public static final String COLUMN_ID_NAME = "id";
    public static final String COLUMN_EMOJI_NAME = "emoji";
    public static final String COLUMN_CREATEDAT_NAME = "created_at";
    private static final long serialVersionUID = 4225502309280816094L;


    private UUID id;

    private NotebookMessage message;

    private User user;

    private String emoji;

    private OffsetDateTime createdAt;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = COLUMN_ID_NAME, nullable = false)
    public UUID getId() {
        return id;
    }

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "message_id", nullable = false)
    public NotebookMessage getMessage() {
        return message;
    }

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    public User getUser() {
        return user;
    }

    @Size(max = 32)
    @NotNull
    @Column(name = COLUMN_EMOJI_NAME, nullable = false, length = 32)
    public String getEmoji() {
        return emoji;
    }

    @NotNull
    @ColumnDefault("now()")
    @Column(name = COLUMN_CREATEDAT_NAME, nullable = false)
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

}
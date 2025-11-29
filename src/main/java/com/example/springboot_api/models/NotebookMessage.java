package com.example.springboot_api.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity(name = "Notebook_Message")
@Table(name = "notebook_messages", schema = "public", indexes = {
        @Index(name = "idx_notebook_messages_notebook_created", columnList = "notebook_id, created_at"),
        @Index(name = "idx_notebook_messages_user", columnList = "user_id")
})
public class NotebookMessage implements Serializable {
    private static final long serialVersionUID = -1182561080247186497L;
    private UUID id;

    private Notebook notebook;

    private User user;

    private String type;

    private String content;

    private NotebookMessage replyToMessage;

    private Map<String, Object> aiContext;

    private OffsetDateTime createdAt;

    private Set<MessageReaction> messageReactions = new LinkedHashSet<>();

    private Set<NotebookMessage> notebookMessages = new LinkedHashSet<>();

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    public UUID getId() {
        return id;
    }

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "notebook_id", nullable = false)
    public Notebook getNotebook() {
        return notebook;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "user_id")
    public User getUser() {
        return user;
    }

    @Size(max = 50)
    @NotNull
    @Column(name = "type", nullable = false, length = 50)
    public String getType() {
        return type;
    }

    @NotNull
    @Column(name = "content", nullable = false, length = Integer.MAX_VALUE)
    public String getContent() {
        return content;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "reply_to_message_id")
    public NotebookMessage getReplyToMessage() {
        return replyToMessage;
    }

    @Column(name = "ai_context")
    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> getAiContext() {
        return aiContext;
    }

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    @OneToMany(mappedBy = "message")
    public Set<MessageReaction> getMessageReactions() {
        return messageReactions;
    }

    @OneToMany(mappedBy = "replyToMessage")
    public Set<NotebookMessage> getNotebookMessages() {
        return notebookMessages;
    }

}
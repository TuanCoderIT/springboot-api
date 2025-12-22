package com.example.springboot_api.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.Accessors;
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
@ToString(onlyExplicitlyIncluded = true)
@Accessors(chain = true)
@Entity(name = NotebookMessage.ENTITY_NAME)
@Table(name = NotebookMessage.TABLE_NAME, schema = "public", indexes = {
        @Index(name = "idx_notebook_messages_notebook_created", columnList = "notebook_id, created_at"),
        @Index(name = "idx_notebook_messages_user", columnList = "user_id")
})
public class NotebookMessage implements Serializable {
    public static final String ENTITY_NAME = "Notebook_Message";
    public static final String TABLE_NAME = "notebook_messages";
    public static final String COLUMN_ID_NAME = "id";
    public static final String COLUMN_TYPE_NAME = "type";
    public static final String COLUMN_CONTENT_NAME = "content";
    public static final String COLUMN_AICONTEXT_NAME = "ai_context";
    public static final String COLUMN_CREATEDAT_NAME = "created_at";
    private static final long serialVersionUID = -1491724199729826782L;


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
    @Column(name = COLUMN_ID_NAME, nullable = false)
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
    @Column(name = COLUMN_TYPE_NAME, nullable = false, length = 50)
    public String getType() {
        return type;
    }

    @NotNull
    @Column(name = COLUMN_CONTENT_NAME, nullable = false, length = Integer.MAX_VALUE)
    public String getContent() {
        return content;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "reply_to_message_id")
    public NotebookMessage getReplyToMessage() {
        return replyToMessage;
    }

    @Column(name = COLUMN_AICONTEXT_NAME)
    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> getAiContext() {
        return aiContext;
    }

    @NotNull
    @ColumnDefault("now()")
    @Column(name = COLUMN_CREATEDAT_NAME, nullable = false)
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
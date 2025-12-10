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
@ToString
@Entity(name = NotebookBotMessage.ENTITY_NAME)
@Table(name = NotebookBotMessage.TABLE_NAME, schema = "public", indexes = {
        @Index(name = "idx_bot_messages_notebook", columnList = "notebook_id, created_at"),
        @Index(name = "idx_bot_messages_conversation", columnList = "conversation_id, created_at")
})
public class NotebookBotMessage implements Serializable {
    public static final String ENTITY_NAME = "Notebook_Bot_Message";
    public static final String TABLE_NAME = "notebook_bot_messages";
    public static final String COLUMN_ID_NAME = "id";
    public static final String COLUMN_ROLE_NAME = "role";
    public static final String COLUMN_CONTENT_NAME = "content";
    public static final String COLUMN_MODE_NAME = "mode";
    public static final String COLUMN_CONTEXT_NAME = "context";
    public static final String COLUMN_CREATEDAT_NAME = "created_at";
    public static final String COLUMN_METADATA_NAME = "metadata";
    private static final long serialVersionUID = 1307834477887665062L;


    private UUID id;

    private Notebook notebook;

    private NotebookBotConversation conversation;

    private User user;

    private String role;

    private String content;

    private String mode;

    private Map<String, Object> context;

    private OffsetDateTime createdAt;

    private Map<String, Object> metadata;

    private LlmModel llmModel;

    private Set<NotebookBotMessageFile> notebookBotMessageFiles = new LinkedHashSet<>();

    private Set<NotebookBotMessageSource> notebookBotMessageSources = new LinkedHashSet<>();

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

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "conversation_id", nullable = false)
    public NotebookBotConversation getConversation() {
        return conversation;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "user_id")
    public User getUser() {
        return user;
    }

    @Size(max = 16)
    @NotNull
    @Column(name = COLUMN_ROLE_NAME, nullable = false, length = 16)
    public String getRole() {
        return role;
    }

    @NotNull
    @Column(name = COLUMN_CONTENT_NAME, nullable = false, length = Integer.MAX_VALUE)
    public String getContent() {
        return content;
    }

    @Size(max = 16)
    @Column(name = COLUMN_MODE_NAME, length = 16)
    public String getMode() {
        return mode;
    }

    @Column(name = COLUMN_CONTEXT_NAME)
    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> getContext() {
        return context;
    }

    @NotNull
    @ColumnDefault("now()")
    @Column(name = COLUMN_CREATEDAT_NAME, nullable = false)
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    @Column(name = COLUMN_METADATA_NAME)
    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "llm_model_id")
    public LlmModel getLlmModel() {
        return llmModel;
    }

    @OneToMany(mappedBy = "message")
    public Set<NotebookBotMessageFile> getNotebookBotMessageFiles() {
        return notebookBotMessageFiles;
    }

    @OneToMany(mappedBy = "message")
    public Set<NotebookBotMessageSource> getNotebookBotMessageSources() {
        return notebookBotMessageSources;
    }

}
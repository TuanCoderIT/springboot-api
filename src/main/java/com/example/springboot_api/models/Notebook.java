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
@Entity(name = Notebook.ENTITY_NAME)
@Table(name = Notebook.TABLE_NAME, schema = "public", indexes = {
        @Index(name = "idx_notebooks_type_visibility", columnList = "type, visibility"),
        @Index(name = "idx_notebooks_created_by", columnList = "created_by")
})
public class Notebook implements Serializable {
    public static final String ENTITY_NAME = "Notebook";
    public static final String TABLE_NAME = "notebooks";
    public static final String COLUMN_ID_NAME = "id";
    public static final String COLUMN_TITLE_NAME = "title";
    public static final String COLUMN_DESCRIPTION_NAME = "description";
    public static final String COLUMN_TYPE_NAME = "type";
    public static final String COLUMN_VISIBILITY_NAME = "visibility";
    public static final String COLUMN_THUMBNAILURL_NAME = "thumbnail_url";
    public static final String COLUMN_METADATA_NAME = "metadata";
    public static final String COLUMN_CREATEDAT_NAME = "created_at";
    public static final String COLUMN_UPDATEDAT_NAME = "updated_at";
    private static final long serialVersionUID = 2552482617490998989L;


    private UUID id;

    private String title;

    private String description;

    private String type;

    private String visibility;

    private User createdBy;

    private String thumbnailUrl;

    private Map<String, Object> metadata;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    private Set<AiTask> aiTasks = new LinkedHashSet<>();

    private Set<FileChunk> fileChunks = new LinkedHashSet<>();

    private Set<Flashcard> flashcards = new LinkedHashSet<>();

    private Set<NotebookActivityLog> notebookActivityLogs = new LinkedHashSet<>();

    private Set<NotebookBotConversationState> notebookBotConversationStates = new LinkedHashSet<>();

    private Set<NotebookBotConversation> notebookBotConversations = new LinkedHashSet<>();

    private Set<NotebookBotMessage> notebookBotMessages = new LinkedHashSet<>();

    private Set<NotebookFile> notebookFiles = new LinkedHashSet<>();

    private Set<NotebookMember> notebookMembers = new LinkedHashSet<>();

    private Set<NotebookMessage> notebookMessages = new LinkedHashSet<>();

    private Set<NotebookQuizz> notebookQuizzes = new LinkedHashSet<>();

    private Set<TtsAsset> ttsAssets = new LinkedHashSet<>();

    private Set<VideoAsset> videoAssets = new LinkedHashSet<>();

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = COLUMN_ID_NAME, nullable = false)
    public UUID getId() {
        return id;
    }

    @Size(max = 255)
    @NotNull
    @Column(name = COLUMN_TITLE_NAME, nullable = false)
    public String getTitle() {
        return title;
    }

    @Column(name = COLUMN_DESCRIPTION_NAME, length = Integer.MAX_VALUE)
    public String getDescription() {
        return description;
    }

    @Size(max = 50)
    @NotNull
    @Column(name = COLUMN_TYPE_NAME, nullable = false, length = 50)
    public String getType() {
        return type;
    }

    @Size(max = 50)
    @NotNull
    @Column(name = COLUMN_VISIBILITY_NAME, nullable = false, length = 50)
    public String getVisibility() {
        return visibility;
    }

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "created_by", nullable = false)
    public User getCreatedBy() {
        return createdBy;
    }

    @Column(name = COLUMN_THUMBNAILURL_NAME, length = Integer.MAX_VALUE)
    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    @Column(name = COLUMN_METADATA_NAME)
    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @NotNull
    @ColumnDefault("now()")
    @Column(name = COLUMN_CREATEDAT_NAME, nullable = false)
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    @NotNull
    @ColumnDefault("now()")
    @Column(name = COLUMN_UPDATEDAT_NAME, nullable = false)
    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    @OneToMany(mappedBy = "notebook")
    public Set<AiTask> getAiTasks() {
        return aiTasks;
    }

    @OneToMany(mappedBy = "notebook")
    public Set<FileChunk> getFileChunks() {
        return fileChunks;
    }

    @OneToMany(mappedBy = "notebook")
    public Set<Flashcard> getFlashcards() {
        return flashcards;
    }

    @OneToMany(mappedBy = "notebook")
    public Set<NotebookActivityLog> getNotebookActivityLogs() {
        return notebookActivityLogs;
    }

    @OneToMany(mappedBy = "notebook")
    public Set<NotebookBotConversationState> getNotebookBotConversationStates() {
        return notebookBotConversationStates;
    }

    @OneToMany(mappedBy = "notebook")
    public Set<NotebookBotConversation> getNotebookBotConversations() {
        return notebookBotConversations;
    }

    @OneToMany(mappedBy = "notebook")
    public Set<NotebookBotMessage> getNotebookBotMessages() {
        return notebookBotMessages;
    }

    @OneToMany(mappedBy = "notebook")
    public Set<NotebookFile> getNotebookFiles() {
        return notebookFiles;
    }

    @OneToMany(mappedBy = "notebook")
    public Set<NotebookMember> getNotebookMembers() {
        return notebookMembers;
    }

    @OneToMany(mappedBy = "notebook")
    public Set<NotebookMessage> getNotebookMessages() {
        return notebookMessages;
    }

    @OneToMany(mappedBy = "notebook")
    public Set<NotebookQuizz> getNotebookQuizzes() {
        return notebookQuizzes;
    }

    @OneToMany(mappedBy = "notebook")
    public Set<TtsAsset> getTtsAssets() {
        return ttsAssets;
    }

    @OneToMany(mappedBy = "notebook")
    public Set<VideoAsset> getVideoAssets() {
        return videoAssets;
    }

}
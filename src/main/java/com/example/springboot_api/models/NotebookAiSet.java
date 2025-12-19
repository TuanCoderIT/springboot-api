package com.example.springboot_api.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
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
@Entity(name = NotebookAiSet.ENTITY_NAME)
@Table(name = NotebookAiSet.TABLE_NAME, schema = "public")
public class NotebookAiSet implements Serializable {
    public static final String ENTITY_NAME = "Notebook_Ai_Set";
    public static final String TABLE_NAME = "notebook_ai_sets";
    public static final String COLUMN_ID_NAME = "id";
    public static final String COLUMN_SETTYPE_NAME = "set_type";
    public static final String COLUMN_STATUS_NAME = "status";
    public static final String COLUMN_ERRORMESSAGE_NAME = "error_message";
    public static final String COLUMN_MODELCODE_NAME = "model_code";
    public static final String COLUMN_PROVIDER_NAME = "provider";
    public static final String COLUMN_TITLE_NAME = "title";
    public static final String COLUMN_DESCRIPTION_NAME = "description";
    public static final String COLUMN_INPUTCONFIG_NAME = "input_config";
    public static final String COLUMN_OUTPUTSTATS_NAME = "output_stats";
    public static final String COLUMN_METADATA_NAME = "metadata";
    public static final String COLUMN_CREATEDAT_NAME = "created_at";
    public static final String COLUMN_STARTEDAT_NAME = "started_at";
    public static final String COLUMN_FINISHEDAT_NAME = "finished_at";
    public static final String COLUMN_UPDATEDAT_NAME = "updated_at";
    private static final long serialVersionUID = -560452432615436163L;


    private UUID id;

    private Notebook notebook;

    private User createdBy;

    private String setType;

    private String status;

    private String errorMessage;

    private String modelCode;

    private String provider;

    private LlmModel llmModel;

    private String title;

    private String description;

    private Map<String, Object> inputConfig;

    private Map<String, Object> outputStats;

    private Map<String, Object> metadata;

    private OffsetDateTime createdAt;

    private OffsetDateTime startedAt;

    private OffsetDateTime finishedAt;

    private OffsetDateTime updatedAt;

    private Set<Flashcard> flashcards = new LinkedHashSet<>();

    private Set<NotebookAiSetFile> notebookAiSetFiles = new LinkedHashSet<>();

    private Set<NotebookAiSetSuggestion> notebookAiSetSuggestions = new LinkedHashSet<>();

    private NotebookAiSummary notebookAiSummary;

    private Set<NotebookMindmap> notebookMindmaps = new LinkedHashSet<>();

    private Set<NotebookQuizz> notebookQuizzes = new LinkedHashSet<>();

    private Set<TtsAsset> ttsAssets = new LinkedHashSet<>();

    private Set<VideoAsset> videoAssets = new LinkedHashSet<>();

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
    @JoinColumn(name = "created_by")
    public User getCreatedBy() {
        return createdBy;
    }

    @NotNull
    @Column(name = COLUMN_SETTYPE_NAME, nullable = false, length = Integer.MAX_VALUE)
    public String getSetType() {
        return setType;
    }

    @NotNull
    @ColumnDefault("'queued'")
    @Column(name = COLUMN_STATUS_NAME, nullable = false, length = Integer.MAX_VALUE)
    public String getStatus() {
        return status;
    }

    @Column(name = COLUMN_ERRORMESSAGE_NAME, length = Integer.MAX_VALUE)
    public String getErrorMessage() {
        return errorMessage;
    }

    @Column(name = COLUMN_MODELCODE_NAME, length = Integer.MAX_VALUE)
    public String getModelCode() {
        return modelCode;
    }

    @Column(name = COLUMN_PROVIDER_NAME, length = Integer.MAX_VALUE)
    public String getProvider() {
        return provider;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "llm_model_id")
    public LlmModel getLlmModel() {
        return llmModel;
    }

    @Column(name = COLUMN_TITLE_NAME, length = Integer.MAX_VALUE)
    public String getTitle() {
        return title;
    }

    @Column(name = COLUMN_DESCRIPTION_NAME, length = Integer.MAX_VALUE)
    public String getDescription() {
        return description;
    }

    @Column(name = COLUMN_INPUTCONFIG_NAME)
    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> getInputConfig() {
        return inputConfig;
    }

    @Column(name = COLUMN_OUTPUTSTATS_NAME)
    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> getOutputStats() {
        return outputStats;
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

    @Column(name = COLUMN_STARTEDAT_NAME)
    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    @Column(name = COLUMN_FINISHEDAT_NAME)
    public OffsetDateTime getFinishedAt() {
        return finishedAt;
    }

    @Column(name = COLUMN_UPDATEDAT_NAME)
    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    @OneToMany(mappedBy = "notebookAiSets")
    public Set<Flashcard> getFlashcards() {
        return flashcards;
    }

    @OneToMany(mappedBy = "aiSet")
    public Set<NotebookAiSetFile> getNotebookAiSetFiles() {
        return notebookAiSetFiles;
    }

    @OneToMany(mappedBy = "notebookAiSet")
    public Set<NotebookAiSetSuggestion> getNotebookAiSetSuggestions() {
        return notebookAiSetSuggestions;
    }

    @OneToOne(mappedBy = "notebookAiSets")
    public NotebookAiSummary getNotebookAiSummary() {
        return notebookAiSummary;
    }

    @OneToMany(mappedBy = "sourceAiSet")
    public Set<NotebookMindmap> getNotebookMindmaps() {
        return notebookMindmaps;
    }

    @OneToMany(mappedBy = "notebookAiSets")
    public Set<NotebookQuizz> getNotebookQuizzes() {
        return notebookQuizzes;
    }

    @OneToMany(mappedBy = "notebookAiSets")
    public Set<TtsAsset> getTtsAssets() {
        return ttsAssets;
    }

    @OneToMany(mappedBy = "notebookAiSets")
    public Set<VideoAsset> getVideoAssets() {
        return videoAssets;
    }

}
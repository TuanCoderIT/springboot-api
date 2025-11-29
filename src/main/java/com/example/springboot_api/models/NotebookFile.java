package com.example.springboot_api.models;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity(name = "Notebook_File")
@Table(name = "notebook_files", schema = "public", indexes = {
        @Index(name = "idx_notebook_files_notebook", columnList = "notebook_id"),
        @Index(name = "idx_notebook_files_uploaded_by", columnList = "uploaded_by"),
        @Index(name = "idx_notebook_files_status", columnList = "status")
})
public class NotebookFile implements Serializable {
    private static final long serialVersionUID = 5942172068451284162L;
    private UUID id;

    private Notebook notebook;

    private User uploadedBy;

    private String originalFilename;

    private String mimeType;

    private Long fileSize;

    private String storageUrl;

    private String status;

    private Integer pagesCount;

    private Boolean ocrDone = false;

    private Boolean embeddingDone = false;

    private Map<String, Object> extraMetadata;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    private Set<AiTask> aiTasks = new LinkedHashSet<>();

    private Set<FileChunk> fileChunks = new LinkedHashSet<>();

    private Set<FilePage> filePages = new LinkedHashSet<>();

    private Set<Flashcard> flashcards = new LinkedHashSet<>();

    private Set<Quiz> quizzes = new LinkedHashSet<>();

    private Set<TtsAsset> ttsAssets = new LinkedHashSet<>();

    private Set<VideoAsset> videoAssets = new LinkedHashSet<>();

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

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "uploaded_by", nullable = false)
    public User getUploadedBy() {
        return uploadedBy;
    }

    @NotNull
    @Column(name = "original_filename", nullable = false, length = Integer.MAX_VALUE)
    public String getOriginalFilename() {
        return originalFilename;
    }

    @Size(max = 255)
    @Column(name = "mime_type")
    public String getMimeType() {
        return mimeType;
    }

    @Column(name = "file_size")
    public Long getFileSize() {
        return fileSize;
    }

    @NotNull
    @Column(name = "storage_url", nullable = false, length = Integer.MAX_VALUE)
    public String getStorageUrl() {
        return storageUrl;
    }

    @Size(max = 50)
    @NotNull
    @Column(name = "status", nullable = false, length = 50)
    public String getStatus() {
        return status;
    }

    @Column(name = "pages_count")
    public Integer getPagesCount() {
        return pagesCount;
    }

    @NotNull
    @ColumnDefault("false")
    @Column(name = "ocr_done", nullable = false)
    public Boolean getOcrDone() {
        return ocrDone;
    }

    @NotNull
    @ColumnDefault("false")
    @Column(name = "embedding_done", nullable = false)
    public Boolean getEmbeddingDone() {
        return embeddingDone;
    }

    @Column(name = "extra_metadata")
    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> getExtraMetadata() {
        return extraMetadata;
    }

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "updated_at", nullable = false)
    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    @OneToMany(mappedBy = "file")
    public Set<AiTask> getAiTasks() {
        return aiTasks;
    }

    @OneToMany(mappedBy = "file")
    public Set<FileChunk> getFileChunks() {
        return fileChunks;
    }

    @OneToMany(mappedBy = "file")
    public Set<FilePage> getFilePages() {
        return filePages;
    }

    @OneToMany(mappedBy = "file")
    public Set<Flashcard> getFlashcards() {
        return flashcards;
    }

    @OneToMany(mappedBy = "file")
    public Set<Quiz> getQuizzes() {
        return quizzes;
    }

    @OneToMany(mappedBy = "file")
    public Set<TtsAsset> getTtsAssets() {
        return ttsAssets;
    }

    @OneToMany(mappedBy = "file")
    public Set<VideoAsset> getVideoAssets() {
        return videoAssets;
    }

}
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
@Entity(name = NotebookFile.ENTITY_NAME)
@Table(name = NotebookFile.TABLE_NAME, schema = "public", indexes = {
        @Index(name = "idx_notebook_files_notebook", columnList = "notebook_id"),
        @Index(name = "idx_notebook_files_uploaded_by", columnList = "uploaded_by"),
        @Index(name = "idx_notebook_files_status", columnList = "status")
})
public class NotebookFile implements Serializable {
    public static final String ENTITY_NAME = "Notebook_File";
    public static final String TABLE_NAME = "notebook_files";
    public static final String COLUMN_ID_NAME = "id";
    public static final String COLUMN_ORIGINALFILENAME_NAME = "original_filename";
    public static final String COLUMN_MIMETYPE_NAME = "mime_type";
    public static final String COLUMN_FILESIZE_NAME = "file_size";
    public static final String COLUMN_STORAGEURL_NAME = "storage_url";
    public static final String COLUMN_STATUS_NAME = "status";
    public static final String COLUMN_PAGESCOUNT_NAME = "pages_count";
    public static final String COLUMN_OCRDONE_NAME = "ocr_done";
    public static final String COLUMN_EMBEDDINGDONE_NAME = "embedding_done";
    public static final String COLUMN_EXTRAMETADATA_NAME = "extra_metadata";
    public static final String COLUMN_CREATEDAT_NAME = "created_at";
    public static final String COLUMN_UPDATEDAT_NAME = "updated_at";
    private static final long serialVersionUID = -4660902087269919238L;


    private UUID id;

    private Notebook notebook;

    private User uploadedBy;

    private String originalFilename;

    private String mimeType;

    private Long fileSize;

    private String storageUrl;

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
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "uploaded_by", nullable = false)
    public User getUploadedBy() {
        return uploadedBy;
    }

    @NotNull
    @Column(name = COLUMN_ORIGINALFILENAME_NAME, nullable = false, length = Integer.MAX_VALUE)
    public String getOriginalFilename() {
        return originalFilename;
    }

    @Size(max = 255)
    @Column(name = COLUMN_MIMETYPE_NAME)
    public String getMimeType() {
        return mimeType;
    }

    @Column(name = COLUMN_FILESIZE_NAME)
    public Long getFileSize() {
        return fileSize;
    }

    @NotNull
    @Column(name = COLUMN_STORAGEURL_NAME, nullable = false, length = Integer.MAX_VALUE)
    public String getStorageUrl() {
        return storageUrl;
    }

    @Column(name = COLUMN_PAGESCOUNT_NAME)
    public Integer getPagesCount() {
        return pagesCount;
    }

    @NotNull
    @ColumnDefault("false")
    @Column(name = COLUMN_OCRDONE_NAME, nullable = false)
    public Boolean getOcrDone() {
        return ocrDone;
    }

    @NotNull
    @ColumnDefault("false")
    @Column(name = COLUMN_EMBEDDINGDONE_NAME, nullable = false)
    public Boolean getEmbeddingDone() {
        return embeddingDone;
    }

    @Column(name = COLUMN_EXTRAMETADATA_NAME)
    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> getExtraMetadata() {
        return extraMetadata;
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

/*
 TODO [Reverse Engineering] create field to map the 'status' column
 Available actions: Define target Java type | Uncomment as is | Remove column mapping
    private Object status;
*/
}
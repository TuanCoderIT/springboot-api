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
    public static final String COLUMN_CHUNKSIZE_NAME = "chunk_size";
    public static final String COLUMN_CHUNKOVERLAP_NAME = "chunk_overlap";
    private static final long serialVersionUID = -1804503772143418487L;


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

    private Integer chunkSize;

    private Integer chunkOverlap;

    private Set<AiTask> aiTasks = new LinkedHashSet<>();

    private Set<FileChunk> fileChunks = new LinkedHashSet<>();

    private Set<FlashcardFile> flashcardFiles = new LinkedHashSet<>();

    private Set<QuizFile> quizFiles = new LinkedHashSet<>();

    private Set<TtsFile> ttsFiles = new LinkedHashSet<>();

    private Set<VideoAssetFile> videoAssetFiles = new LinkedHashSet<>();

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

    @Size(max = 50)
    @NotNull
    @Column(name = COLUMN_STATUS_NAME, nullable = false, length = 50)
    public String getStatus() {
        return status;
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

    @ColumnDefault("800")
    @Column(name = COLUMN_CHUNKSIZE_NAME)
    public Integer getChunkSize() {
        return chunkSize;
    }

    @ColumnDefault("120")
    @Column(name = COLUMN_CHUNKOVERLAP_NAME)
    public Integer getChunkOverlap() {
        return chunkOverlap;
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
    public Set<FlashcardFile> getFlashcardFiles() {
        return flashcardFiles;
    }

    @OneToMany(mappedBy = "file")
    public Set<QuizFile> getQuizFiles() {
        return quizFiles;
    }

    @OneToMany(mappedBy = "file")
    public Set<TtsFile> getTtsFiles() {
        return ttsFiles;
    }

    @OneToMany(mappedBy = "file")
    public Set<VideoAssetFile> getVideoAssetFiles() {
        return videoAssetFiles;
    }

}
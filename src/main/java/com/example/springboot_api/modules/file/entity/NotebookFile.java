package com.example.springboot_api.modules.file.entity;

import com.example.springboot_api.modules.ai_task.entity.AiTask;
import com.example.springboot_api.modules.asset.entity.TtsAsset;
import com.example.springboot_api.modules.asset.entity.VideoAsset;
import com.example.springboot_api.modules.auth.entity.User;
import com.example.springboot_api.modules.chunk.entity.FileChunk;
import com.example.springboot_api.modules.flashcard.entity.Flashcard;
import com.example.springboot_api.modules.notebook.entity.Notebook;
import com.example.springboot_api.modules.page.entity.FilePage;
import com.example.springboot_api.modules.quiz.entity.Quiz;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "notebook_files")
public class NotebookFile {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "notebook_id", nullable = false)
    private Notebook notebook;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @NotNull
    @Column(name = "original_filename", nullable = false, length = Integer.MAX_VALUE)
    private String originalFilename;

    @Size(max = 255)
    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "file_size")
    private Long fileSize;

    @NotNull
    @Column(name = "storage_url", nullable = false, length = Integer.MAX_VALUE)
    private String storageUrl;

    @Column(name = "pages_count")
    private Integer pagesCount;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "ocr_done", nullable = false)
    private Boolean ocrDone = false;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "embedding_done", nullable = false)
    private Boolean embeddingDone = false;

    @Column(name = "extra_metadata")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> extraMetadata;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "file")
    private Set<AiTask> aiTasks = new LinkedHashSet<>();

    @OneToMany(mappedBy = "file")
    private Set<FileChunk> fileChunks = new LinkedHashSet<>();

    @OneToMany(mappedBy = "file")
    private Set<FilePage> filePages = new LinkedHashSet<>();

    @OneToMany(mappedBy = "file")
    private Set<Flashcard> flashcards = new LinkedHashSet<>();

    @OneToMany(mappedBy = "file")
    private Set<Quiz> quizzes = new LinkedHashSet<>();

    @OneToMany(mappedBy = "file")
    private Set<TtsAsset> ttsAssets = new LinkedHashSet<>();
    @OneToMany(mappedBy = "file")
    private Set<VideoAsset> videoAssets = new LinkedHashSet<>();

/*
 TODO [Reverse Engineering] create field to map the 'status' column
 Available actions: Define target Java type | Uncomment as is | Remove column mapping
    @ColumnDefault("'pending'")
    @Column(name = "status", columnDefinition = "notebook_file_status not null")
    private Object status;
*/
}
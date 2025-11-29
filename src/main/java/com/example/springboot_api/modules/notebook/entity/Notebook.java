package com.example.springboot_api.modules.notebook.entity;

import com.example.springboot_api.modules.activity.entity.NotebookActivityLog;
import com.example.springboot_api.modules.ai_task.entity.AiTask;
import com.example.springboot_api.modules.asset.entity.TtsAsset;
import com.example.springboot_api.modules.asset.entity.VideoAsset;
import com.example.springboot_api.modules.auth.entity.User;
import com.example.springboot_api.modules.chunk.entity.FileChunk;
import com.example.springboot_api.modules.file.entity.NotebookFile;
import com.example.springboot_api.modules.member.entity.NotebookMember;
import com.example.springboot_api.modules.notebook.entity.enums.NotebookType;
import com.example.springboot_api.modules.notebook.entity.enums.NotebookVisibility;
import com.example.springboot_api.modules.chat.entity.NotebookMessage;
import com.example.springboot_api.modules.flashcard.entity.Flashcard;
import com.example.springboot_api.modules.quiz.entity.Quiz;
import com.example.springboot_api.modules.rag.entity.RagQuery;
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
@Table(name = "notebooks")
public class Notebook {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Size(max = 255)
    @NotNull
    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", length = Integer.MAX_VALUE)
    private String description;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "thumbnail_url", length = Integer.MAX_VALUE)
    private String thumbnailUrl;

    @Column(name = "metadata")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> metadata;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // enum mapping
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private NotebookType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false)
    private NotebookVisibility visibility;

    // liên kết
    @OneToMany(mappedBy = "notebook")
    private Set<AiTask> aiTasks = new LinkedHashSet<>();

    @OneToMany(mappedBy = "notebook")
    private Set<FileChunk> fileChunks = new LinkedHashSet<>();

    @OneToMany(mappedBy = "notebook")
    private Set<Flashcard> flashcards = new LinkedHashSet<>();

    @OneToMany(mappedBy = "notebook")
    private Set<NotebookActivityLog> notebookActivityLogs = new LinkedHashSet<>();

    @OneToMany(mappedBy = "notebook")
    private Set<NotebookFile> notebookFiles = new LinkedHashSet<>();

    @OneToMany(mappedBy = "notebook")
    private Set<NotebookMember> notebookMembers = new LinkedHashSet<>();

    @OneToMany(mappedBy = "notebook")
    private Set<NotebookMessage> notebookMessages = new LinkedHashSet<>();

    @OneToMany(mappedBy = "notebook")
    private Set<Quiz> quizzes = new LinkedHashSet<>();
    @OneToMany(mappedBy = "notebook")
    private Set<TtsAsset> ttsAssets = new LinkedHashSet<>();

    @OneToMany(mappedBy = "notebook")
    private Set<RagQuery> ragQueries = new LinkedHashSet<>();
    @OneToMany(mappedBy = "notebook")
    private Set<VideoAsset> videoAssets = new LinkedHashSet<>();

    /*
     * TODO [Reverse Engineering] create field to map the 'type' column
     * Available actions: Define target Java type | Uncomment as is | Remove column
     * mapping
     * 
     * @ColumnDefault("'personal'")
     * 
     * @Column(name = "type", columnDefinition = "notebook_type not null")
     * private Object type;
     */
    /*
     * TODO [Reverse Engineering] create field to map the 'visibility' column
     * Available actions: Define target Java type | Uncomment as is | Remove column
     * mapping
     * 
     * @ColumnDefault("'private'")
     * 
     * @Column(name = "visibility", columnDefinition =
     * "notebook_visibility not null")
     * private Object visibility;
     */
}
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
@Entity(name = NotebookQuizz.ENTITY_NAME)
@Table(name = NotebookQuizz.TABLE_NAME, schema = "public")
public class NotebookQuizz implements Serializable {
    public static final String ENTITY_NAME = "Notebook_Quizz";
    public static final String TABLE_NAME = "notebook_quizzes";
    public static final String COLUMN_ID_NAME = "id";
    public static final String COLUMN_QUESTION_NAME = "question";
    public static final String COLUMN_EXPLANATION_NAME = "explanation";
    public static final String COLUMN_DIFFICULTYLEVEL_NAME = "difficulty_level";
    public static final String COLUMN_EMBEDDING_NAME = "embedding";
    public static final String COLUMN_METADATA_NAME = "metadata";
    public static final String COLUMN_CREATEDAT_NAME = "created_at";
    private static final long serialVersionUID = 9067903996967890555L;


    private UUID id;

    private Notebook notebook;

    private String question;

    private String explanation;

    private Short difficultyLevel;

    private User createdBy;

    private Map<String, Object> metadata;

    private OffsetDateTime createdAt;

    private Set<NotebookQuizFile> notebookQuizFiles = new LinkedHashSet<>();
    private Set<NotebookQuizOption> notebookQuizOptions = new LinkedHashSet<>();

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
    @Column(name = COLUMN_QUESTION_NAME, nullable = false, length = Integer.MAX_VALUE)
    public String getQuestion() {
        return question;
    }

    @Column(name = COLUMN_EXPLANATION_NAME, length = Integer.MAX_VALUE)
    public String getExplanation() {
        return explanation;
    }

    @Column(name = COLUMN_DIFFICULTYLEVEL_NAME)
    public Short getDifficultyLevel() {
        return difficultyLevel;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "created_by")
    public User getCreatedBy() {
        return createdBy;
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

    @OneToMany(mappedBy = "quiz")
    public Set<NotebookQuizFile> getNotebookQuizFiles() {
        return notebookQuizFiles;
    }

    @OneToMany(mappedBy = "quiz")
    public Set<NotebookQuizOption> getNotebookQuizOptions() {
        return notebookQuizOptions;
    }

/*
 TODO [Reverse Engineering] create field to map the 'embedding' column
 Available actions: Define target Java type | Uncomment as is | Remove column mapping
    private Object embedding;
*/
}
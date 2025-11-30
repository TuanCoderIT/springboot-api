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
@Entity(name = Quiz.ENTITY_NAME)
@Table(name = Quiz.TABLE_NAME)
public class Quiz implements Serializable {
    public static final String ENTITY_NAME = "Quiz";
    public static final String TABLE_NAME = "quizzes";
    public static final String COLUMN_ID_NAME = "id";
    public static final String COLUMN_TITLE_NAME = "title";
    public static final String COLUMN_METADATA_NAME = "metadata";
    public static final String COLUMN_CREATEDAT_NAME = "created_at";
    private static final long serialVersionUID = 7290977021731423992L;


    private UUID id;

    private Notebook notebook;

    private String title;

    private User createdBy;

    private Map<String, Object> metadata;

    private OffsetDateTime createdAt;

    private Set<QuizFile> quizFiles = new LinkedHashSet<>();

    private Set<QuizQuestion> quizQuestions = new LinkedHashSet<>();

    private Set<QuizSubmission> quizSubmissions = new LinkedHashSet<>();

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

    @Size(max = 255)
    @NotNull
    @Column(name = COLUMN_TITLE_NAME, nullable = false)
    public String getTitle() {
        return title;
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
    public Set<QuizFile> getQuizFiles() {
        return quizFiles;
    }

    @OneToMany(mappedBy = "quiz")
    public Set<QuizQuestion> getQuizQuestions() {
        return quizQuestions;
    }

    @OneToMany(mappedBy = "quiz")
    public Set<QuizSubmission> getQuizSubmissions() {
        return quizSubmissions;
    }

}
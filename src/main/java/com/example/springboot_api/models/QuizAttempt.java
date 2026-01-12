package com.example.springboot_api.models;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.CascadeType;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * Entity lưu lịch sử làm quiz của user.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@Accessors(chain = true)
@Entity(name = QuizAttempt.ENTITY_NAME)
@Table(name = QuizAttempt.TABLE_NAME, schema = "public", indexes = {
        @Index(name = "idx_quiz_attempts_user", columnList = "user_id"),
        @Index(name = "idx_quiz_attempts_ai_set", columnList = "notebook_ai_set_id")
})
public class QuizAttempt implements Serializable {

    public static final String ENTITY_NAME = "QuizAttempt";
    public static final String TABLE_NAME = "quiz_attempts";
    private static final long serialVersionUID = 1L;

    private UUID id;
    private User user;
    private NotebookAiSet notebookAiSet;

    private Integer score;
    private Integer totalQuestions;
    private Integer correctCount;
    private Integer timeSpentSeconds;

    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;
    private OffsetDateTime createdAt;

    private Map<String, Object> analysisJson;

    private List<QuizAttemptAnswer> answers;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    public UUID getId() {
        return id;
    }

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    public User getUser() {
        return user;
    }

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "notebook_ai_set_id", nullable = false)
    public NotebookAiSet getNotebookAiSet() {
        return notebookAiSet;
    }

    @Column(name = "score")
    public Integer getScore() {
        return score;
    }

    @Column(name = "total_questions")
    public Integer getTotalQuestions() {
        return totalQuestions;
    }

    @Column(name = "correct_count")
    public Integer getCorrectCount() {
        return correctCount;
    }

    @Column(name = "time_spent_seconds")
    public Integer getTimeSpentSeconds() {
        return timeSpentSeconds;
    }

    @Column(name = "started_at")
    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    @Column(name = "finished_at")
    public OffsetDateTime getFinishedAt() {
        return finishedAt;
    }

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    @Column(name = "analysis_json")
    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> getAnalysisJson() {
        return analysisJson;
    }

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<QuizAttemptAnswer> getAnswers() {
        return answers;
    }
}

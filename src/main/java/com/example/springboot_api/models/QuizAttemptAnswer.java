package com.example.springboot_api.models;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
 * Entity lưu chi tiết từng câu trả lời trong một lần làm quiz.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@Accessors(chain = true)
@Entity(name = QuizAttemptAnswer.ENTITY_NAME)
@Table(name = QuizAttemptAnswer.TABLE_NAME, schema = "public", indexes = {
        @Index(name = "idx_quiz_attempt_answers_attempt", columnList = "attempt_id")
})
public class QuizAttemptAnswer implements Serializable {

    public static final String ENTITY_NAME = "QuizAttemptAnswer";
    public static final String TABLE_NAME = "quiz_attempt_answers";
    private static final long serialVersionUID = 1L;

    private UUID id;
    private QuizAttempt attempt;
    private NotebookQuizz quiz;
    private NotebookQuizOption selectedOption;
    private Boolean isCorrect;
    private OffsetDateTime createdAt;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    public UUID getId() {
        return id;
    }

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "attempt_id", nullable = false)
    public QuizAttempt getAttempt() {
        return attempt;
    }

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "quiz_id", nullable = false)
    public NotebookQuizz getQuiz() {
        return quiz;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "selected_option_id")
    public NotebookQuizOption getSelectedOption() {
        return selectedOption;
    }

    @NotNull
    @ColumnDefault("false")
    @Column(name = "is_correct", nullable = false)
    public Boolean getIsCorrect() {
        return isCorrect;
    }

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}

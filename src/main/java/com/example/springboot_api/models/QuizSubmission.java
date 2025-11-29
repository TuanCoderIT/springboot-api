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
import java.util.Map;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity(name = "Quiz_Submission")
@Table(name = "quiz_submissions", schema = "public", indexes = {
        @Index(name = "idx_quiz_submissions_quiz", columnList = "quiz_id, created_at"),
        @Index(name = "idx_quiz_submissions_user", columnList = "user_id, created_at")
})
public class QuizSubmission implements Serializable {
    private static final long serialVersionUID = 7618378689261539878L;
    private UUID id;

    private Quiz quiz;

    private User user;

    private Double score;

    private Map<String, Object> answers;

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
    @JoinColumn(name = "quiz_id", nullable = false)
    public Quiz getQuiz() {
        return quiz;
    }

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    public User getUser() {
        return user;
    }

    @Column(name = "score")
    public Double getScore() {
        return score;
    }

    @Column(name = "answers")
    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> getAnswers() {
        return answers;
    }

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

}
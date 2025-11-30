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
@ToString
@Entity(name = QuizSubmission.ENTITY_NAME)
@Table(name = QuizSubmission.TABLE_NAME)
public class QuizSubmission implements Serializable {
    public static final String ENTITY_NAME = "Quiz_Submission";
    public static final String TABLE_NAME = "quiz_submissions";
    public static final String COLUMN_ID_NAME = "id";
    public static final String COLUMN_SCORE_NAME = "score";
    public static final String COLUMN_ANSWERS_NAME = "answers";
    public static final String COLUMN_CREATEDAT_NAME = "created_at";
    private static final long serialVersionUID = 1644625225971325846L;


    private UUID id;

    private Quiz quiz;

    private User user;

    private Double score;

    private Map<String, Object> answers;

    private OffsetDateTime createdAt;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = COLUMN_ID_NAME, nullable = false)
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

    @Column(name = COLUMN_SCORE_NAME)
    public Double getScore() {
        return score;
    }

    @Column(name = COLUMN_ANSWERS_NAME)
    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> getAnswers() {
        return answers;
    }

    @NotNull
    @ColumnDefault("now()")
    @Column(name = COLUMN_CREATEDAT_NAME, nullable = false)
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

}
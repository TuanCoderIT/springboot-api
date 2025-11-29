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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity(name = "Quiz_Question")
@Table(name = "quiz_questions", schema = "public", indexes = {
        @Index(name = "idx_quiz_questions_quiz", columnList = "quiz_id")
})
public class QuizQuestion implements Serializable {
    private static final long serialVersionUID = 8852590243672807604L;
    private UUID id;

    private Quiz quiz;

    private String questionText;

    private String questionType;

    private Map<String, Object> metadata;

    private Set<QuizOption> quizOptions = new LinkedHashSet<>();

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
    @Column(name = "question_text", nullable = false, length = Integer.MAX_VALUE)
    public String getQuestionText() {
        return questionText;
    }

    @Size(max = 32)
    @NotNull
    @ColumnDefault("'multiple_choice'")
    @Column(name = "question_type", nullable = false, length = 32)
    public String getQuestionType() {
        return questionType;
    }

    @Column(name = "metadata")
    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @OneToMany(mappedBy = "question")
    public Set<QuizOption> getQuizOptions() {
        return quizOptions;
    }

}
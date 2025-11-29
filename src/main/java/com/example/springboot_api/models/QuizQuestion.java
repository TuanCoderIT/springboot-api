package com.example.springboot_api.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.Accessors;
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
@ToString(onlyExplicitlyIncluded = true)
@Accessors(chain = true)
@Entity(name = QuizQuestion.ENTITY_NAME)
@Table(name = QuizQuestion.TABLE_NAME, schema = "public", indexes = {
        @Index(name = "idx_quiz_questions_quiz", columnList = "quiz_id")
})
public class QuizQuestion implements Serializable {
    public static final String ENTITY_NAME = "Quiz_Question";
    public static final String TABLE_NAME = "quiz_questions";
    public static final String COLUMN_ID_NAME = "id";
    public static final String COLUMN_QUESTIONTEXT_NAME = "question_text";
    public static final String COLUMN_QUESTIONTYPE_NAME = "question_type";
    public static final String COLUMN_METADATA_NAME = "metadata";
    private static final long serialVersionUID = -7892795159350329277L;


    private UUID id;

    private Quiz quiz;

    private String questionText;

    private String questionType;

    private Map<String, Object> metadata;

    private Set<QuizOption> quizOptions = new LinkedHashSet<>();

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
    @Column(name = COLUMN_QUESTIONTEXT_NAME, nullable = false, length = Integer.MAX_VALUE)
    public String getQuestionText() {
        return questionText;
    }

    @Size(max = 32)
    @NotNull
    @ColumnDefault("'multiple_choice'")
    @Column(name = COLUMN_QUESTIONTYPE_NAME, nullable = false, length = 32)
    public String getQuestionType() {
        return questionType;
    }

    @Column(name = COLUMN_METADATA_NAME)
    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @OneToMany(mappedBy = "question")
    public Set<QuizOption> getQuizOptions() {
        return quizOptions;
    }

}
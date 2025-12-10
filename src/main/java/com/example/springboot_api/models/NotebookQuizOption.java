package com.example.springboot_api.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity(name = NotebookQuizOption.ENTITY_NAME)
@Table(name = NotebookQuizOption.TABLE_NAME, schema = "public")
public class NotebookQuizOption implements Serializable {
    public static final String ENTITY_NAME = "Notebook_Quiz_Option";
    public static final String TABLE_NAME = "notebook_quiz_options";
    public static final String COLUMN_ID_NAME = "id";
    public static final String COLUMN_TEXT_NAME = "text";
    public static final String COLUMN_ISCORRECT_NAME = "is_correct";
    public static final String COLUMN_FEEDBACK_NAME = "feedback";
    public static final String COLUMN_POSITION_NAME = "\"position\"";
    public static final String COLUMN_CREATEDAT_NAME = "created_at";
    private static final long serialVersionUID = -6592381505567713276L;


    private UUID id;

    private NotebookQuizz quiz;

    private String text;

    private Boolean isCorrect = false;

    private String feedback;

    private Integer position;

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
    public NotebookQuizz getQuiz() {
        return quiz;
    }

    @NotNull
    @Column(name = COLUMN_TEXT_NAME, nullable = false, length = Integer.MAX_VALUE)
    public String getText() {
        return text;
    }

    @NotNull
    @ColumnDefault("false")
    @Column(name = COLUMN_ISCORRECT_NAME, nullable = false)
    public Boolean getIsCorrect() {
        return isCorrect;
    }

    @Column(name = COLUMN_FEEDBACK_NAME, length = Integer.MAX_VALUE)
    public String getFeedback() {
        return feedback;
    }

    @Column(name = COLUMN_POSITION_NAME)
    public Integer getPosition() {
        return position;
    }

    @NotNull
    @ColumnDefault("now()")
    @Column(name = COLUMN_CREATEDAT_NAME, nullable = false)
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

}
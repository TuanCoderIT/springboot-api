package com.example.springboot_api.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.io.Serializable;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity(name = QuizOption.ENTITY_NAME)
@Table(name = QuizOption.TABLE_NAME)
public class QuizOption implements Serializable {
    public static final String ENTITY_NAME = "Quiz_Option";
    public static final String TABLE_NAME = "quiz_options";
    public static final String COLUMN_ID_NAME = "id";
    public static final String COLUMN_OPTIONTEXT_NAME = "option_text";
    public static final String COLUMN_ISCORRECT_NAME = "is_correct";
    private static final long serialVersionUID = 2994751429448678340L;


    private UUID id;

    private QuizQuestion question;

    private String optionText;

    private Boolean isCorrect = false;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = COLUMN_ID_NAME, nullable = false)
    public UUID getId() {
        return id;
    }

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "question_id", nullable = false)
    public QuizQuestion getQuestion() {
        return question;
    }

    @NotNull
    @Column(name = COLUMN_OPTIONTEXT_NAME, nullable = false, length = Integer.MAX_VALUE)
    public String getOptionText() {
        return optionText;
    }

    @NotNull
    @ColumnDefault("false")
    @Column(name = COLUMN_ISCORRECT_NAME, nullable = false)
    public Boolean getIsCorrect() {
        return isCorrect;
    }

}
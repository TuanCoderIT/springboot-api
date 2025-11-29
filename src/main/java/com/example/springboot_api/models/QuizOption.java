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
@Entity(name = "Quiz_Option")
@Table(name = "quiz_options", schema = "public", indexes = {
        @Index(name = "idx_quiz_options_question", columnList = "question_id")
})
public class QuizOption implements Serializable {
    private static final long serialVersionUID = -4392414011039934806L;
    private UUID id;

    private QuizQuestion question;

    private String optionText;

    private Boolean isCorrect = false;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
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
    @Column(name = "option_text", nullable = false, length = Integer.MAX_VALUE)
    public String getOptionText() {
        return optionText;
    }

    @NotNull
    @ColumnDefault("false")
    @Column(name = "is_correct", nullable = false)
    public Boolean getIsCorrect() {
        return isCorrect;
    }

}
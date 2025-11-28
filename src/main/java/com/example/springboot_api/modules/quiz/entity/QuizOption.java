package com.example.springboot_api.modules.quiz.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "quiz_options")
public class QuizOption {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "question_id", nullable = false)
    private QuizQuestion question;

    @NotNull
    @Column(name = "option_text", nullable = false, length = Integer.MAX_VALUE)
    private String optionText;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect = false;

}
package com.example.springboot_api.dto.user.quiz;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO response cho QuizOption (câu trả lời của quiz).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizOptionResponse {

    private UUID id;
    private String text;
    private Boolean isCorrect;
    private String feedback;
    private Integer position;
}

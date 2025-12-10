package com.example.springboot_api.dto.user.quiz;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO response cho một quiz (câu hỏi) kèm danh sách options (câu trả lời).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizResponse {

    private UUID id;
    private String question;
    private String explanation;
    private Short difficultyLevel;
    private OffsetDateTime createdAt;
    private List<QuizOptionResponse> options;
}

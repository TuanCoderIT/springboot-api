package com.example.springboot_api.dto.user.quiz.attempt;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO cho quiz attempt.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttemptResponse {

    private UUID id;
    private UUID aiSetId;

    private Integer score;
    private Integer totalQuestions;
    private Integer correctCount;
    private Integer timeSpentSeconds;

    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;
    private OffsetDateTime createdAt;

    /**
     * Có analysis hay chưa.
     */
    private boolean hasAnalysis;

    /**
     * Chi tiết từng câu trả lời (optional, khi lấy detail).
     */
    private List<AttemptAnswerDetail> answers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttemptAnswerDetail {
        private UUID quizId;
        private String question;
        private UUID selectedOptionId;
        private String selectedOptionText;
        private UUID correctOptionId;
        private String correctOptionText;
        private boolean isCorrect;
    }
}

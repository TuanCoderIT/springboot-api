package com.example.springboot_api.dto.user.quiz.attempt;

import java.util.List;
import java.util.UUID;

import lombok.Data;

/**
 * Request DTO để submit quiz attempt.
 */
@Data
public class SubmitAttemptRequest {

    /**
     * Thời gian bắt đầu làm quiz (ISO 8601).
     */
    private String startedAt;

    /**
     * Thời gian kết thúc (ISO 8601).
     */
    private String finishedAt;

    /**
     * Thời gian làm bài (giây).
     */
    private Integer timeSpentSeconds;

    /**
     * Danh sách câu trả lời.
     */
    private List<AnswerItem> answers;

    @Data
    public static class AnswerItem {
        /**
         * Quiz ID (câu hỏi).
         */
        private UUID quizId;

        /**
         * Option ID được chọn.
         */
        private UUID selectedOptionId;
    }
}

package com.example.springboot_api.dto.user.quiz.attempt;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO cho AI analysis kết quả quiz.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizAnalysisResponse {

    /**
     * Điểm số dạng text: "7/10 (70%)"
     */
    private String scoreText;

    /**
     * Tóm tắt ngắn gọn.
     */
    private String summary;

    /**
     * Danh sách điểm mạnh.
     */
    private List<TopicAnalysis> strengths;

    /**
     * Danh sách điểm yếu.
     */
    private List<TopicAnalysis> weaknesses;

    /**
     * Danh sách chủ đề đã cải thiện so với lần trước.
     */
    private List<TopicAnalysis> improvements;

    /**
     * Gợi ý cải thiện.
     */
    private List<String> recommendations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopicAnalysis {
        private String topic;
        private String analysis;
        private List<String> suggestions;
    }
}

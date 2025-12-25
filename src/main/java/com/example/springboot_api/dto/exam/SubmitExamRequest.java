package com.example.springboot_api.dto.exam;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class SubmitExamRequest {
    
    @NotNull(message = "Attempt ID is required")
    private UUID attemptId;
    
    @NotEmpty(message = "Answers are required")
    @Valid
    private List<SubmitAnswerRequest> answers;
    
    // Thông tin nộp bài
    private Boolean isAutoSubmit = false;
    private Integer timeSpentSeconds;
    
    // Anti-cheat data
    private String finalBrowserInfo;
    private Integer tabSwitchCount = 0;
    private Integer copyPasteCount = 0;
    private Integer rightClickCount = 0;
    private List<String> suspiciousActivities;
    
    @Data
    public static class SubmitAnswerRequest {
        
        @NotNull(message = "Question ID is required")
        private UUID questionId;
        
        @NotNull(message = "Answer data is required")
        private Object answerData; // JSON object chứa câu trả lời
        
        private Integer timeSpentSeconds = 0;
        
        // Metadata cho câu trả lời
        private Integer revisionCount = 0; // Số lần sửa đổi
        private Boolean wasSkipped = false; // Có bỏ qua không
        private String confidence; // HIGH, MEDIUM, LOW
    }
}
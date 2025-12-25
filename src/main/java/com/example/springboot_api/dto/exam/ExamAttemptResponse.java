package com.example.springboot_api.dto.exam;

import com.example.springboot_api.models.exam.AttemptStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class ExamAttemptResponse {
    
    private UUID attemptId;
    private UUID examId;
    private String examTitle;
    private Integer attemptNumber;
    private AttemptStatus status;
    
    // Thời gian
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private Integer timeSpentSeconds;
    private Integer remainingTimeSeconds;
    
    // Cấu hình kỳ thi (snapshot)
    private Integer durationMinutes;
    private Boolean shuffleQuestions;
    private Boolean shuffleOptions;
    private Boolean allowReview;
    
    // Câu hỏi
    private List<ExamQuestionResponse> questions;
    
    // Thông tin sinh viên
    private String studentCode;
    private String studentName;
    
    // Trạng thái
    private Boolean isTimeUp;
    private Boolean canSubmit;
    private Boolean autoSubmitEnabled;
    
    @Data
    public static class ExamQuestionResponse {
        private UUID questionId;
        private String questionType;
        private String questionText;
        private String questionImageUrl;
        private String questionAudioUrl;
        private Integer orderIndex;
        private Double points;
        private Integer timeLimitSeconds;
        private String difficultyLevel;
        
        // Options cho MCQ
        private List<QuestionOptionResponse> options;
        
        // Câu trả lời hiện tại (nếu có)
        private Object currentAnswer;
        private Boolean isAnswered;
        private Integer timeSpentSeconds;
        
        @Data
        public static class QuestionOptionResponse {
            private UUID optionId;
            private String optionText;
            private String optionImageUrl;
            private String optionAudioUrl;
            private Integer orderIndex;
            // Không trả về isCorrect cho sinh viên
        }
    }
}
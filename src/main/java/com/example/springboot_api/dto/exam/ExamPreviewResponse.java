package com.example.springboot_api.dto.exam;

import com.example.springboot_api.models.exam.DifficultyLevel;
import com.example.springboot_api.models.exam.ExamStatus;
import com.example.springboot_api.models.exam.QuestionType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class ExamPreviewResponse {
    private UUID id;
    private UUID classId;
    private String className;
    private String subjectCode;
    private String subjectName;
    private String title;
    private String description;
    
    // Thời gian
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer durationMinutes;
    
    // Cấu hình
    private Integer totalQuestions;
    private BigDecimal totalPoints;
    private BigDecimal passingScore;
    private Integer maxAttempts;
    
    // Cài đặt
    private Boolean shuffleQuestions;
    private Boolean shuffleOptions;
    private Boolean showResultsImmediately;
    private Boolean allowReview;
    
    // Anti-cheat
    private Boolean enableProctoring;
    private Boolean enableLockdown;
    private Boolean enablePlagiarismCheck;
    
    // Trạng thái
    private ExamStatus status;
    
    // Thông tin người tạo
    private UUID createdById;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Questions with answers and scoring (preview-specific)
    private List<QuestionPreview> questions;
    
    @Data
    public static class QuestionPreview {
        private UUID id;
        private QuestionType questionType;
        private String questionText;
        private String questionImageUrl;
        private String questionAudioUrl;
        private BigDecimal points;
        private Integer orderIndex;
        private Integer timeLimitSeconds;
        private DifficultyLevel difficultyLevel;
        private String explanation;
        private String correctAnswer;
        private List<OptionPreview> options;
    }
    
    @Data
    public static class OptionPreview {
        private UUID id;
        private String optionText;
        private String optionImageUrl;
        private String optionAudioUrl;
        private Integer orderIndex;
        private Boolean isCorrect;
    }
}
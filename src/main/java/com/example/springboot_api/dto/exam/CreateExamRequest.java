package com.example.springboot_api.dto.exam;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class CreateExamRequest {
    
    @NotNull(message = "Class ID is required")
    private UUID classId;
    
    @NotBlank(message = "Title is required")
    @Size(max = 500, message = "Title must not exceed 500 characters")
    private String title;
    
    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;
    
    @NotNull(message = "Start time is required")
    @Future(message = "Start time must be in the future")
    private LocalDateTime startTime;
    
    @NotNull(message = "End time is required")
    @Future(message = "End time must be in the future")
    private LocalDateTime endTime;
    
    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 minute")
    @Max(value = 480, message = "Duration must not exceed 480 minutes (8 hours)")
    private Integer durationMinutes;
    
    @DecimalMin(value = "0.0", message = "Passing score must be non-negative")
    private BigDecimal passingScore = BigDecimal.ZERO;
    
    // Cài đặt thi
    private Boolean shuffleQuestions = true;
    private Boolean shuffleOptions = true;
    private Boolean showResultsImmediately = false;
    private Boolean allowReview = true;
    
    @Min(value = 1, message = "Max attempts must be at least 1")
    @Max(value = 10, message = "Max attempts must not exceed 10")
    private Integer maxAttempts = 1;
    
    // Anti-cheat settings
    private Boolean enableProctoring = false;
    private Boolean enableLockdown = false;
    private Boolean enablePlagiarismCheck = false;
    
    // Files từ notebook để sinh câu hỏi
    private List<UUID> notebookFileIds;
    
    // Cấu hình sinh câu hỏi
    private Integer numberOfQuestions = 10;
    private String questionTypes = "MCQ"; // MCQ, TRUE_FALSE, ESSAY
    private String difficultyLevel = "MEDIUM"; // EASY, MEDIUM, HARD
    
    // Validation
    @AssertTrue(message = "End time must be after start time")
    public boolean isEndTimeAfterStartTime() {
        if (startTime == null || endTime == null) return true;
        return endTime.isAfter(startTime);
    }
    
    @AssertTrue(message = "Duration must not exceed the time between start and end time")
    public boolean isDurationValid() {
        if (startTime == null || endTime == null || durationMinutes == null) return true;
        long minutesBetween = java.time.Duration.between(startTime, endTime).toMinutes();
        return durationMinutes <= minutesBetween;
    }
}
package com.example.springboot_api.dto.exam;

import com.example.springboot_api.models.exam.ExamStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ExamResponse {
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
    
    // Thông tin trạng thái cho sinh viên
    private Boolean canTakeExam;
    private Boolean isActive;
    private Boolean isTimeUp;
    private Integer remainingAttempts;
    
    // Thống kê (cho giảng viên)
    private Long totalStudents;
    private Long completedAttempts;
    private Double averageScore;
    private Double passRate;
}
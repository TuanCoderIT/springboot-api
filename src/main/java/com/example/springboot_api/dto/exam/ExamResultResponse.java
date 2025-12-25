package com.example.springboot_api.dto.exam;

import com.example.springboot_api.models.exam.AttemptStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class ExamResultResponse {
    
    // Thông tin lượt thi
    private UUID attemptId;
    private UUID examId;
    private String examTitle;
    private Integer attemptNumber;
    private AttemptStatus status;
    
    // Thời gian
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private Integer timeSpentSeconds;
    private String timeSpentFormatted; // "1h 30m 45s"
    
    // Điểm số
    private BigDecimal totalScore;
    private BigDecimal totalPossibleScore;
    private BigDecimal percentageScore;
    private Boolean isPassed;
    private String grade; // A, B, C, D, F
    
    // Thống kê câu hỏi
    private Integer totalQuestions;
    private Integer answeredQuestions;
    private Integer correctAnswers;
    private Integer incorrectAnswers;
    private Integer skippedQuestions;
    
    // Chi tiết theo loại câu hỏi
    private List<QuestionTypeResult> questionTypeResults;
    
    // Thông tin sinh viên
    private UUID studentId;
    private String studentCode;
    private String studentName;
    
    // Cài đặt hiển thị
    private Boolean showDetailedResults;
    private Boolean allowReview;
    private Boolean showCorrectAnswers;
    
    // Thống kê so sánh (nếu được phép)
    private ClassStatistics classStatistics;
    
    @Data
    public static class QuestionTypeResult {
        private String questionType;
        private Integer totalQuestions;
        private Integer correctAnswers;
        private BigDecimal totalPoints;
        private BigDecimal earnedPoints;
        private Double accuracy; // Percentage
    }
    
    @Data
    public static class ClassStatistics {
        private Double classAverage;
        private Double highestScore;
        private Double lowestScore;
        private Integer totalStudents;
        private Integer completedStudents;
        private Double passRate;
        private Integer yourRank; // Thứ hạng trong lớp
    }
}
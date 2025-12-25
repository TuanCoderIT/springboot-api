package com.example.springboot_api.models.exam;

import com.example.springboot_api.models.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "exam_answers")
@Data
@EqualsAndHashCode(exclude = {"attempt", "question", "gradedBy"})
@ToString(exclude = {"attempt", "question", "gradedBy"})
public class ExamAnswer {
    
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    @JsonIgnore
    private ExamAttempt attempt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private ExamQuestion question;
    
    // Câu trả lời (đa hình theo loại câu hỏi)
    @Enumerated(EnumType.STRING)
    @Column(name = "answer_type", nullable = false, length = 20)
    private AnswerType answerType;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "answer_data", nullable = false)
    private String answerData;
    
    // Chấm điểm
    @Column(name = "is_correct")
    private Boolean isCorrect;
    
    @Column(name = "points_earned", precision = 10, scale = 2)
    private BigDecimal pointsEarned = BigDecimal.ZERO;
    
    @Column(name = "auto_graded")
    private Boolean autoGraded = false;
    
    // Thời gian
    @Column(name = "answered_at", nullable = false)
    private LocalDateTime answeredAt = LocalDateTime.now();
    
    @Column(name = "time_spent_seconds")
    private Integer timeSpentSeconds = 0;
    
    // AI grading (mở rộng tương lai)
    @Column(name = "ai_feedback", columnDefinition = "TEXT")
    private String aiFeedback;
    
    @Column(name = "ai_confidence_score", precision = 3, scale = 2)
    private BigDecimal aiConfidenceScore;
    
    // Manual grading
    @Column(name = "manual_feedback", columnDefinition = "TEXT")
    private String manualFeedback;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "graded_by")
    private User gradedBy;
    
    @Column(name = "graded_at")
    private LocalDateTime gradedAt;
    
    // Metadata
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    private String metadata = "{}";
    
    // Audit fields
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    // Helper methods
    public boolean isMCQ() {
        return answerType == AnswerType.MCQ;
    }
    
    public boolean isTrueFalse() {
        return answerType == AnswerType.TRUE_FALSE;
    }
    
    public boolean isEssay() {
        return answerType == AnswerType.ESSAY;
    }
    
    public boolean isAutoGradable() {
        return answerType == AnswerType.MCQ || answerType == AnswerType.TRUE_FALSE;
    }
    
    public void autoGrade(Boolean correct, BigDecimal points) {
        this.isCorrect = correct;
        this.pointsEarned = correct ? points : BigDecimal.ZERO;
        this.autoGraded = true;
        this.gradedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
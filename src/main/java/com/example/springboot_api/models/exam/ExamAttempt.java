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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "exam_attempts")
@Data
@EqualsAndHashCode(exclude = {"exam", "student", "answers"})
@ToString(exclude = {"exam", "student", "answers"})
public class ExamAttempt {
    
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    @JsonIgnore
    private Exam exam;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;
    
    // Thông tin lượt thi
    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber = 1;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private AttemptStatus status = AttemptStatus.IN_PROGRESS;
    
    // Thời gian
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt = LocalDateTime.now();
    
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;
    
    @Column(name = "time_spent_seconds")
    private Integer timeSpentSeconds = 0;
    
    // Điểm số
    @Column(name = "total_score", precision = 10, scale = 2)
    private BigDecimal totalScore = BigDecimal.ZERO;
    
    @Column(name = "percentage_score", precision = 5, scale = 2)
    private BigDecimal percentageScore = BigDecimal.ZERO;
    
    @Column(name = "is_passed")
    private Boolean isPassed;
    
    // Cấu hình lượt thi (snapshot tại thời điểm thi)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "exam_snapshot", nullable = false)
    private String examSnapshot;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "questions_snapshot", nullable = false)
    private String questionsSnapshot;
    
    // Anti-cheat data
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "browser_info")
    private String browserInfo = "{}";
    
    @Column(name = "ip_address")
    private String ipAddress;
    
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "proctoring_data")
    private String proctoringData = "{}";
    
    // Metadata
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    private String metadata = "{}";
    
    // Audit fields
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    // Relationships
    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ExamAnswer> answers = new ArrayList<>();
    
    // Helper methods
    public boolean isInProgress() {
        return status == AttemptStatus.IN_PROGRESS;
    }
    
    public boolean isSubmitted() {
        return status == AttemptStatus.SUBMITTED || status == AttemptStatus.AUTO_SUBMITTED;
    }
    
    public boolean isGraded() {
        return status == AttemptStatus.GRADED;
    }
    
    public boolean isTimeUp(Integer durationMinutes) {
        if (startedAt == null || durationMinutes == null) return false;
        return LocalDateTime.now().isAfter(startedAt.plusMinutes(durationMinutes));
    }
    
    public void submit(boolean autoSubmit) {
        this.submittedAt = LocalDateTime.now();
        this.status = autoSubmit ? AttemptStatus.AUTO_SUBMITTED : AttemptStatus.SUBMITTED;
        this.timeSpentSeconds = calculateTimeSpent();
        this.updatedAt = LocalDateTime.now();
    }
    
    public void grade(BigDecimal score, BigDecimal totalPossible) {
        this.totalScore = score;
        this.percentageScore = totalPossible.compareTo(BigDecimal.ZERO) > 0 
            ? score.divide(totalPossible, 2, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal("100"))
            : BigDecimal.ZERO;
        this.status = AttemptStatus.GRADED;
        this.updatedAt = LocalDateTime.now();
    }
    
    private Integer calculateTimeSpent() {
        if (startedAt == null) return 0;
        LocalDateTime endTime = submittedAt != null ? submittedAt : LocalDateTime.now();
        return (int) java.time.Duration.between(startedAt, endTime).getSeconds();
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
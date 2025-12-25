package com.example.springboot_api.models.exam;

import com.example.springboot_api.models.Class;
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
@Table(name = "exams")
@Data
@EqualsAndHashCode(exclude = {"questions", "attempts"})
@ToString(exclude = {"questions", "attempts"})
public class Exam {
    
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private Class classEntity;
    
    @Column(name = "title", nullable = false, length = 500)
    private String title;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    // Cấu hình thời gian
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;
    
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;
    
    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;
    
    // Cấu hình thi
    @Column(name = "total_questions", nullable = false)
    private Integer totalQuestions = 0;
    
    @Column(name = "total_points", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPoints = BigDecimal.ZERO;
    
    @Column(name = "passing_score", nullable = false, precision = 10, scale = 2)
    private BigDecimal passingScore = BigDecimal.ZERO;
    
    // Cài đặt thi
    @Column(name = "shuffle_questions")
    private Boolean shuffleQuestions = true;
    
    @Column(name = "shuffle_options")
    private Boolean shuffleOptions = true;
    
    @Column(name = "show_results_immediately")
    private Boolean showResultsImmediately = false;
    
    @Column(name = "allow_review")
    private Boolean allowReview = true;
    
    @Column(name = "max_attempts")
    private Integer maxAttempts = 1;
    
    // Anti-cheat settings
    @Column(name = "enable_proctoring")
    private Boolean enableProctoring = false;
    
    @Column(name = "enable_lockdown")
    private Boolean enableLockdown = false;
    
    @Column(name = "enable_plagiarism_check")
    private Boolean enablePlagiarismCheck = false;
    
    // Trạng thái
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private ExamStatus status = ExamStatus.DRAFT;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    private String metadata = "{}";
    
    // Audit fields
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    // Relationships
    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ExamQuestion> questions = new ArrayList<>();
    
    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ExamAttempt> attempts = new ArrayList<>();
    
    // Helper methods
    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return status == ExamStatus.ACTIVE && 
               now.isAfter(startTime) && 
               now.isBefore(endTime);
    }
    
    public boolean canStudentTakeExam() {
        return status == ExamStatus.ACTIVE && isActive();
    }
    
    public boolean isTimeUp() {
        return LocalDateTime.now().isAfter(endTime);
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
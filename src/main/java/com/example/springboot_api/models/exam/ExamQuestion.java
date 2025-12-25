package com.example.springboot_api.models.exam;

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
@Table(name = "exam_questions")
@Data
@EqualsAndHashCode(exclude = {"exam", "options"})
@ToString(exclude = {"exam", "options"})
public class ExamQuestion {
    
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    @JsonIgnore
    private Exam exam;
    
    // Thông tin câu hỏi
    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 20)
    private QuestionType questionType;
    
    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;
    
    @Column(name = "question_image_url")
    private String questionImageUrl;
    
    @Column(name = "question_audio_url")
    private String questionAudioUrl;
    
    // Điểm số và thứ tự
    @Column(name = "points", nullable = false, precision = 10, scale = 2)
    private BigDecimal points = BigDecimal.ONE;
    
    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;
    
    // Cấu hình câu hỏi
    @Column(name = "time_limit_seconds")
    private Integer timeLimitSeconds;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty_level", length = 10)
    private DifficultyLevel difficultyLevel = DifficultyLevel.MEDIUM;
    
    // Metadata cho các loại câu hỏi khác nhau
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "question_config")
    private String questionConfig = "{}";
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "correct_answer")
    private String correctAnswer;
    
    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;
    
    // Audit fields
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    // Relationships
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("orderIndex ASC")
    private List<ExamQuestionOption> options = new ArrayList<>();
    
    // Helper methods
    public boolean isMCQ() {
        return questionType == QuestionType.MCQ;
    }
    
    public boolean isTrueFalse() {
        return questionType == QuestionType.TRUE_FALSE;
    }
    
    public boolean isEssay() {
        return questionType == QuestionType.ESSAY;
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
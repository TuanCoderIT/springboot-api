package com.example.springboot_api.models.exam;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "exam_question_options")
@Data
@EqualsAndHashCode(exclude = {"question"})
@ToString(exclude = {"question"})
public class ExamQuestionOption {
    
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    @JsonIgnore
    private ExamQuestion question;
    
    // Nội dung lựa chọn
    @Column(name = "option_text", nullable = false, columnDefinition = "TEXT")
    private String optionText;
    
    @Column(name = "option_image_url")
    private String optionImageUrl;
    
    @Column(name = "option_audio_url")
    private String optionAudioUrl;
    
    // Thứ tự và trạng thái
    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;
    
    @Column(name = "is_correct")
    private Boolean isCorrect = false;
    
    // Metadata
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    private String metadata = "{}";
    
    // Audit fields
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
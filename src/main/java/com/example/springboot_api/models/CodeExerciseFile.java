package com.example.springboot_api.models;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * File code: starter (khởi tạo), solution (đáp án), user (code của user).
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@Accessors(chain = true)
@Entity(name = "CodeExerciseFile")
@Table(name = "code_exercise_files", schema = "public")
public class CodeExerciseFile {

    public static final String ENTITY_NAME = "CodeExerciseFile";
    public static final String TABLE_NAME = "code_exercise_files";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @ToString.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id", nullable = false)
    private CodeExercise exercise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "filename", nullable = false)
    @ToString.Include
    private String filename;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private String role = "starter"; // starter, solution, user

    @Column(name = "is_main")
    @Builder.Default
    private Boolean isMain = false;

    @Column(name = "is_pass")
    @Builder.Default
    private Boolean isPass = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

package com.example.springboot_api.file.entity;

import com.example.springboot_api.notebook.entity.Notebook;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stored_files", indexes = {
        @Index(name = "idx_files_notebook_id", columnList = "notebook_id"),
        @Index(name = "idx_files_file_type", columnList = "file_type"),
        @Index(name = "idx_files_created_at", columnList = "created_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoredFile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notebook_id", nullable = false, foreignKey = @ForeignKey(name = "fk_files_notebook"))
    private Notebook notebook;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", length = 50)
    private FileType fileType;

    @Column(name = "original_name", length = 255)
    private String originalName;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "ocr_text", columnDefinition = "text")
    private String ocrText;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}

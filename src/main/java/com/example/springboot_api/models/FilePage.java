package com.example.springboot_api.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity(name = "File_Page")
@Table(name = "file_pages", schema = "public", indexes = {
        @Index(name = "uq_file_pages_file_page", columnList = "file_id, page_number", unique = true),
        @Index(name = "idx_file_pages_file_id", columnList = "file_id")
})
public class FilePage implements Serializable {
    private static final long serialVersionUID = -6263618522524636L;
    private UUID id;

    private NotebookFile file;

    private Integer pageNumber;

    private String textContent;

    private Integer tokenCount;

    private OffsetDateTime createdAt;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    public UUID getId() {
        return id;
    }

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "file_id", nullable = false)
    public NotebookFile getFile() {
        return file;
    }

    @NotNull
    @Column(name = "page_number", nullable = false)
    public Integer getPageNumber() {
        return pageNumber;
    }

    @Column(name = "text_content", length = Integer.MAX_VALUE)
    public String getTextContent() {
        return textContent;
    }

    @Column(name = "token_count")
    public Integer getTokenCount() {
        return tokenCount;
    }

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

}
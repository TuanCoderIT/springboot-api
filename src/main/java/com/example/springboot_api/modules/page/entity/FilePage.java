package com.example.springboot_api.modules.page.entity;

import com.example.springboot_api.modules.file.entity.NotebookFile;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "file_pages")
public class FilePage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "file_id", nullable = false)
    private NotebookFile file;

    @NotNull
    @Column(name = "page_number", nullable = false)
    private Integer pageNumber;

    @Column(name = "text_content", length = Integer.MAX_VALUE)
    private String textContent;

    @Column(name = "token_count")
    private Integer tokenCount;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

}
package com.example.springboot_api.modules.asset.entity;

import com.example.springboot_api.modules.auth.entity.User;
import com.example.springboot_api.modules.notebook.entity.Notebook;
import com.example.springboot_api.modules.file.entity.NotebookFile;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
@Table(name = "video_assets")
public class VideoAsset {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "notebook_id", nullable = false)
    private Notebook notebook;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "file_id")
    private NotebookFile file;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Size(max = 16)
    @Column(name = "language", length = 16)
    private String language;

    @Size(max = 64)
    @Column(name = "style", length = 64)
    private String style;

    @Column(name = "text_source", length = Integer.MAX_VALUE)
    private String textSource;

    @NotNull
    @Column(name = "video_url", nullable = false, length = Integer.MAX_VALUE)
    private String videoUrl;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

}
package com.example.springboot_api.models;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity(name = VideoAsset.ENTITY_NAME)
@Table(name = VideoAsset.TABLE_NAME)
public class VideoAsset implements Serializable {
    public static final String ENTITY_NAME = "Video_Asset";
    public static final String TABLE_NAME = "video_assets";
    public static final String COLUMN_ID_NAME = "id";
    public static final String COLUMN_LANGUAGE_NAME = "language";
    public static final String COLUMN_STYLE_NAME = "style";
    public static final String COLUMN_TEXTSOURCE_NAME = "text_source";
    public static final String COLUMN_VIDEOURL_NAME = "video_url";
    public static final String COLUMN_DURATIONSECONDS_NAME = "duration_seconds";
    public static final String COLUMN_CREATEDAT_NAME = "created_at";
    private static final long serialVersionUID = 1358579455246993418L;

    private UUID id;

    private Notebook notebook;

    private User createdBy;

    private String language;

    private String style;

    private String textSource;

    private String videoUrl;

    private Integer durationSeconds;

    private OffsetDateTime createdAt;

    private Set<VideoAssetFile> videoAssetFiles = new LinkedHashSet<>();

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = COLUMN_ID_NAME, nullable = false)
    public UUID getId() {
        return id;
    }

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "notebook_id", nullable = false)
    public Notebook getNotebook() {
        return notebook;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "created_by")
    public User getCreatedBy() {
        return createdBy;
    }

    @Size(max = 16)
    @Column(name = COLUMN_LANGUAGE_NAME, length = 16)
    public String getLanguage() {
        return language;
    }

    @Size(max = 64)
    @Column(name = COLUMN_STYLE_NAME, length = 64)
    public String getStyle() {
        return style;
    }

    @Column(name = COLUMN_TEXTSOURCE_NAME, length = Integer.MAX_VALUE)
    public String getTextSource() {
        return textSource;
    }

    @NotNull
    @Column(name = COLUMN_VIDEOURL_NAME, nullable = false, length = Integer.MAX_VALUE)
    public String getVideoUrl() {
        return videoUrl;
    }

    @Column(name = COLUMN_DURATIONSECONDS_NAME)
    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    @NotNull
    @ColumnDefault("now()")
    @Column(name = COLUMN_CREATEDAT_NAME, nullable = false)
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    @OneToMany(mappedBy = "videoAsset")
    public Set<VideoAssetFile> getVideoAssetFiles() {
        return videoAssetFiles;
    }

}
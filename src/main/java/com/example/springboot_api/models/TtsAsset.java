package com.example.springboot_api.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
@ToString
@Entity(name = TtsAsset.ENTITY_NAME)
@Table(name = TtsAsset.TABLE_NAME, schema = "public", indexes = {
        @Index(name = "idx_tts_assets_notebook", columnList = "notebook_id, created_at")
})
public class TtsAsset implements Serializable {
    public static final String ENTITY_NAME = "Tts_Asset";
    public static final String TABLE_NAME = "tts_assets";
    public static final String COLUMN_ID_NAME = "id";
    public static final String COLUMN_LANGUAGE_NAME = "language";
    public static final String COLUMN_VOICENAME_NAME = "voice_name";
    public static final String COLUMN_TEXTSOURCE_NAME = "text_source";
    public static final String COLUMN_AUDIOURL_NAME = "audio_url";
    public static final String COLUMN_DURATIONSECONDS_NAME = "duration_seconds";
    public static final String COLUMN_CREATEDAT_NAME = "created_at";
    private static final long serialVersionUID = 5553417680604496525L;


    private UUID id;

    private Notebook notebook;

    private User createdBy;

    private String language;

    private String voiceName;

    private String textSource;

    private String audioUrl;

    private Integer durationSeconds;

    private OffsetDateTime createdAt;

    private NotebookAiSet notebookAiSets;

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
    @Column(name = COLUMN_VOICENAME_NAME, length = 64)
    public String getVoiceName() {
        return voiceName;
    }

    @Column(name = COLUMN_TEXTSOURCE_NAME, length = Integer.MAX_VALUE)
    public String getTextSource() {
        return textSource;
    }

    @NotNull
    @Column(name = COLUMN_AUDIOURL_NAME, nullable = false, length = Integer.MAX_VALUE)
    public String getAudioUrl() {
        return audioUrl;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notebook_ai_sets")
    public NotebookAiSet getNotebookAiSets() {
        return notebookAiSets;
    }

}
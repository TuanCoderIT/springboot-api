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
@Entity(name = "Tts_Asset")
@Table(name = "tts_assets", schema = "public", indexes = {
        @Index(name = "idx_tts_assets_notebook", columnList = "notebook_id, created_at")
})
public class TtsAsset implements Serializable {
    private static final long serialVersionUID = -6280337968131558127L;
    private UUID id;

    private Notebook notebook;

    private NotebookFile file;

    private User createdBy;

    private String language;

    private String voiceName;

    private String textSource;

    private String audioUrl;

    private Integer durationSeconds;

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
    @JoinColumn(name = "notebook_id", nullable = false)
    public Notebook getNotebook() {
        return notebook;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "file_id")
    public NotebookFile getFile() {
        return file;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "created_by")
    public User getCreatedBy() {
        return createdBy;
    }

    @Size(max = 16)
    @Column(name = "language", length = 16)
    public String getLanguage() {
        return language;
    }

    @Size(max = 64)
    @Column(name = "voice_name", length = 64)
    public String getVoiceName() {
        return voiceName;
    }

    @Column(name = "text_source", length = Integer.MAX_VALUE)
    public String getTextSource() {
        return textSource;
    }

    @NotNull
    @Column(name = "audio_url", nullable = false, length = Integer.MAX_VALUE)
    public String getAudioUrl() {
        return audioUrl;
    }

    @Column(name = "duration_seconds")
    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

}
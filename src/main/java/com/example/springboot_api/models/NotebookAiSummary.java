package com.example.springboot_api.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.Accessors;
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
@ToString(onlyExplicitlyIncluded = true)
@Accessors(chain = true)
@Entity(name = NotebookAiSummary.ENTITY_NAME)
@Table(name = NotebookAiSummary.TABLE_NAME, schema = "public", indexes = {
        @Index(name = "idx_notebook_ai_summaries_tts", columnList = "tts_provider, tts_model"),
        @Index(name = "idx_notebook_ai_summaries_created_at", columnList = "created_at")
})
public class NotebookAiSummary implements Serializable {
    public static final String ENTITY_NAME = "Notebook_Ai_Summary";
    public static final String TABLE_NAME = "notebook_ai_summaries";
    public static final String COLUMN_ID_NAME = "id";
    public static final String COLUMN_CONTENTMD_NAME = "content_md";
    public static final String COLUMN_SCRIPTTTS_NAME = "script_tts";
    public static final String COLUMN_LANGUAGE_NAME = "language";
    public static final String COLUMN_AUDIOURL_NAME = "audio_url";
    public static final String COLUMN_AUDIOFORMAT_NAME = "audio_format";
    public static final String COLUMN_AUDIODURATIONMS_NAME = "audio_duration_ms";
    public static final String COLUMN_TTSPROVIDER_NAME = "tts_provider";
    public static final String COLUMN_TTSMODEL_NAME = "tts_model";
    public static final String COLUMN_VOICEID_NAME = "voice_id";
    public static final String COLUMN_VOICELABEL_NAME = "voice_label";
    public static final String COLUMN_VOICESPEED_NAME = "voice_speed";
    public static final String COLUMN_VOICEPITCH_NAME = "voice_pitch";
    public static final String COLUMN_CREATEDAT_NAME = "created_at";
    public static final String COLUMN_UPDATEDAT_NAME = "updated_at";
    private static final long serialVersionUID = 2756161831042188383L;


    private UUID id;

    private NotebookAiSet notebookAiSets;

    private String contentMd;

    private String scriptTts;

    private String language;

    private String audioUrl;

    private String audioFormat;

    private Integer audioDurationMs;

    private String ttsProvider;

    private String ttsModel;

    private String voiceId;

    private String voiceLabel;

    private Float voiceSpeed;

    private Float voicePitch;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    private User createBy;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = COLUMN_ID_NAME, nullable = false)
    public UUID getId() {
        return id;
    }

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "id", nullable = false)
    public NotebookAiSet getNotebookAiSets() {
        return notebookAiSets;
    }

    @NotNull
    @Column(name = COLUMN_CONTENTMD_NAME, nullable = false, length = Integer.MAX_VALUE)
    public String getContentMd() {
        return contentMd;
    }

    @Column(name = COLUMN_SCRIPTTTS_NAME, length = Integer.MAX_VALUE)
    public String getScriptTts() {
        return scriptTts;
    }

    @NotNull
    @ColumnDefault("'vi'")
    @Column(name = COLUMN_LANGUAGE_NAME, nullable = false, length = Integer.MAX_VALUE)
    public String getLanguage() {
        return language;
    }

    @Column(name = COLUMN_AUDIOURL_NAME, length = Integer.MAX_VALUE)
    public String getAudioUrl() {
        return audioUrl;
    }

    @Column(name = COLUMN_AUDIOFORMAT_NAME, length = Integer.MAX_VALUE)
    public String getAudioFormat() {
        return audioFormat;
    }

    @Column(name = COLUMN_AUDIODURATIONMS_NAME)
    public Integer getAudioDurationMs() {
        return audioDurationMs;
    }

    @Column(name = COLUMN_TTSPROVIDER_NAME, length = Integer.MAX_VALUE)
    public String getTtsProvider() {
        return ttsProvider;
    }

    @Column(name = COLUMN_TTSMODEL_NAME, length = Integer.MAX_VALUE)
    public String getTtsModel() {
        return ttsModel;
    }

    @Column(name = COLUMN_VOICEID_NAME, length = Integer.MAX_VALUE)
    public String getVoiceId() {
        return voiceId;
    }

    @Column(name = COLUMN_VOICELABEL_NAME, length = Integer.MAX_VALUE)
    public String getVoiceLabel() {
        return voiceLabel;
    }

    @Column(name = COLUMN_VOICESPEED_NAME)
    public Float getVoiceSpeed() {
        return voiceSpeed;
    }

    @Column(name = COLUMN_VOICEPITCH_NAME)
    public Float getVoicePitch() {
        return voicePitch;
    }

    @NotNull
    @ColumnDefault("now()")
    @Column(name = COLUMN_CREATEDAT_NAME, nullable = false)
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    @Column(name = COLUMN_UPDATEDAT_NAME)
    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "create_by")
    public User getCreateBy() {
        return createBy;
    }

}
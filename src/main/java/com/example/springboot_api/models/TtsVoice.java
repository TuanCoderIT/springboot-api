package com.example.springboot_api.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity(name = TtsVoice.ENTITY_NAME)
@Table(name = TtsVoice.TABLE_NAME, schema = "public", indexes = {
        @Index(name = "idx_tts_voices_provider", columnList = "provider"),
        @Index(name = "idx_tts_voices_gender", columnList = "gender"),
        @Index(name = "idx_tts_voices_language", columnList = "language"),
        @Index(name = "idx_tts_voices_is_active", columnList = "is_active")
}, uniqueConstraints = {
        @UniqueConstraint(name = "tts_voices_voice_id_key", columnNames = {"voice_id"})
})
public class TtsVoice implements Serializable {
    public static final String ENTITY_NAME = "Tts_Voice";
    public static final String TABLE_NAME = "tts_voices";
    public static final String COLUMN_ID_NAME = "id";
    public static final String COLUMN_VOICEID_NAME = "voice_id";
    public static final String COLUMN_VOICENAME_NAME = "voice_name";
    public static final String COLUMN_DESCRIPTION_NAME = "description";
    public static final String COLUMN_PROVIDER_NAME = "provider";
    public static final String COLUMN_GENDER_NAME = "gender";
    public static final String COLUMN_LANGUAGE_NAME = "language";
    public static final String COLUMN_ACCENT_NAME = "accent";
    public static final String COLUMN_STYLE_NAME = "style";
    public static final String COLUMN_AGEGROUP_NAME = "age_group";
    public static final String COLUMN_USECASE_NAME = "use_case";
    public static final String COLUMN_SAMPLEAUDIOURL_NAME = "sample_audio_url";
    public static final String COLUMN_SAMPLETEXT_NAME = "sample_text";
    public static final String COLUMN_SAMPLEDURATIONMS_NAME = "sample_duration_ms";
    public static final String COLUMN_DEFAULTSPEED_NAME = "default_speed";
    public static final String COLUMN_DEFAULTPITCH_NAME = "default_pitch";
    public static final String COLUMN_ISACTIVE_NAME = "is_active";
    public static final String COLUMN_ISPREMIUM_NAME = "is_premium";
    public static final String COLUMN_SORTORDER_NAME = "sort_order";
    public static final String COLUMN_CREATEDAT_NAME = "created_at";
    public static final String COLUMN_UPDATEDAT_NAME = "updated_at";
    private static final long serialVersionUID = 208532082284298852L;


    private UUID id;

    private String voiceId;

    private String voiceName;

    private String description;

    private String provider;

    private String gender;

    private String language;

    private String accent;

    private String style;

    private String ageGroup;

    private String useCase;

    private String sampleAudioUrl;

    private String sampleText;

    private Integer sampleDurationMs;

    private Double defaultSpeed;

    private Double defaultPitch;

    private Boolean isActive;

    private Boolean isPremium;

    private Integer sortOrder;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = COLUMN_ID_NAME, nullable = false)
    public UUID getId() {
        return id;
    }

    @Size(max = 100)
    @NotNull
    @Column(name = COLUMN_VOICEID_NAME, nullable = false, length = 100)
    public String getVoiceId() {
        return voiceId;
    }

    @Size(max = 100)
    @NotNull
    @Column(name = COLUMN_VOICENAME_NAME, nullable = false, length = 100)
    public String getVoiceName() {
        return voiceName;
    }

    @Column(name = COLUMN_DESCRIPTION_NAME, length = Integer.MAX_VALUE)
    public String getDescription() {
        return description;
    }

    @Size(max = 50)
    @NotNull
    @ColumnDefault("'gemini'")
    @Column(name = COLUMN_PROVIDER_NAME, nullable = false, length = 50)
    public String getProvider() {
        return provider;
    }

    @Size(max = 20)
    @Column(name = COLUMN_GENDER_NAME, length = 20)
    public String getGender() {
        return gender;
    }

    @Size(max = 10)
    @ColumnDefault("'en'")
    @Column(name = COLUMN_LANGUAGE_NAME, length = 10)
    public String getLanguage() {
        return language;
    }

    @Size(max = 50)
    @Column(name = COLUMN_ACCENT_NAME, length = 50)
    public String getAccent() {
        return accent;
    }

    @Size(max = 50)
    @Column(name = COLUMN_STYLE_NAME, length = 50)
    public String getStyle() {
        return style;
    }

    @Size(max = 20)
    @Column(name = COLUMN_AGEGROUP_NAME, length = 20)
    public String getAgeGroup() {
        return ageGroup;
    }

    @Size(max = 100)
    @Column(name = COLUMN_USECASE_NAME, length = 100)
    public String getUseCase() {
        return useCase;
    }

    @Size(max = 500)
    @Column(name = COLUMN_SAMPLEAUDIOURL_NAME, length = 500)
    public String getSampleAudioUrl() {
        return sampleAudioUrl;
    }

    @Column(name = COLUMN_SAMPLETEXT_NAME, length = Integer.MAX_VALUE)
    public String getSampleText() {
        return sampleText;
    }

    @Column(name = COLUMN_SAMPLEDURATIONMS_NAME)
    public Integer getSampleDurationMs() {
        return sampleDurationMs;
    }

    @ColumnDefault("1.0")
    @Column(name = COLUMN_DEFAULTSPEED_NAME)
    public Double getDefaultSpeed() {
        return defaultSpeed;
    }

    @ColumnDefault("0.0")
    @Column(name = COLUMN_DEFAULTPITCH_NAME)
    public Double getDefaultPitch() {
        return defaultPitch;
    }

    @ColumnDefault("true")
    @Column(name = COLUMN_ISACTIVE_NAME)
    public Boolean getIsActive() {
        return isActive;
    }

    @ColumnDefault("false")
    @Column(name = COLUMN_ISPREMIUM_NAME)
    public Boolean getIsPremium() {
        return isPremium;
    }

    @ColumnDefault("0")
    @Column(name = COLUMN_SORTORDER_NAME)
    public Integer getSortOrder() {
        return sortOrder;
    }

    @ColumnDefault("now()")
    @Column(name = COLUMN_CREATEDAT_NAME)
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    @ColumnDefault("now()")
    @Column(name = COLUMN_UPDATEDAT_NAME)
    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

}
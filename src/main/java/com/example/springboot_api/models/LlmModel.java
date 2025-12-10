package com.example.springboot_api.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity(name = LlmModel.ENTITY_NAME)
@Table(name = LlmModel.TABLE_NAME, schema = "public", indexes = {
        @Index(name = "idx_llm_models_active", columnList = "is_active")
}, uniqueConstraints = {
        @UniqueConstraint(name = "llm_models_code_key", columnNames = {"code"})
})
public class LlmModel implements Serializable {
    public static final String ENTITY_NAME = "Llm_Model";
    public static final String TABLE_NAME = "llm_models";
    public static final String COLUMN_ID_NAME = "id";
    public static final String COLUMN_CODE_NAME = "code";
    public static final String COLUMN_PROVIDER_NAME = "provider";
    public static final String COLUMN_DISPLAYNAME_NAME = "display_name";
    public static final String COLUMN_ISACTIVE_NAME = "is_active";
    public static final String COLUMN_ISDEFAULT_NAME = "is_default";
    public static final String COLUMN_METADATA_NAME = "metadata";
    public static final String COLUMN_CREATEDAT_NAME = "created_at";
    private static final long serialVersionUID = 2434333559678126647L;


    private UUID id;

    private String code;

    private String provider;

    private String displayName;

    private Boolean isActive = false;

    private Boolean isDefault = false;

    private Map<String, Object> metadata;

    private OffsetDateTime createdAt;

    private Set<NotebookAiSet> notebookAiSets = new LinkedHashSet<>();

    private Set<NotebookBotMessage> notebookBotMessages = new LinkedHashSet<>();

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = COLUMN_ID_NAME, nullable = false)
    public UUID getId() {
        return id;
    }

    @NotNull
    @Column(name = COLUMN_CODE_NAME, nullable = false, length = Integer.MAX_VALUE)
    public String getCode() {
        return code;
    }

    @NotNull
    @Column(name = COLUMN_PROVIDER_NAME, nullable = false, length = Integer.MAX_VALUE)
    public String getProvider() {
        return provider;
    }

    @NotNull
    @Column(name = COLUMN_DISPLAYNAME_NAME, nullable = false, length = Integer.MAX_VALUE)
    public String getDisplayName() {
        return displayName;
    }

    @NotNull
    @ColumnDefault("true")
    @Column(name = COLUMN_ISACTIVE_NAME, nullable = false)
    public Boolean getIsActive() {
        return isActive;
    }

    @NotNull
    @ColumnDefault("false")
    @Column(name = COLUMN_ISDEFAULT_NAME, nullable = false)
    public Boolean getIsDefault() {
        return isDefault;
    }

    @Column(name = COLUMN_METADATA_NAME)
    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @NotNull
    @ColumnDefault("now()")
    @Column(name = COLUMN_CREATEDAT_NAME, nullable = false)
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    @OneToMany(mappedBy = "llmModel")
    public Set<NotebookAiSet> getNotebookAiSets() {
        return notebookAiSets;
    }

    @OneToMany(mappedBy = "llmModel")
    public Set<NotebookBotMessage> getNotebookBotMessages() {
        return notebookBotMessages;
    }

}
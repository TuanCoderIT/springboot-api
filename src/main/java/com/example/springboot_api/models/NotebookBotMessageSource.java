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
@Entity(name = NotebookBotMessageSource.ENTITY_NAME)
@Table(name = NotebookBotMessageSource.TABLE_NAME, schema = "public")
public class NotebookBotMessageSource implements Serializable {
    public static final String ENTITY_NAME = "Notebook_Bot_Message_Source";
    public static final String TABLE_NAME = "notebook_bot_message_sources";
    public static final String COLUMN_ID_NAME = "id";
    public static final String COLUMN_SOURCETYPE_NAME = "source_type";
    public static final String COLUMN_FILEID_NAME = "file_id";
    public static final String COLUMN_CHUNKINDEX_NAME = "chunk_index";
    public static final String COLUMN_TITLE_NAME = "title";
    public static final String COLUMN_URL_NAME = "url";
    public static final String COLUMN_SNIPPET_NAME = "snippet";
    public static final String COLUMN_PROVIDER_NAME = "provider";
    public static final String COLUMN_WEBINDEX_NAME = "web_index";
    public static final String COLUMN_SCORE_NAME = "score";
    public static final String COLUMN_CREATEDAT_NAME = "created_at";
    private static final long serialVersionUID = 4405427035122460023L;


    private UUID id;

    private NotebookBotMessage message;

    private String sourceType;

    private UUID fileId;

    private Integer chunkIndex;

    private String title;

    private String url;

    private String snippet;

    private String provider;

    private Integer webIndex;

    private Double score;

    private OffsetDateTime createdAt;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = COLUMN_ID_NAME, nullable = false)
    public UUID getId() {
        return id;
    }

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "message_id", nullable = false)
    public NotebookBotMessage getMessage() {
        return message;
    }

    @NotNull
    @Column(name = COLUMN_SOURCETYPE_NAME, nullable = false, length = Integer.MAX_VALUE)
    public String getSourceType() {
        return sourceType;
    }

    @Column(name = COLUMN_FILEID_NAME)
    public UUID getFileId() {
        return fileId;
    }

    @Column(name = COLUMN_CHUNKINDEX_NAME)
    public Integer getChunkIndex() {
        return chunkIndex;
    }

    @Column(name = COLUMN_TITLE_NAME, length = Integer.MAX_VALUE)
    public String getTitle() {
        return title;
    }

    @Column(name = COLUMN_URL_NAME, length = Integer.MAX_VALUE)
    public String getUrl() {
        return url;
    }

    @Column(name = COLUMN_SNIPPET_NAME, length = Integer.MAX_VALUE)
    public String getSnippet() {
        return snippet;
    }

    @Column(name = COLUMN_PROVIDER_NAME, length = Integer.MAX_VALUE)
    public String getProvider() {
        return provider;
    }

    @Column(name = COLUMN_WEBINDEX_NAME)
    public Integer getWebIndex() {
        return webIndex;
    }

    @Column(name = COLUMN_SCORE_NAME)
    public Double getScore() {
        return score;
    }

    @NotNull
    @ColumnDefault("now()")
    @Column(name = COLUMN_CREATEDAT_NAME, nullable = false)
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

}
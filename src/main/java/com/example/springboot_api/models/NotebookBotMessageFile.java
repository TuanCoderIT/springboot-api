package com.example.springboot_api.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.Accessors;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@Accessors(chain = true)
@Entity(name = NotebookBotMessageFile.ENTITY_NAME)
@Table(name = NotebookBotMessageFile.TABLE_NAME, schema = "public")
public class NotebookBotMessageFile implements Serializable {
    public static final String ENTITY_NAME = "Notebook_Bot_Message_File";
    public static final String TABLE_NAME = "notebook_bot_message_files";
    public static final String COLUMN_ID_NAME = "id";
    public static final String COLUMN_FILETYPE_NAME = "file_type";
    public static final String COLUMN_FILEURL_NAME = "file_url";
    public static final String COLUMN_MIMETYPE_NAME = "mime_type";
    public static final String COLUMN_FILENAME_NAME = "file_name";
    public static final String COLUMN_OCRTEXT_NAME = "ocr_text";
    public static final String COLUMN_CAPTION_NAME = "caption";
    public static final String COLUMN_METADATA_NAME = "metadata";
    public static final String COLUMN_CREATEDAT_NAME = "created_at";
    private static final long serialVersionUID = 9102029085371355889L;


    private UUID id;

    private NotebookBotMessage message;

    private String fileType;

    private String fileUrl;

    private String mimeType;

    private String fileName;

    private String ocrText;

    private String caption;

    private Map<String, Object> metadata;

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

    @Column(name = COLUMN_FILETYPE_NAME, length = Integer.MAX_VALUE)
    public String getFileType() {
        return fileType;
    }

    @Column(name = COLUMN_FILEURL_NAME, length = Integer.MAX_VALUE)
    public String getFileUrl() {
        return fileUrl;
    }

    @Column(name = COLUMN_MIMETYPE_NAME, length = Integer.MAX_VALUE)
    public String getMimeType() {
        return mimeType;
    }

    @Column(name = COLUMN_FILENAME_NAME, length = Integer.MAX_VALUE)
    public String getFileName() {
        return fileName;
    }

    @Column(name = COLUMN_OCRTEXT_NAME, length = Integer.MAX_VALUE)
    public String getOcrText() {
        return ocrText;
    }

    @Column(name = COLUMN_CAPTION_NAME, length = Integer.MAX_VALUE)
    public String getCaption() {
        return caption;
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

}
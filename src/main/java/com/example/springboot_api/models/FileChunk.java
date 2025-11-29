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
@Entity(name = FileChunk.ENTITY_NAME)
@Table(name = FileChunk.TABLE_NAME, schema = "public", indexes = {
        @Index(name = "idx_file_chunks_notebook", columnList = "notebook_id"),
        @Index(name = "idx_file_chunks_file", columnList = "file_id"),
        @Index(name = "idx_file_chunks_embedding", columnList = "embedding")
})
public class FileChunk implements Serializable {
    public static final String ENTITY_NAME = "File_Chunk";
    public static final String TABLE_NAME = "file_chunks";
    public static final String COLUMN_ID_NAME = "id";
    public static final String COLUMN_PAGENUMBER_NAME = "page_number";
    public static final String COLUMN_CHUNKINDEX_NAME = "chunk_index";
    public static final String COLUMN_CONTENT_NAME = "content";
    public static final String COLUMN_EMBEDDING_NAME = "embedding";
    public static final String COLUMN_METADATA_NAME = "metadata";
    public static final String COLUMN_CREATEDAT_NAME = "created_at";
    private static final long serialVersionUID = 7429314984607649520L;


    private UUID id;

    private Notebook notebook;

    private NotebookFile file;

    private Integer pageNumber;

    private Integer chunkIndex;

    private String content;

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
    @JoinColumn(name = "notebook_id", nullable = false)
    public Notebook getNotebook() {
        return notebook;
    }

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "file_id", nullable = false)
    public NotebookFile getFile() {
        return file;
    }

    @Column(name = COLUMN_PAGENUMBER_NAME)
    public Integer getPageNumber() {
        return pageNumber;
    }

    @NotNull
    @Column(name = COLUMN_CHUNKINDEX_NAME, nullable = false)
    public Integer getChunkIndex() {
        return chunkIndex;
    }

    @NotNull
    @Column(name = COLUMN_CONTENT_NAME, nullable = false, length = Integer.MAX_VALUE)
    public String getContent() {
        return content;
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

/*
 TODO [Reverse Engineering] create field to map the 'embedding' column
 Available actions: Define target Java type | Uncomment as is | Remove column mapping
    private Object embedding;
*/
}
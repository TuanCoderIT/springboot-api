package com.example.springboot_api.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
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
@Entity(name = "File_Chunk")
@Table(name = "file_chunks", schema = "public", indexes = {
        @Index(name = "idx_file_chunks_notebook", columnList = "notebook_id"),
        @Index(name = "idx_file_chunks_file", columnList = "file_id"),
        @Index(name = "idx_file_chunks_embedding", columnList = "embedding")
})
public class FileChunk implements Serializable {
    private static final long serialVersionUID = 8528951451508750720L;
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

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "file_id", nullable = false)
    public NotebookFile getFile() {
        return file;
    }

    @Column(name = "page_number")
    public Integer getPageNumber() {
        return pageNumber;
    }

    @NotNull
    @Column(name = "chunk_index", nullable = false)
    public Integer getChunkIndex() {
        return chunkIndex;
    }

    @NotNull
    @Column(name = "content", nullable = false, length = Integer.MAX_VALUE)
    public String getContent() {
        return content;
    }

    @Column(name = "metadata")
    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

/*
 TODO [Reverse Engineering] create field to map the 'embedding' column
 Available actions: Define target Java type | Uncomment as is | Remove column mapping
    private Object embedding;
*/
}
package com.example.springboot_api.modules.chunk.entity;

import com.example.springboot_api.modules.file.entity.NotebookFile;
import com.example.springboot_api.modules.notebook.entity.Notebook;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "file_chunks")
public class FileChunk {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "notebook_id", nullable = false)
    private Notebook notebook;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "file_id", nullable = false)
    private NotebookFile file;

    @Column(name = "page_number")
    private Integer pageNumber;

    @NotNull
    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @NotNull
    @Column(name = "content", nullable = false, length = Integer.MAX_VALUE)
    private String content;

    @Column(name = "metadata")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> metadata;
    @NotNull
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

/*
 TODO [Reverse Engineering] create field to map the 'embedding' column
 Available actions: Define target Java type | Uncomment as is | Remove column mapping
    @Column(name = "embedding", columnDefinition = "vector(1536) not null")
    private Object embedding;
*/
}
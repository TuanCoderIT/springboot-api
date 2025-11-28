package com.example.springboot_api.modules.rag.entity;

import com.example.springboot_api.modules.auth.entity.User;
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
@Table(name = "rag_queries")
public class RagQuery {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "notebook_id", nullable = false)
    private Notebook notebook;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "user_id")
    private User user;

    @NotNull
    @Column(name = "question", nullable = false, length = Integer.MAX_VALUE)
    private String question;

    @Column(name = "answer", length = Integer.MAX_VALUE)
    private String answer;

    @Column(name = "source_chunks")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> sourceChunks;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

}
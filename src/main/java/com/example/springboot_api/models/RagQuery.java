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
@Entity(name = "Rag_Query")
@Table(name = "rag_queries", schema = "public", indexes = {
        @Index(name = "idx_rag_queries_notebook", columnList = "notebook_id, created_at"),
        @Index(name = "idx_rag_queries_user", columnList = "user_id, created_at")
})
public class RagQuery implements Serializable {
    private static final long serialVersionUID = 5664365710151332112L;
    private UUID id;

    private Notebook notebook;

    private User user;

    private String question;

    private String answer;

    private Map<String, Object> sourceChunks;

    private Integer latencyMs;

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
    @JoinColumn(name = "user_id")
    public User getUser() {
        return user;
    }

    @NotNull
    @Column(name = "question", nullable = false, length = Integer.MAX_VALUE)
    public String getQuestion() {
        return question;
    }

    @Column(name = "answer", length = Integer.MAX_VALUE)
    public String getAnswer() {
        return answer;
    }

    @Column(name = "source_chunks")
    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> getSourceChunks() {
        return sourceChunks;
    }

    @Column(name = "latency_ms")
    public Integer getLatencyMs() {
        return latencyMs;
    }

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

}
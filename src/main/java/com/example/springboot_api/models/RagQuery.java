package com.example.springboot_api.models;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity(name = RagQuery.ENTITY_NAME)
@Table(name = RagQuery.TABLE_NAME)
public class RagQuery implements Serializable {
    public static final String ENTITY_NAME = "Rag_Query";
    public static final String TABLE_NAME = "rag_queries";
    public static final String COLUMN_ID_NAME = "id";
    public static final String COLUMN_QUESTION_NAME = "question";
    public static final String COLUMN_ANSWER_NAME = "answer";
    public static final String COLUMN_SOURCECHUNKS_NAME = "source_chunks";
    public static final String COLUMN_LATENCYMS_NAME = "latency_ms";
    public static final String COLUMN_CREATEDAT_NAME = "created_at";
    private static final long serialVersionUID = 4545290807690008791L;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "user_id")
    public User getUser() {
        return user;
    }

    @NotNull
    @Column(name = COLUMN_QUESTION_NAME, nullable = false, length = Integer.MAX_VALUE)
    public String getQuestion() {
        return question;
    }

    @Column(name = COLUMN_ANSWER_NAME, length = Integer.MAX_VALUE)
    public String getAnswer() {
        return answer;
    }

    @Column(name = COLUMN_SOURCECHUNKS_NAME)
    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> getSourceChunks() {
        return sourceChunks;
    }

    @Column(name = COLUMN_LATENCYMS_NAME)
    public Integer getLatencyMs() {
        return latencyMs;
    }

    @NotNull
    @ColumnDefault("now()")
    @Column(name = COLUMN_CREATEDAT_NAME, nullable = false)
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

}
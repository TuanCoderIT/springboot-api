package com.example.springboot_api.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.Accessors;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@Accessors(chain = true)
@Entity(name = RegulationChatAnalytic.ENTITY_NAME)
@Table(name = RegulationChatAnalytic.TABLE_NAME, schema = "public", indexes = {
        @Index(name = "idx_reg_chat_anl_notebook_time", columnList = "notebook_id, created_at"),
        @Index(name = "idx_reg_chat_anl_conversation_time", columnList = "conversation_id, created_at"),
        @Index(name = "idx_reg_chat_anl_user_time", columnList = "user_id, created_at"),
        @Index(name = "idx_reg_chat_anl_status", columnList = "status"),
        @Index(name = "idx_reg_chat_anl_rating", columnList = "rating"),
        @Index(name = "idx_reg_chat_anl_feedback_type", columnList = "feedback_type"),
        @Index(name = "idx_reg_chat_anl_hash_time", columnList = "query_hash, created_at"),
        @Index(name = "idx_reg_chat_anl_language_time", columnList = "query_language, created_at")
})
public class RegulationChatAnalytic implements Serializable {
    public static final String ENTITY_NAME = "Regulation_Chat_Analytic";
    public static final String TABLE_NAME = "regulation_chat_analytics";
    public static final String COLUMN_ID_NAME = "id";
    public static final String COLUMN_MODE_NAME = "mode";
    public static final String COLUMN_STATUS_NAME = "status";
    public static final String COLUMN_RATING_NAME = "rating";
    public static final String COLUMN_FEEDBACKTYPE_NAME = "feedback_type";
    public static final String COLUMN_FEEDBACKTEXT_NAME = "feedback_text";
    public static final String COLUMN_LATENCYMS_NAME = "latency_ms";
    public static final String COLUMN_CREATEDAT_NAME = "created_at";
    public static final String COLUMN_QUERYTEXT_NAME = "query_text";
    public static final String COLUMN_QUERYHASH_NAME = "query_hash";
    public static final String COLUMN_QUERYLANGUAGE_NAME = "query_language";
    public static final String COLUMN_AVGSOURCESCORE_NAME = "avg_source_score";
    private static final long serialVersionUID = -6647743946343123775L;


    private UUID id;

    private Notebook notebook;

    private NotebookBotConversation conversation;

    private NotebookBotMessage message;

    private User user;

    private String mode;

    private String status;

    private Short rating;

    private String feedbackType;

    private String feedbackText;

    private Integer latencyMs;

    private OffsetDateTime createdAt;

    private String queryText;

    private String queryHash;

    private String queryLanguage;

    private BigDecimal avgSourceScore;

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
    @JoinColumn(name = "conversation_id", nullable = false)
    public NotebookBotConversation getConversation() {
        return conversation;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "message_id")
    public NotebookBotMessage getMessage() {
        return message;
    }

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "user_id", nullable = false)
    public User getUser() {
        return user;
    }

    @Size(max = 20)
    @NotNull
    @ColumnDefault("'RAG'")
    @Column(name = COLUMN_MODE_NAME, nullable = false, length = 20)
    public String getMode() {
        return mode;
    }

    @Size(max = 30)
    @NotNull
    @ColumnDefault("'OK'")
    @Column(name = COLUMN_STATUS_NAME, nullable = false, length = 30)
    public String getStatus() {
        return status;
    }

    @Column(name = COLUMN_RATING_NAME)
    public Short getRating() {
        return rating;
    }

    @Size(max = 20)
    @Column(name = COLUMN_FEEDBACKTYPE_NAME, length = 20)
    public String getFeedbackType() {
        return feedbackType;
    }

    @Column(name = COLUMN_FEEDBACKTEXT_NAME, length = Integer.MAX_VALUE)
    public String getFeedbackText() {
        return feedbackText;
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

    @Column(name = COLUMN_QUERYTEXT_NAME, length = Integer.MAX_VALUE)
    public String getQueryText() {
        return queryText;
    }

    @Size(max = 64)
    @Column(name = COLUMN_QUERYHASH_NAME, length = 64)
    public String getQueryHash() {
        return queryHash;
    }

    @Size(max = 5)
    @Column(name = COLUMN_QUERYLANGUAGE_NAME, length = 5)
    public String getQueryLanguage() {
        return queryLanguage;
    }

    @Column(name = COLUMN_AVGSOURCESCORE_NAME, precision = 6, scale = 5)
    public BigDecimal getAvgSourceScore() {
        return avgSourceScore;
    }

}
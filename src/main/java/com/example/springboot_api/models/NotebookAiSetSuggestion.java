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
@ToString
@Entity(name = NotebookAiSetSuggestion.ENTITY_NAME)
@Table(name = NotebookAiSetSuggestion.TABLE_NAME, schema = "public", indexes = {
        @Index(name = "idx_nass_ai_set", columnList = "notebook_ai_set_id, created_at")
})
public class NotebookAiSetSuggestion implements Serializable {
    public static final String ENTITY_NAME = "Notebook_Ai_Set_Suggestion";
    public static final String TABLE_NAME = "notebook_ai_set_suggestions";
    public static final String COLUMN_ID_NAME = "id";
    public static final String COLUMN_SUGGESTIONS_NAME = "suggestions";
    public static final String COLUMN_CREATEDAT_NAME = "created_at";
    private static final long serialVersionUID = -6173592578502058642L;


    private UUID id;

    private NotebookAiSet notebookAiSet;

    private Map<String, Object> suggestions;

    private User createdBy;

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
    @JoinColumn(name = "notebook_ai_set_id", nullable = false)
    public NotebookAiSet getNotebookAiSet() {
        return notebookAiSet;
    }

    @NotNull
    @Column(name = COLUMN_SUGGESTIONS_NAME, nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> getSuggestions() {
        return suggestions;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "created_by")
    public User getCreatedBy() {
        return createdBy;
    }

    @NotNull
    @ColumnDefault("now()")
    @Column(name = COLUMN_CREATEDAT_NAME, nullable = false)
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

}
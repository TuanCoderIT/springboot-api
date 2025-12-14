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
@Entity(name = NotebookMindmap.ENTITY_NAME)
@Table(name = NotebookMindmap.TABLE_NAME, schema = "public", indexes = {
        @Index(name = "idx_mindmaps_notebook", columnList = "notebook_id"),
        @Index(name = "idx_mindmaps_ai_set", columnList = "source_ai_set_id")
})
public class NotebookMindmap implements Serializable {
    public static final String ENTITY_NAME = "Notebook_Mindmap";
    public static final String TABLE_NAME = "notebook_mindmaps";
    public static final String COLUMN_ID_NAME = "id";
    public static final String COLUMN_TITLE_NAME = "title";
    public static final String COLUMN_MINDMAP_NAME = "mindmap";
    public static final String COLUMN_LAYOUT_NAME = "layout";
    public static final String COLUMN_CREATEDAT_NAME = "created_at";
    public static final String COLUMN_UPDATEDAT_NAME = "updated_at";
    private static final long serialVersionUID = 3308836427075503607L;


    private UUID id;

    private Notebook notebook;

    private String title;

    private Map<String, Object> mindmap;

    private Map<String, Object> layout;

    private NotebookAiSet sourceAiSet;

    private User createdBy;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

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
    @Column(name = COLUMN_TITLE_NAME, nullable = false, length = Integer.MAX_VALUE)
    public String getTitle() {
        return title;
    }

    @NotNull
    @Column(name = COLUMN_MINDMAP_NAME, nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> getMindmap() {
        return mindmap;
    }

    @Column(name = COLUMN_LAYOUT_NAME)
    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> getLayout() {
        return layout;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "source_ai_set_id")
    public NotebookAiSet getSourceAiSet() {
        return sourceAiSet;
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

    @NotNull
    @ColumnDefault("now()")
    @Column(name = COLUMN_UPDATEDAT_NAME, nullable = false)
    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

}
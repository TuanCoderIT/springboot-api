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
import jakarta.persistence.Index;
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
import lombok.experimental.Accessors;

/**
 * Entity lưu các sự kiện trong Timeline AI-generated.
 * Mỗi event thuộc về một NotebookAiSet (timeline task).
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@Accessors(chain = true)
@Entity(name = TimelineEvent.ENTITY_NAME)
@Table(name = TimelineEvent.TABLE_NAME, schema = "public", indexes = {
        @Index(name = "idx_timeline_events_notebook", columnList = "notebook_id"),
        @Index(name = "idx_timeline_events_ai_set", columnList = "notebook_ai_sets_id"),
        @Index(name = "idx_timeline_events_order", columnList = "notebook_ai_sets_id, event_order")
})
public class TimelineEvent implements Serializable {

    public static final String ENTITY_NAME = "Timeline_Event";
    public static final String TABLE_NAME = "timeline_events";
    public static final String COLUMN_ID_NAME = "id";
    public static final String COLUMN_EVENTORDER_NAME = "event_order";
    public static final String COLUMN_DATE_NAME = "date";
    public static final String COLUMN_DATEPRECISION_NAME = "date_precision";
    public static final String COLUMN_TITLE_NAME = "title";
    public static final String COLUMN_DESCRIPTION_NAME = "description";
    public static final String COLUMN_IMPORTANCE_NAME = "importance";
    public static final String COLUMN_ICON_NAME = "icon";
    public static final String COLUMN_EXTRAMETADATA_NAME = "extra_metadata";
    public static final String COLUMN_CREATEDAT_NAME = "created_at";

    private static final long serialVersionUID = 1L;

    private UUID id;
    private Notebook notebook;
    private NotebookAiSet notebookAiSets;
    private User createdBy;

    private Integer eventOrder;
    private String date;
    private String datePrecision;
    private String title;
    private String description;
    private String importance;
    private String icon;
    private Map<String, Object> extraMetadata;
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
    @JoinColumn(name = "notebook_ai_sets_id")
    public NotebookAiSet getNotebookAiSets() {
        return notebookAiSets;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "created_by")
    public User getCreatedBy() {
        return createdBy;
    }

    @NotNull
    @Column(name = COLUMN_EVENTORDER_NAME, nullable = false)
    public Integer getEventOrder() {
        return eventOrder;
    }

    @Column(name = COLUMN_DATE_NAME, length = 50)
    public String getDate() {
        return date;
    }

    @ColumnDefault("'unknown'")
    @Column(name = COLUMN_DATEPRECISION_NAME, length = 20)
    public String getDatePrecision() {
        return datePrecision;
    }

    @NotNull
    @Column(name = COLUMN_TITLE_NAME, nullable = false, length = 500)
    public String getTitle() {
        return title;
    }

    @Column(name = COLUMN_DESCRIPTION_NAME, length = Integer.MAX_VALUE)
    public String getDescription() {
        return description;
    }

    @ColumnDefault("'normal'")
    @Column(name = COLUMN_IMPORTANCE_NAME, length = 20)
    public String getImportance() {
        return importance;
    }

    @Column(name = COLUMN_ICON_NAME, length = 50)
    public String getIcon() {
        return icon;
    }

    @Column(name = COLUMN_EXTRAMETADATA_NAME)
    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> getExtraMetadata() {
        return extraMetadata;
    }

    @NotNull
    @ColumnDefault("now()")
    @Column(name = COLUMN_CREATEDAT_NAME, nullable = false)
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}

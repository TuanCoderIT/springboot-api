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
import lombok.experimental.Accessors;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@Accessors(chain = true)
@Entity(name = ChapterItem.ENTITY_NAME)
@Table(name = ChapterItem.TABLE_NAME, schema = "public")
public class ChapterItem implements Serializable {
    public static final String ENTITY_NAME = "Chapter_Item";
    public static final String TABLE_NAME = "chapter_items";
    public static final String COLUMN_ID_NAME = "id";
    public static final String COLUMN_ITEMTYPE_NAME = "item_type";
    public static final String COLUMN_REFID_NAME = "ref_id";
    public static final String COLUMN_TITLE_NAME = "title";
    public static final String COLUMN_SORTORDER_NAME = "sort_order";
    public static final String COLUMN_METADATA_NAME = "metadata";
    public static final String COLUMN_CREATEDAT_NAME = "created_at";
    public static final String COLUMN_VISIBLE_IN_LESSON_NAME = "visible_in_lesson";
    public static final String COLUMN_VISIBLE_IN_NOTEBOOK_NAME = "visible_in_notebook";
    private static final long serialVersionUID = 1055570440939484806L;

    private UUID id;

    private NotebookChapter chapter;

    private String itemType;

    private UUID refId;

    private String title;

    private Integer sortOrder;

    private Map<String, Object> metadata;

    private OffsetDateTime createdAt;

    private Boolean visibleInLesson = true;

    private Boolean visibleInNotebook = true;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = COLUMN_ID_NAME, nullable = false)
    public UUID getId() {
        return id;
    }

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "chapter_id", nullable = false)
    public NotebookChapter getChapter() {
        return chapter;
    }

    @NotNull
    @Column(name = COLUMN_ITEMTYPE_NAME, nullable = false, length = Integer.MAX_VALUE)
    public String getItemType() {
        return itemType;
    }

    @Column(name = COLUMN_REFID_NAME)
    public UUID getRefId() {
        return refId;
    }

    @Column(name = COLUMN_TITLE_NAME, length = Integer.MAX_VALUE)
    public String getTitle() {
        return title;
    }

    @NotNull
    @ColumnDefault("0")
    @Column(name = COLUMN_SORTORDER_NAME, nullable = false)
    public Integer getSortOrder() {
        return sortOrder;
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

    @NotNull
    @ColumnDefault("true")
    @Column(name = COLUMN_VISIBLE_IN_LESSON_NAME, nullable = false)
    public Boolean getVisibleInLesson() {
        return visibleInLesson;
    }

    @NotNull
    @ColumnDefault("true")
    @Column(name = COLUMN_VISIBLE_IN_NOTEBOOK_NAME, nullable = false)
    public Boolean getVisibleInNotebook() {
        return visibleInNotebook;
    }

}
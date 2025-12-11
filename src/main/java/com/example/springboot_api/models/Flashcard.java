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
@Entity(name = Flashcard.ENTITY_NAME)
@Table(name = Flashcard.TABLE_NAME, schema = "public", indexes = {
        @Index(name = "idx_flashcards_notebook", columnList = "notebook_id")
})
public class Flashcard implements Serializable {
    public static final String ENTITY_NAME = "Flashcard";
    public static final String TABLE_NAME = "flashcards";
    public static final String COLUMN_ID_NAME = "id";
    public static final String COLUMN_FRONTTEXT_NAME = "front_text";
    public static final String COLUMN_BACKTEXT_NAME = "back_text";
    public static final String COLUMN_EXTRAMETADATA_NAME = "extra_metadata";
    public static final String COLUMN_CREATEDAT_NAME = "created_at";
    public static final String COLUMN_HINT_NAME = "hint";
    public static final String COLUMN_EXAMPLE_NAME = "example";
    public static final String COLUMN_IMAGEURL_NAME = "image_url";
    public static final String COLUMN_AUDIOURL_NAME = "audio_url";
    private static final long serialVersionUID = -1596234167648093732L;


    private UUID id;

    private Notebook notebook;

    private User createdBy;

    private String frontText;

    private String backText;

    private Map<String, Object> extraMetadata;

    private OffsetDateTime createdAt;

    private NotebookAiSet notebookAiSets;

    private String hint;

    private String example;

    private String imageUrl;

    private String audioUrl;

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
    @JoinColumn(name = "created_by")
    public User getCreatedBy() {
        return createdBy;
    }

    @NotNull
    @Column(name = COLUMN_FRONTTEXT_NAME, nullable = false, length = Integer.MAX_VALUE)
    public String getFrontText() {
        return frontText;
    }

    @NotNull
    @Column(name = COLUMN_BACKTEXT_NAME, nullable = false, length = Integer.MAX_VALUE)
    public String getBackText() {
        return backText;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notebook_ai_sets_id")
    public NotebookAiSet getNotebookAiSets() {
        return notebookAiSets;
    }

    @Column(name = COLUMN_HINT_NAME, length = Integer.MAX_VALUE)
    public String getHint() {
        return hint;
    }

    @Column(name = COLUMN_EXAMPLE_NAME, length = Integer.MAX_VALUE)
    public String getExample() {
        return example;
    }

    @Column(name = COLUMN_IMAGEURL_NAME, length = Integer.MAX_VALUE)
    public String getImageUrl() {
        return imageUrl;
    }

    @Column(name = COLUMN_AUDIOURL_NAME, length = Integer.MAX_VALUE)
    public String getAudioUrl() {
        return audioUrl;
    }

}
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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
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
    private static final long serialVersionUID = 6145289652347593416L;


    private UUID id;

    private Notebook notebook;

    private User createdBy;

    private String frontText;

    private String backText;

    private Map<String, Object> extraMetadata;

    private OffsetDateTime createdAt;

    private Set<FlashcardFile> flashcardFiles = new LinkedHashSet<>();

    private Set<FlashcardReview> flashcardReviews = new LinkedHashSet<>();

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

    @OneToMany(mappedBy = "flashcard")
    public Set<FlashcardFile> getFlashcardFiles() {
        return flashcardFiles;
    }

    @OneToMany(mappedBy = "flashcard")
    public Set<FlashcardReview> getFlashcardReviews() {
        return flashcardReviews;
    }

}